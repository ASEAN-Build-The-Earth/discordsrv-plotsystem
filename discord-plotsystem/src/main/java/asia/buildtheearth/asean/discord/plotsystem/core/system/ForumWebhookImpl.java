package asia.buildtheearth.asean.discord.plotsystem.core.system;

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
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Method;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.AbstractWebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ForumWebhookImpl extends AbstractWebhookProvider implements ForumWebhook {
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
    public ForumWebhookImpl(JDA jda, @NotNull YamlConfiguration config, @NotNull YamlConfiguration forumConfig) throws IllegalArgumentException {
        super(config);
        this.forumConfig = forumConfig;
        this.jdaImpl = this.makeJdaImpl(jda);
        this.webhookReference = new Webhook.WebhookReference(this.getJDA(), this.webhookID, this.channelID);
        this.webhook = this.retrieveWebhook();
    }

    /**
     * Return the given JDA as implementation,
     * also do a mandatory check if jda is valid.
     *
     * @param jda JDA to get
     * @return JDA as implementation class
     */
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
    public WebhookProvider getProvider() { return this; }

    @Override
    public @NotNull Webhook.WebhookReference getWebhookReference(){
        return this.webhookReference;
    }

    @Override
    public JDAImpl getJDA() {
        return this.jdaImpl;
    }

    @Override
    public CompletableFuture<Void> validateWebhook(WebhookProvider... others) {

        List<CompletableFuture<?>> validateActions = this.validateWebhookReference(others);

        return CompletableFuture.allOf(validateActions.toArray(new CompletableFuture<?>[0])).handle((ok, error) -> {
            if(error != null)  DiscordPS.error(Debug.Error.WEBHOOK_VALIDATION_UNKNOWN_EXCEPTION, error.toString());
            // If webhook reference validation failed, tag validation is guarantee to fails too
            if(DiscordPS.getDebugger().hasGroup(Debug.ErrorGroup.WEBHOOK_REFS_VALIDATION)) {
                DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED);
                return CompletableFuture.<Void>completedFuture(null);
            } // Else validate for it with API calls
            else return this.validateWebhookTags();
        }).thenCompose(Function.identity());
    }

    /**
     * Validate for webhook reference in guild,
     * this request a guild data for each guild the bot is in
     * then check the webhook existence in those guild.
     *
     * @param others Other webhook to be validated within the same API call
     * @return Futures of each guild validation action
     */
    private @NotNull List<CompletableFuture<?>> validateWebhookReference(WebhookProvider... others) {
        // Fetch database to update webhooks every session
        List<CompletableFuture<?>> validateActions = new ArrayList<>();

        for(Guild guild : this.getJDA().getGuilds()) {

            Member selfMember = guild.getSelfMember();

            if (!selfMember.hasPermission(Permission.MANAGE_WEBHOOKS)) {
                DiscordPS.error(
                    Debug.Error.WEBHOOK_CAN_NOT_MANAGE,
            "Unable to manage webhooks guild-wide in " + guild
                    + ", please allow the permission MANAGE_WEBHOOKS"
                );
                continue;
            }

            validateActions.add(this.validateWebhooksInGuild(guild, selfMember, others));
        }

        return validateActions;
    }

    /**
     * Configure and resolved status tags does not have valid API reference yet,
     * this method fetch the tag reference from the webhook guild and apply tags base on configured name.
     *
     * @return Future to the validation action
     * @see AvailableTag#resolveAllTag(FileConfiguration)
     * @see AvailableTag#applyAllTag()
     */
    private CompletableFuture<Void> validateWebhookTags() {
        // Order matters here
        // 1st Resolve the tag enum to is corresponding config value
        this.resolveStatusTags();

        // 2nd validate its existence by fetching available tags from this webhook channel
        return this.getAvailableTags(true).submit().handle((optData, error) -> {
            if(error != null) DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED, error.getMessage(), error);
            else if(optData.isEmpty()) DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED,
                "Failed to fetch webhook channel data to validate available tags because API response returned empty.");
            else {
                DataArray availableTags = optData.get();
                AvailableTag.initCache(availableTags);
                AvailableTag.applyAllTag();
            }

            return null;
        });
    }

    /**
     * Resolve all configured status tags from forum config file,
     * output statically as {@link AvailableTag} which resolve for tag color and snowflake id.
     *
     * @see AvailableTag#resolveAllTag(FileConfiguration)
     */
    private void resolveStatusTags() {
        // Resolve the config path into tag enum
        try {
            AvailableTag.resolveAllTag(this.forumConfig);
        }
        catch (NoSuchElementException ex) {
            DiscordPS.error(Debug.Error.WEBHOOK_TAG_CONFIGURATION_FAILED, ex);
        }
    }

    /**
     * Fetch for available tags in this webhook channel.
     *
     * @param allowSecondAttempt To retry on 404 response
     * @return The request action that if success, return optional of raw data array.
     */
    @NotNull
    private RestAction<Optional<DataArray>> getAvailableTags(boolean allowSecondAttempt) {
        Route.CompiledRoute route = Route
            .get(Route.Channels.MODIFY_CHANNEL.getRoute())
            .compile(Long.toUnsignedString(this.channelID));

        return new RestActionImpl<>(this.getJDA(), route, (response, request) -> {
            try {
                int status = response.code;
                if (status == 404) {
                    if (allowSecondAttempt)
                        return retryWebhookExecution(() -> getAvailableTags(false));
                    request.cancel();

                    return Optional.empty();
                }
                Optional<DataObject> body = response.optObject();

                if (body.isPresent()) {
                    if (body.get().hasKey("code")) {
                        if (body.get().getInt("code") == 10015) {
                            DiscordPS.debug("Webhook delivery returned 10015 (Unknown Webhook)");
                            request.cancel();
                            return Optional.empty();
                        }
                    }

                    // Additional checks since this route does have channel data
                    // to fail if the channel isn't forum channel
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

    /**
     * Fetch guild for webhook information and validate if our webhook exist in the guild,
     * this also check for all bot created webhook if it is sync to the configured one or not.
     *
     * @param guild The guild to check for webhook
     * @param bot The plugin bot as discord member
     */
    private CompletableFuture<Void> validateWebhooksInGuild(@NotNull Guild guild, @NotNull Member bot, WebhookProvider... others) {

        Route.CompiledRoute route = Route.Guilds.GET_WEBHOOKS.compile(guild.getId());

        // Retrieve guild webhooks as raw data since this outdated api produce null value for newer webhook.
        return new RestActionImpl<List<DataObject>>(this.getJDA(), route, (response, request) -> {
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
        }).submit().thenAccept(webhooks -> {
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

                        boolean registered = false;

                        for(WebhookProvider other : others) {
                            if(id == other.getWebhookID() && channel == other.getChannelID()) {
                                DiscordPS.debug("Found configured webhook: " + name + ": " + username);
                                registered = true;
                                break;
                            }
                        }

                        if(!registered) {
                            // name:username(snowflake)
                            notRegistered.add(name + ":" + username + "(" + Long.toUnsignedString(id) + ")");
                            DiscordPS.warning("Found un-registered bot created webhook: " + name + ": " + username);
                        }
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

    @Override
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

                return components.flatMap(Layout::fromRawData);
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

    @NotNull
    public RestAction<Void> removeThreadMember(@NotNull String threadID,
                                               @NotNull String memberID) {
        return this.editThreadMember(Method.DELETE, threadID, memberID);
    }

    @NotNull
    public RestAction<Void> addThreadMember(@NotNull String threadID,
                                            @NotNull String memberID) {
        return this.editThreadMember(Method.PUT, threadID, memberID);
    }

    @NotNull
    public RestAction<Void> editThreadMember(@NotNull Method method,
                                             @NotNull String threadID,
                                             @NotNull String memberID) {
        // Borrow old endpoint replacing recipients as thread member
        // /channels/{channel_id}/thread-members/{user_id}
        String endpoint = Route.Channels.ADD_RECIPIENT.getRoute().replace("recipients", "thread-members");
        Route.CompiledRoute route = Route.custom(method, endpoint).compile(threadID, memberID);
        return new RestActionImpl<>(this.getJDA(), route);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public <T> CompletableFuture<Optional<MessageReference>> queueNewUpdateAction(
            @NotNull RestAction<Optional<T>> restAction,
            @NotNull Function<T, RestAction<Optional<MessageReference>>> whenComplete) {

        CompletableFuture<Optional<MessageReference>> action = new CompletableFuture<>();

        Consumer<T> whenActionComplete = data -> whenComplete.apply(data)
                .queue(action::complete, action::completeExceptionally);

        Runnable whenActionFailed = () -> action.completeExceptionally(
                new RuntimeException("update action returned empty")
        );

        Consumer<Optional<T>> onSuccess = opt -> opt.ifPresentOrElse(whenActionComplete, whenActionFailed);

        restAction.queue(onSuccess, action::completeExceptionally);

        return action;
    }

    /**
     * Retry rest action after 5 seconds.
     *
     * @param execution The rest action to execute
     * @return The supplied rest action that is called afet 5 seconds delay
     * @param <T> The rest action result type
     */
    private <T> @NotNull Optional<T> retryWebhookExecution(@NotNull Supplier<RestAction<Optional<T>>> execution) {
        DiscordPS.debug("Webhook delivery returned 404, trying again in 5 seconds");
        return execution.get().completeAfter(5, TimeUnit.SECONDS);
    }

    /**
     * Retrieve message reference from raw data object.
     *
     * @param body The data object response
     * @return The message reference as {@link MessageReference}
     */
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
}
