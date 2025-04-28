package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.ReceivedMessage;
import github.tintinkung.discordps.core.providers.WebhookProviderImpl;
import github.tintinkung.discordps.core.system.layout.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public interface ForumWebhookImpl {

    WebhookProviderImpl getProvider();

    @NotNull
    RestAction<Optional<DataObject>> editThreadChannelTags(
            @NotNull String channelID,
            @Nullable Collection<Long> appliedTags,
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
}
