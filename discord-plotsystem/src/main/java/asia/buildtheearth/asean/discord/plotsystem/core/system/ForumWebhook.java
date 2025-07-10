package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.requests.Request;
import github.scarsz.discordsrv.dependencies.jda.api.requests.Response;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.ReceivedMessage;
import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Manager interface for interacting with Discord Forum Channels via a webhook context.
 *
 * <p>Most methods return a {@code RestAction<Optional<T>>} where the {@link Optional} will be:</p>
 * <ul>
 *   <li><b>Present</b>: when the operation completes successfully and a response object is available.</li>
 *   <li><b>Empty</b>: when the operation fails silently (e.g., due to permissions, missing resources, or Discord API conditions)
 *       and the failure is not considered an exceptional case.</li>
 * </ul>
 *
 * <p>
 * {@code allowSecondAttempt} parameter. When enabled, the operation will retry
 * once on known failures usually returns from response object.
 * </p>
 */
public interface ForumWebhook {

    /**
     * Get the provider for this forum webhook.
     *
     * @return The webhook provider of this forum.
     */
    WebhookProvider getProvider();

    /**
     * Edits the applied tags of a thread channel.
     *
     * @param channelID          ID of the thread channel.
     * @param appliedTags        Collection of tag IDs to apply, or null to leave unchanged.
     * @param allowSecondAttempt Whether to retry once if the request fails.
     * @return A RestAction holding an Optional containing the response data.
     */
    @NotNull
    RestAction<Optional<DataObject>> editThreadChannelTags(
            @NotNull String channelID,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt);

    /**
     * Modifies properties of a thread channel.
     *
     * @param channelID           ID of the thread channel.
     * @param name                New name for the thread, or null to leave unchanged.
     * @param appliedTags         Collection of tag IDs to apply, or null to leave unchanged.
     * @param autoArchiveDuration Auto-archive duration in minutes, or null to leave unchanged.
     * @param locked              Whether the thread should be locked, or null to leave unchanged.
     * @param archived            Whether the thread should be archived, or null to leave unchanged.
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional containing the response data.
     */
    @NotNull
    RestAction<Optional<DataObject>> modifyThreadChannel(
            @NotNull String channelID,
            @Nullable String name,
            @Nullable Collection<Long> appliedTags,
            @Nullable Integer autoArchiveDuration,
            @Nullable Boolean locked,
            @Nullable Boolean archived,
            boolean allowSecondAttempt);

    /**
     * Edits the initial message in a thread using webhook data.
     *
     * @param threadID            ID of the thread.
     * @param webhookData         Webhook message data.
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the edited message reference.
     */
    @NotNull
    RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt);

    /**
     * Edits a specific message in a thread using webhook data.
     *
     * @param threadID            ID of the thread.
     * @param messageID           ID of the message to edit.
     * @param webhookData         Webhook message data.
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the edited message reference.
     */
    @NotNull
    RestAction<Optional<MessageReference>> editThreadMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt);

    /**
     * Edits the initial message in a thread with optional components.
     *
     * @param threadID            ID of the thread.
     * @param webhookData         Webhook message data.
     * @param withComponents      Whether to forcefully include message components (e.g., buttons).
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the edited message reference.
     */
    @NotNull
    RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt);


    /**
     * Starts a new thread and sends an initial webhook message.
     *
     * @param webhookData         Webhook message data.
     * @param appliedTags         Collection of tag IDs to apply, or null.
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the created message reference.
     */
    @NotNull
    RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt);

    /**
     * Starts a new thread with optional components in the initial webhook message.
     *
     * @param webhookData         Webhook message data.
     * @param appliedTags         Collection of tag IDs to apply, or null.
     * @param withComponents      Whether to forcefully include message components (e.g., buttons).
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the created message reference.
     */
    @NotNull
    RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean withComponents,
            boolean allowSecondAttempt);

    /**
     * Retrieves a specific webhook message from a thread.
     *
     * @param threadID            ID of the thread.
     * @param messageID           ID of the message to retrieve.
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the received message.
     */
    @NotNull
    RestAction<Optional<ReceivedMessage>> getWebhookMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            boolean allowSecondAttempt);

    /**
     * Retrieves the layout of the initial message in a thread.
     *
     * @param threadID            ID of the thread.
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the layout.
     */
    @NotNull
    RestAction<Optional<Layout>> getInitialLayout(
            @NotNull String threadID,
            boolean allowSecondAttempt);

    /**
     * Sends a new webhook message in an existing thread.
     *
     * @param threadID            ID of the thread.
     * @param webhookData         Webhook message data.
     * @param withComponents      Whether to forcefully include message components (e.g., buttons).
     * @param allowSecondAttempt  Whether to retry once if the request fails.
     * @return A RestAction holding an Optional with the sent message reference.
     */
    @NotNull
    RestAction<Optional<MessageReference>> sendMessageInThread(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt);

    /**
     * Removes a member from the specified thread.
     *
     * @param threadID ID of the thread.
     * @param memberID ID of the member to remove.
     * @return A RestAction representing the completion of the request.
     */
    @NotNull
    RestAction<Void> removeThreadMember(@NotNull String threadID,
                                        @NotNull String memberID);


    /**
     * Adds a member to the specified thread.
     *
     * @param threadID ID of the thread.
     * @param memberID ID of the member to add.
     * @return A RestAction representing the completion of the request.
     */
    @NotNull
    RestAction<Void> addThreadMember(@NotNull String threadID,
                                     @NotNull String memberID);

    /**
     * Queue a rest action followed by another rest action.
     *
     * @param restAction The rest action to starts with.
     * @param whenComplete The follow-up action to do after the first action complete successfully, invoking with its result.
     * @return The combined action that is completed with the follow-up action.
     * @param <T> The type of the action that will be resolved to.
     */
    @NotNull
    <T> CompletableFuture<Optional<MessageReference>> queueNewUpdateAction(
            @NotNull RestAction<Optional<T>> restAction,
            @NotNull Function<T, RestAction<Optional<MessageReference>>> whenComplete);

    /**
     * Conventional utility class to handle rest action response.
     *
     * @param <T> Type of the return value of this response.
     */
    class RestResponse<T> {
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