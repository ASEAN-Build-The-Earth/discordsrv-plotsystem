package asia.buildtheearth.asean.discord.plotsystem.core.system;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.AccountType;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.AccountTypeException;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.requests.Request;
import github.scarsz.discordsrv.dependencies.jda.api.requests.Response;
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
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.AbstractWebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation for forum webhook functionalities.
 *
 * @see WebhookProvider
 * @see ForumWebhook
 */
public class ForumWebhookImpl extends AbstractWebhookProvider implements ForumWebhook {
    private final JDAImpl jdaImpl;
    private final Webhook webhook;
    private final Webhook.WebhookReference webhookReference;
    private final MessageChannel channel;

    /**
     * Initialize Webhook provider with a given configuration
     *
     * @param config Yaml Configuration file of this webhook
     * @throws IllegalArgumentException If the given configuration is invalid for webhook ID and its channel ID
     */
    public ForumWebhookImpl(@NotNull JDA jda,
                            @NotNull YamlConfiguration config) throws IllegalArgumentException {
        super(config);
        this.jdaImpl = this.makeJdaImpl(jda);
        this.channel = new TextChannelImpl(this.channelID, new GuildImpl(this.jdaImpl, this.guildID));
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
        catch (AccountTypeException ex) { throw new IllegalArgumentException(ex); }

        return (JDAImpl) jda;
    }

    /** {@inheritDoc} */
    @Override
    public Webhook getWebhook() {
        return this.webhook;
    }

    /** {@inheritDoc} */
    @Override
    public WebhookProvider getProvider() { return this; }

    /** {@inheritDoc} */
    @Override
    public @NotNull Webhook.WebhookReference getWebhookReference(){
        return this.webhookReference;
    }

    /** {@inheritDoc} */
    @Override
    public JDAImpl getJDA() {
        return this.jdaImpl;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<DataObject>> editThreadChannelTags(
            @NotNull String channelID,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt) {
        return this.modifyThreadChannel(channelID, null, appliedTags, null, null, null, allowSecondAttempt);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt) {
        return this.editWebhookMessage(threadID, threadID, webhookData, false, allowSecondAttempt);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<MessageReference>> editThreadMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt) {
        return this.editWebhookMessage(threadID, messageID, webhookData, false, allowSecondAttempt);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt) {
        return this.editWebhookMessage(threadID, threadID, webhookData, withComponents, allowSecondAttempt);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt) {
        return this.newThreadWithMessage(webhookData, appliedTags, false, allowSecondAttempt);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean withComponents,
            boolean allowSecondAttempt) {
        return this.newThreadWithMessage(webhookData, appliedTags, false, allowSecondAttempt);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<DataObject>> modifyThreadChannel(
            @NotNull String channelID,
            @Nullable String name,
            @Nullable Collection<Long> appliedTags,
            @Nullable Integer autoArchiveDuration,
            @Nullable Boolean locked,
            @Nullable Boolean archived,
            boolean allowSecondAttempt) {

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
        RestResponse<DataObject> response = new RestResponse<>(Function.identity());

        if(allowSecondAttempt) response.setRetryExecution(() -> modifyThreadChannel(channelID, name, appliedTags, autoArchiveDuration, locked, archived, false));

        return new RestActionImpl<>(this.jdaImpl, route, requestBody, response::execute);
    }

    public @NotNull RestAction<Optional<MessageReference>> editWebhookMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt) {

        Checks.isSnowflake(threadID, "Webhook thread ID");
        Checks.isSnowflake(messageID, "Webhook message ID");

        Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK_EDIT
                .compile(webhook.getId(), webhook.getToken(), messageID)
                .withQueryParams("thread_id", threadID)
                .withQueryParams("with_components", String.valueOf(withComponents));

        MultipartBody requestBody = webhookData.prepareRequestBody();
        RestResponse<MessageReference> response = new RestResponse<>(this::packageMessageResponse);

        if(allowSecondAttempt) response.setRetryExecution(() -> editWebhookMessage(threadID, messageID, webhookData, withComponents, false));

        return new RestActionImpl<>(this.jdaImpl, route, requestBody, response::execute);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<ReceivedMessage>> getWebhookMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            boolean allowSecondAttempt) {

        Checks.isSnowflake(threadID, "Webhook thread ID");
        Checks.isSnowflake(messageID, "Webhook message ID");

        Route.CompiledRoute route = Route.get(Route.Webhooks.EXECUTE_WEBHOOK_EDIT.getRoute())
                .compile(webhook.getId(), webhook.getToken(), messageID)
                .withQueryParams("thread_id", threadID);

        RestResponse<ReceivedMessage> response = new RestResponse<>(data -> this.jdaImpl
            .getEntityBuilder()
            .createMessage(data, this.channel, false)
        );

        if(allowSecondAttempt) response.setRetryExecution(() -> getWebhookMessage(threadID, messageID, false));

        return new RestActionImpl<>(this.jdaImpl, route, response::execute);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull RestAction<Optional<Layout>> getInitialLayout(
            @NotNull String threadID,
            boolean allowSecondAttempt) {

        Checks.isSnowflake(threadID, "Webhook thread ID");

        Route.CompiledRoute route = Route.get(Route.Webhooks.EXECUTE_WEBHOOK_EDIT.getRoute())
                .compile(webhook.getId(), webhook.getToken(), threadID)
                .withQueryParams("thread_id", threadID);

        RestResponse<Layout> response = new RestResponse<>(data -> data
            .optArray("components")
            .flatMap(Layout::fromRawData)
            .orElse(null)
        );

        if(allowSecondAttempt) response.setRetryExecution(() -> getInitialLayout(threadID, false));

        return new RestActionImpl<>(this.jdaImpl, route, response::execute);
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
            RestResponse<MessageReference> response = new RestResponse<>(this::packageMessageResponse);

            if(allowSecondAttempt) response.setRetryExecution(() -> newThreadWithMessage(webhookData, appliedTags, withComponents, false));

            return new RestActionImpl<>(this.jdaImpl, route, requestBody, response::execute);
    }

    /** {@inheritDoc} */
    @Override
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

        MultipartBody requestBody = webhookData.prepareRequestBody();

        RestResponse<MessageReference> response = new RestResponse<>(this::packageMessageResponse);

        if(allowSecondAttempt) response.setRetryExecution(() ->  sendMessageInThread(threadID, webhookData, withComponents, false));

        return new RestActionImpl<>(this.jdaImpl, route, requestBody, response::execute);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RestAction<Void> removeThreadMember(@NotNull String threadID,
                                               @NotNull String memberID) {
        return this.editThreadMember(Method.DELETE, threadID, memberID);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
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

    /** {@inheritDoc} */
    @NotNull
    @Override
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
     * Retrieve message reference from raw data object.
     *
     * @param body The data object response
     * @return The message reference as {@link MessageReference}
     */
    private @Nullable MessageReference packageMessageResponse(@NotNull DataObject body) {
        try {
            // Note that channel_id and id returns the same snowflake for the first message of the thread
            String channelID = body.getString("channel_id", null);
            String messageID = body.getString("id", null);

            if(channelID == null || messageID == null) {
                DiscordPS.error(
                    "Trying to parse forum webhook message response of invalid data "
                    + "(channel: " + channelID + ", message: " + messageID + ')'
                );
                return null;
            }

            return new MessageReference(
                    Long.parseUnsignedLong(messageID),
                    Long.parseUnsignedLong(channelID),
                    this.guildID,
                    null,
                    this.jdaImpl
            );
        }
        catch (ParsingException ex) {
            DiscordPS.error("Failed to parse forum webhook message response", ex);
            return null;
        }
    }

    /**
     * Conventional utility class to handle rest action response.
     *
     * @param <T> Type of the return value of this response.
     */
    public static class RestResponse<T> {
        private static final long RETRY_AFTER_MILLIS = 5000;
        private @Nullable Supplier<RestAction<Optional<T>>> retryExecution;
        private final @NotNull Function<@NotNull DataObject, @Nullable T> response;

        /**
         * Create a rest action {@link DataObject} response.
         *
         * @param response The response function.
         */
        public RestResponse(@NotNull Function<@NotNull DataObject, @Nullable T> response) {
            this.response = response;
        }

        /**
         * Provide this response with retry supplier.
         *
         * <p>The supplier will be executed once if a bad response is returned from discord api.</p>
         *
         * @param execution The execution that return this respective response.
         */
        public void setRetryExecution(@Nullable Supplier<RestAction<Optional<T>>> execution) {
            this.retryExecution = execution;
        }

        /**
         * Execute this rest action response.
         *
         * <p>Execution is checked for a retry if {@link Response#isOk()} returns {@code false}.
         * Result is then received via the response function given by class constructor
         * (invoked by response body if present).
         * </p>
         *
         * @param response Response from discord API
         * @return The execution result handled with {@linkplain Optional}.
         */
        public Optional<T> execute(Response response, Request<Optional<T>> ignored) {
            try {
                if(!response.isOk()) {
                    if(this.retryExecution == null) return Optional.empty();

                    // Not sure with the threading on this one, in theory it should block the rest action thread for 5 seconds.
                    DiscordPS.debug("Discord returned bad response for Forum webhook execution, trying again in 5 seconds");
                    return this.retryExecution.get().completeAfter(RETRY_AFTER_MILLIS, TimeUnit.MILLISECONDS);
                }

                return response.optObject().map(this.response);
            }
            catch (Throwable ex) {
                DiscordPS.error("Failed to execute API response of a webhook forum process", ex);
                return Optional.empty();
            }
        }
    }
}