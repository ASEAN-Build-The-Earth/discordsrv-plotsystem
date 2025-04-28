package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.AccountType;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.AccountTypeException;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.*;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.Debug;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.providers.WebhookProvider;
import github.tintinkung.discordps.core.providers.WebhookProviderImpl;
import github.tintinkung.discordps.core.system.layout.Layout;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ForumWebhook extends WebhookProvider implements ForumWebhookImpl {
    private final JDAImpl jdaImpl;
    private final Webhook webhook;
    private final Webhook.WebhookReference webhookReference;
    private final YamlConfiguration forumConfig;


    /**
     * Initialize Webhook provider with a given configuration
     *
     * @param config Yaml Configuration file of this webhook
     * @throws IllegalArgumentException If the given configuration is invalid for webhook ID and its channel ID
     */
    public ForumWebhook(JDA jda, @NotNull YamlConfiguration config, @NotNull YamlConfiguration forumConfig) throws IllegalArgumentException {
        super(config);
        this.forumConfig = forumConfig;
        this.jdaImpl = this.makeJdaImpl(jda);
        this.webhookReference = new Webhook.WebhookReference(this.getJDA(), this.webhookID, this.channelID);
        this.webhook = this.retrieveWebhook();
    }

    private JDAImpl makeJdaImpl(@NotNull JDA jda) {
        // Validate for account type (This should never happen)
        try { AccountTypeException.check(jda.getAccountType(), AccountType.BOT); }
        catch (AccountTypeException ex) { throw new IllegalArgumentException(ex.getMessage()); }

        return (JDAImpl) jda;
    }

    @Override
    public Webhook getWebhook() {
        return this.webhook;
    }

    @Override
    public WebhookProviderImpl getProvider() { return this; }

    @Override
    public @NotNull Webhook.WebhookReference getWebhookReference(){
        return this.webhookReference;
    }

    @Override
    public JDAImpl getJDA() {
        return this.jdaImpl;
    }

    @Override
    public void validateWebhook() {
        // Fetch database to update webhooks every session
        try {
            // get rid of all previous webhooks created by itself if it is no longer configured anywhere
            for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {

                Member selfMember = guild.getSelfMember();

                if (!selfMember.hasPermission(Permission.MANAGE_WEBHOOKS)) {
                    DiscordPS.error(
                        Debug.Error.WEBHOOK_CAN_NOT_MANAGE,
                        "Unable to manage webhooks guild-wide in " + guild
                        + ", please allow the permission MANAGE_WEBHOOKS"
                    );
                    continue;
                }

                this.validateWebhooksInGuild(guild, selfMember);
            }
        } catch (RuntimeException ex) {
            DiscordPS.error(
                Debug.Error.WEBHOOK_VALIDATION_UNKNOWN_EXCEPTION,
                "Failed to validate guild webhooks of unknown exception: " + ex.getMessage(),
                ex
            );
        }

        if(DiscordPS.getDebugger().hasGroup(Debug.ErrorGroup.WEBHOOK_REFS_VALIDATION)) {
            DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED);
            return;
        }

        // Order matters here
        // 1st Resolve the tag enum to is corresponding config value
        resolveStatusTags();
        // 2nd validate its existence by fetching available tags from this webhook channel
        validateStatusTags();
    }

    private void resolveStatusTags() {

        // Resolve the config path into tag enum
        try {
            AvailableTags.resolveAllTag(this.forumConfig);
        }
        catch (NoSuchElementException ex) {
            DiscordPS.error(Debug.Error.WEBHOOK_TAG_CONFIGURATION_FAILED, ex);
        }
    }

    private void validateStatusTags() {
        try {
            DataArray availableTags = getAvailableTags(true)
                    .complete()
                    .orElseThrow(() -> new RuntimeException("Failed to fetch webhook channel data to validate available tags."));
            AvailableTags.initCache(availableTags);
            AvailableTags.applyAllTag();
        }
        catch (RuntimeException ex) {
            DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED, ex.getMessage(), ex);
        }
    }

    public RestAction<Optional<DataArray>> getAvailableTags(boolean allowSecondAttempt) {
        Route.CompiledRoute route = Route
            .get(Route.Channels.MODIFY_CHANNEL.getRoute())
            .compile(Long.toUnsignedString(this.channelID));

        return new RestActionImpl<>(this.jdaImpl, route, (response, request) -> {
            try {
                int status = response.code;
                if (status == 404) {
                    // 404 = Invalid Webhook (most likely to have been deleted)
                    DiscordPS.error("Channel GET returned 404" + (allowSecondAttempt? " ... retrying in 5 seconds" : ""));
                    if (allowSecondAttempt)
                        return getAvailableTags(false).completeAfter(5, TimeUnit.SECONDS);
                    request.cancel();

                    return Optional.empty();
                }
                Optional<DataObject> body = response.optObject();

                if (body.isPresent()) {
                    if (body.get().hasKey("code")) {
                        if (body.get().getInt("code") == 10015) {
                            DiscordSRV.debug(github.scarsz.discordsrv.Debug.MINECRAFT_TO_DISCORD, "Webhook delivery returned 10015 (Unknown Webhook)");
                            request.cancel();
                            return Optional.empty();
                        }
                    }

                    // Also fail if the channel isn't forum channel
                    if(body.get().hasKey("type")) {
                        int type = body.get().getInt("type");
                        if(type != 15) {
                            DiscordPS.error(Debug.Error.WEBHOOK_CHANNEL_NOT_FORUM);
                            return Optional.empty();
                        }
                    }

                    // Push all available tags in the forum channel
                    if(body.get().hasKey("available_tags"))
                        return Optional.of(body.get().getArray("available_tags"));


                    return Optional.empty();
                }
            }
            catch (Throwable ex) {
                DiscordPS.warning("Failed to receive API response: " + ex.toString());
                request.cancel();
            }
            return Optional.empty();
        });
    }

    private void validateWebhooksInGuild(@NotNull Guild guild, Member bot) {

        Route.CompiledRoute route = Route.Guilds.GET_WEBHOOKS.compile(guild.getId());

        // Retrieve guild webhooks as raw data since this outdated api produce null value for newer webhook.
        new RestActionImpl<List<DataObject>>(this.getJDA(), route, (response, request) -> {
            DataArray array = response.getArray();
            List<DataObject> webhooks = new ArrayList<>(array.length());

            for(int i = 0; i < array.length(); ++i) {
                try {
                    DataObject webhookObj = array.getObject(i);

                    webhooks.add(webhookObj);
                } catch (Exception e) {
                    DiscordPS.error("[Internal] Error creating webhook from json", e);
                }
            }

            return Collections.unmodifiableList(webhooks);
        }).queue(webhooks -> {
            boolean webhookExist = false;
            List<String> notRegistered = new ArrayList<>();

            for (DataObject webhook : webhooks) {
                try {
                    // Filter for bot created webhook only
                    DataObject user = webhook.getObject("user");

                    long userID = Long.parseUnsignedLong(user.getString("id"));

                    if(userID != bot.getIdLong()) continue;

                    // Check for configured webhooks
                    String name = webhook.getString("name");
                    String username = user.getString("username");

                    long id = Long.parseUnsignedLong(webhook.getString("id"));
                    long channel = Long.parseUnsignedLong(webhook.getString("channel_id"));

                    if(name.startsWith(Constants.DISCORD_SRV_WEBHOOK_PREFIX_LEGACY)
                    || name.startsWith(Constants.DISCORD_SRV_WEBHOOK_PREFIX))
                        continue;

                    if(id == this.webhookID && channel == this.channelID) {
                        DiscordPS.debug("Found configured webhook: " + name + ": " + username);
                        webhookExist = true;
                    }
                    else {
                        // name:username(snowflake)
                        notRegistered.add(name + ":" + username + "(" + Long.toUnsignedString(id) + ")");
                        DiscordPS.warning("Found un-registered bot created webhook: " + name + ": " + username);
                    }
                } catch (ParsingException ex) {
                    DiscordPS.error("Fetching webhook: "
                        + "Exception occur trying to parse webhook data, "
                        + "maybe discord API updated?",
                        ex);
                }
            }

            if(!webhookExist)
                DiscordPS.error(Debug.Error.WEBHOOK_NOT_FOUND_IN_GUILD);

            if(!notRegistered.isEmpty()) DiscordPS.warning(
                Debug.Warning.WEBHOOK_NOT_REGISTERED_DETECTED,
                "Detected un-registered webhook created by this bot " + notRegistered
            );
        });
    }

    @Override
    public @NotNull RestAction<Optional<DataObject>> editThreadChannelTags(
            @NotNull String channelID,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt) {
        return this.modifyThreadChannel(channelID, null, appliedTags, null, null, null, allowSecondAttempt);
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt) {
        return this.editWebhookMessage(threadID, threadID, webhookData, false, allowSecondAttempt);
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> editThreadMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt) {
        return this.editWebhookMessage(threadID, messageID, webhookData, false, allowSecondAttempt);
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt) {
        return this.editWebhookMessage(threadID, threadID, webhookData, withComponents, allowSecondAttempt);
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt) {
        return this.newThreadWithMessage(webhookData, appliedTags, false, allowSecondAttempt);
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean withComponents,
            boolean allowSecondAttempt) {
        return this.newThreadWithMessage(webhookData, appliedTags, false, allowSecondAttempt);
    }


    public @NotNull RestAction<Optional<DataObject>> modifyThreadChannel(
            @NotNull String channelID,
            @Nullable String name,
            @Nullable Collection<Long> appliedTags,
            @Nullable Integer autoArchiveDuration,
            @Nullable Boolean locked,
            @Nullable Boolean archived,
            boolean allowSecondAttempt
    ) {
        Checks.isSnowflake(channelID, "Thread Channel ID");
        Route.CompiledRoute route = Route.Channels.MODIFY_CHANNEL.compile(channelID);

        DataObject data = DataObject.empty();

        if(name != null) data.put("name", name);

        if(locked != null) data.put("locked", locked);

        if(archived != null) data.put("archived", archived);

        if(autoArchiveDuration != null) data.put("auto_archive_duration", autoArchiveDuration);

        if(appliedTags != null) {
            DataArray tagArray = DataArray.empty();
            for (Long tagID : appliedTags) {
                if (tagID != null) {
                    tagArray.add(tagID);
                }
            }
            data.put("applied_tags", tagArray);
        }

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), data.toString());

        return new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, requestBody, (response, request) -> {
            try {
                if(!response.isOk()) {
                    if(allowSecondAttempt)
                        return retryWebhookExecution(() ->
                                modifyThreadChannel(channelID, name, appliedTags, autoArchiveDuration, locked, archived, false));
                    return Optional.empty();
                }

                if (response.optObject().isEmpty()) return Optional.empty();

                DataObject body = response.optObject().get();
                return Optional.of(body);
            }
            catch (Throwable ex) {
                DiscordPS.debug("Failed to receive API response modifying forum thread: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    public @NotNull RestAction<Optional<MessageReference>> editWebhookMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt
    ) {
        Checks.isSnowflake(threadID, "Webhook thread ID");
        Checks.isSnowflake(messageID, "Webhook message ID");

        Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK_EDIT
                .compile(webhook.getId(), webhook.getToken(), messageID)
                .withQueryParams("thread_id", threadID)
                .withQueryParams("with_components", String.valueOf(withComponents));

        MultipartBody requestBody = webhookData.prepareRequestBody();

        return new RestActionImpl<>(this.jdaImpl, route, requestBody, (response, request) -> {
            try {
                if(!response.isOk()) {
                    if(allowSecondAttempt)
                        return retryWebhookExecution(() ->
                                editWebhookMessage(threadID, messageID, webhookData, withComponents, false));
                    return Optional.empty();
                }

                if (response.optObject().isEmpty()) return Optional.empty();

                DataObject body = response.optObject().get();
                return Optional.ofNullable(packageMessageResponse(body));
            }
            catch (Throwable ex) {
                DiscordPS.debug("Failed to receive API response editing webhook message: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    public @NotNull RestAction<Optional<ReceivedMessage>> getWebhookMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            boolean allowSecondAttempt) {

        Checks.isSnowflake(threadID, "Webhook thread ID");
        Checks.isSnowflake(messageID, "Webhook message ID");

        Route.CompiledRoute route = Route.get(Route.Webhooks.EXECUTE_WEBHOOK_EDIT.getRoute())
                .compile(webhook.getId(), webhook.getToken(), messageID)
                .withQueryParams("thread_id", threadID);

        return new RestActionImpl<>(this.jdaImpl, route, (response, request) -> {
            try {
                if(!response.isOk()) {
                    if(allowSecondAttempt)
                        return retryWebhookExecution(() ->
                                getWebhookMessage(threadID, messageID, false));
                    return Optional.empty();
                }

                if (response.optObject().isEmpty()) return Optional.empty();

                // Mock a text channel because forum channel doesn't exist
                TextChannelImpl channel = new TextChannelImpl(this.channelID, new GuildImpl(this.jdaImpl, this.guildID));

                DataObject body = response.optObject().get();
                return Optional.of(this.jdaImpl.getEntityBuilder().createMessage(body, channel, false));
            }
            catch (Throwable ex) {
                DiscordPS.debug("Failed to receive API response creating new thread with message: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    public @NotNull RestAction<Optional<Layout>> getInitialLayout(
            @NotNull String threadID,
            boolean allowSecondAttempt) {

        Checks.isSnowflake(threadID, "Webhook thread ID");

        Route.CompiledRoute route = Route.get(Route.Webhooks.EXECUTE_WEBHOOK_EDIT.getRoute())
                .compile(webhook.getId(), webhook.getToken(), threadID)
                .withQueryParams("thread_id", threadID);

        return new RestActionImpl<>(this.jdaImpl, route, (response, request) -> {
            try {
                if(!response.isOk()) {
                    if(allowSecondAttempt)
                        return retryWebhookExecution(() -> getInitialLayout(threadID, false));
                    return Optional.empty();
                }

                if (response.optObject().isEmpty()) return Optional.empty();

                DataObject body = response.optObject().get();
                Optional<DataArray> components = body.optArray("components");

                return components.flatMap((data) -> Optional.of(Layout.fromRawData(data)));
                // return Optional.of(this.jdaImpl.getEntityBuilder().createMessage(body, channel, false));
            }
            catch (Throwable ex) {
                DiscordPS.debug("Failed to receive API response creating new thread with message: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }


    public @NotNull RestAction<Optional<MessageReference>> newThreadWithMessage(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean withComponents,
            boolean allowSecondAttempt) {

            Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK
                    .compile(webhook.getId(), webhook.getToken())
                    .withQueryParams("wait", "true")
                    .withQueryParams("with_components", String.valueOf(withComponents));

            DiscordPS.debug("Querying URL: " + route.getCompiledRoute());

            if(StringUtils.isBlank(webhookData.getString("thread_name", null))) {
                throw new IllegalArgumentException("Thread Name is required to create a new thread.");
            }

            if(appliedTags != null) {
                DataArray tagArray = DataArray.empty();
                for (Long tagID : appliedTags) {
                    if (tagID != null) {
                        tagArray.add(tagID);
                    }
                }

                if(!tagArray.isEmpty())
                    webhookData.put("applied_tags", tagArray);
            }

            MultipartBody requestBody = webhookData.prepareRequestBody();

            return new RestActionImpl<>(this.jdaImpl, route, requestBody, (response, request) -> {
                try {
                    if(!response.isOk()) {
                        if(allowSecondAttempt)
                            return retryWebhookExecution(() ->
                                    newThreadWithMessage(webhookData, appliedTags, withComponents, false));
                        return Optional.empty();
                    }

                    if (response.optObject().isEmpty()) return Optional.empty();

                    DataObject body = response.optObject().get();
                    return Optional.ofNullable(packageMessageResponse(body));
                }
                catch (Throwable ex) {
                    DiscordPS.debug("Failed to receive API response creating new thread with message: " + ex.getMessage());
                    return Optional.empty();
                }
            });
    }

    public @NotNull RestAction<Optional<MessageReference>> sendMessageInThread(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt) {

        Checks.isSnowflake(threadID, "Webhook thread ID");

        Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK
                .compile(webhook.getId(), webhook.getToken())
                .withQueryParams("wait", "true")
                .withQueryParams("thread_id", threadID)
                .withQueryParams("with_components", String.valueOf(withComponents));

        DiscordPS.debug("Querying URL: " + route.getCompiledRoute());

        MultipartBody requestBody = webhookData.prepareRequestBody();

        return new RestActionImpl<>(this.jdaImpl, route, requestBody, (response, request) -> {
            try {
                if(!response.isOk()) {
                    if(allowSecondAttempt)
                        return retryWebhookExecution(() ->
                                sendMessageInThread(threadID, webhookData, withComponents, false));
                    return Optional.empty();
                }

                if (response.optObject().isEmpty()) return Optional.empty();

                DataObject body = response.optObject().get();
                return Optional.ofNullable(packageMessageResponse(body));
            }
            catch (Throwable ex) {
                DiscordPS.debug("Failed to receive API response creating new thread with message: " + ex.getMessage());
                return Optional.empty();
            }
        });
    }

    private <T> Optional<T> retryWebhookExecution(@NotNull WebhookExecution<T> execution) {
        DiscordPS.debug("Webhook delivery returned 404, trying again in 5 seconds");
        return execution.resolve().completeAfter(5, TimeUnit.SECONDS);
    }

    private @Nullable MessageReference packageMessageResponse(@NotNull DataObject body) {

        if (body.hasKey("code")) {
            if (body.getInt("code") == 10015) {
                DiscordPS.debug("Webhook delivery returned 10015 (Unknown Webhook)");
                return null;
            }
        }

        try {
            // Note that channel_id and id returns the same snowflake for the first message of the thread
            String channelID = body.getString("channel_id");
            String messageID = body.getString("id");

            return new MessageReference(
                    Long.parseUnsignedLong(messageID),
                    Long.parseUnsignedLong(channelID),
                    this.guildID,
                    null,
                    this.jdaImpl
            );
        }
        catch (ParsingException ex) {
            DiscordPS.error("[Internal] Failed to parse webhook execution response data:", ex);
            return null;
        }
    }

    @FunctionalInterface
    interface WebhookExecution <T> {
        RestAction<Optional<T>> resolve();
    }

    /**
     * Mock {@link MessageChannel} to handle UNKNOWN_CHANNEL because forum channel is yet to support in JDA 4.
     */
    public static class ForumChannel extends TextChannelImpl {

        private final JDAImpl jda;

        public ForumChannel(long channelID, long guildID, JDAImpl jda) {
            super(channelID, new GuildImpl(jda, guildID));
            this.jda = jda;
        }
        @NotNull
        @Override
        public ChannelType getType() {
            return ChannelType.TEXT;
        }

        @NotNull
        @Override
        public JDA getJDA() {
            return this.jda;
        }
    }
}
