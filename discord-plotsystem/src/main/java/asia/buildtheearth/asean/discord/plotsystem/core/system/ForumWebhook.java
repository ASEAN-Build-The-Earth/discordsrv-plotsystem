package asia.buildtheearth.asean.discord.plotsystem.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
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
import java.util.function.Function;

public interface ForumWebhook {

    WebhookProvider getProvider();

    @NotNull
    RestAction<Optional<DataObject>> editThreadChannelTags(
            @NotNull String channelID,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<DataObject>> modifyThreadChannel(
            @NotNull String channelID,
            @Nullable String name,
            @Nullable Collection<Long> appliedTags,
            @Nullable Integer autoArchiveDuration,
            @Nullable Boolean locked,
            @Nullable Boolean archived,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<MessageReference>> editThreadMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<MessageReference>> editInitialThreadMessage(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<MessageReference>> newThreadFromWebhook(
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            @Nullable Collection<Long> appliedTags,
            boolean withComponents,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<ReceivedMessage>> getWebhookMessage(
            @NotNull String threadID,
            @NotNull String messageID,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Optional<Layout>> getInitialLayout(
            @NotNull String threadID,
            boolean allowSecondAttempt);

    @NotNull RestAction<Optional<MessageReference>> sendMessageInThread(
            @NotNull String threadID,
            @NotNull WebhookDataBuilder.WebhookData webhookData,
            boolean withComponents,
            boolean allowSecondAttempt);

    @NotNull
    RestAction<Void> removeThreadMember(@NotNull String threadID,
                                        @NotNull String memberID);

    @NotNull
    RestAction<Void> addThreadMember(@NotNull String threadID,
                                     @NotNull String memberID);

    /**
     * Queue a rest action followed by another rest action
     *
     * @param restAction The rest action to starts with
     * @param whenComplete The follow-up action to do after the first action complete successfully, invoking with its result.
     * @return The combined action that is completed with the follow-up action
     * @param <T> The type of the action that will be resolved to
     */
    @NotNull
    <T> CompletableFuture<Optional<MessageReference>> queueNewUpdateAction(
            @NotNull RestAction<Optional<T>> restAction,
            @NotNull Function<T, RestAction<Optional<MessageReference>>> whenComplete);
}
