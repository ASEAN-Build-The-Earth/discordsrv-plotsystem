package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.ForumWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.RateLimitedException;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.ReceivedMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class MockForumWebhook implements ForumWebhook {

    /**
     * Generate a random message reference in the forum channel
     *
     * @return new message reference
     */
    @Contract(" -> new")
    public @NotNull MessageReference randomMessage() {
        return new MessageReference(
            MockSnowflake.getRandom(),
            this.getProvider().getChannelID(),
            MockSnowflake.MAIN_GUILD.getMockID(),
            null,
            DiscordPS.getPlugin().getJDA()
        );
    }

    @Override
    public abstract WebhookProvider getProvider();

    @Override
    public @NotNull RestAction<Optional<DataObject>> editThreadChannelTags(@NotNull String channelID, @Nullable Collection<Long> appliedTags, boolean allowSecondAttempt) {
        return new MockOptRestAction<>();
    }

    @Override
    public @NotNull RestAction<Optional<DataObject>> modifyThreadChannel(@NotNull String channelID, @Nullable String name, @Nullable Collection<Long> appliedTags, @Nullable Integer autoArchiveDuration, @Nullable Boolean locked, @Nullable Boolean archived, boolean allowSecondAttempt) {
        return new MockOptRestAction<>();
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> editInitialThreadMessage(@NotNull String threadID, WebhookDataBuilder.@NotNull WebhookData webhookData, boolean allowSecondAttempt) {
        return new MockOptRestAction<>(randomMessage());
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> editThreadMessage(@NotNull String threadID, @NotNull String messageID, WebhookDataBuilder.@NotNull WebhookData webhookData, boolean allowSecondAttempt) {
        return new MockOptRestAction<>(randomMessage());
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> editInitialThreadMessage(@NotNull String threadID, WebhookDataBuilder.@NotNull WebhookData webhookData, boolean withComponents, boolean allowSecondAttempt) {
        return new MockOptRestAction<>(randomMessage());
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> newThreadFromWebhook(WebhookDataBuilder.@NotNull WebhookData webhookData, @Nullable Collection<Long> appliedTags, boolean allowSecondAttempt) {
        return new MockOptRestAction<>(randomMessage());
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> newThreadFromWebhook(WebhookDataBuilder.@NotNull WebhookData webhookData, @Nullable Collection<Long> appliedTags, boolean withComponents, boolean allowSecondAttempt) {
        return new MockOptRestAction<>(randomMessage());
    }

    @Override
    public @NotNull RestAction<Optional<ReceivedMessage>> getWebhookMessage(@NotNull String threadID, @NotNull String messageID, boolean allowSecondAttempt) {
        return new MockOptRestAction<>();
    }

    @Override
    public @NotNull RestAction<Optional<Layout>> getInitialLayout(@NotNull String threadID, boolean allowSecondAttempt) {
        return new MockOptRestAction<>();
    }

    @Override
    public @NotNull RestAction<Optional<MessageReference>> sendMessageInThread(@NotNull String threadID, WebhookDataBuilder.@NotNull WebhookData webhookData, boolean withComponents, boolean allowSecondAttempt) {
        return new MockOptRestAction<>(randomMessage());
    }

    @Override
    public @NotNull RestAction<Void> removeThreadMember(@NotNull String threadID, @NotNull String memberID) {
        return new MockRestAction<>();
    }

    @Override
    public @NotNull RestAction<Void> addThreadMember(@NotNull String threadID, @NotNull String memberID) {
        return new MockRestAction<>();
    }

    @Override
    public @NotNull <T> CompletableFuture<Optional<MessageReference>> queueNewUpdateAction(@NotNull RestAction<Optional<T>> restAction, @NotNull Function<T, RestAction<Optional<MessageReference>>> whenComplete) {
        return new MockOptRestAction<MessageReference>().submit();
    }

    public static class MockRestAction<T> implements RestAction<T> {
        @NotNull
        @Override
        public JDA getJDA() {
            return MockDiscordPS.getPlugin().getJDA();
        }

        @NotNull
        @Override
        public RestAction<T> setCheck(@Nullable BooleanSupplier booleanSupplier) {
            return this;
        }

        @Override
        public void queue(@Nullable Consumer<? super T> consumer, @Nullable Consumer<? super Throwable> failure) {
            if(consumer != null) consumer.accept(null);
            if(failure != null) failure.accept(null);
        }

        @Override
        public T complete(boolean b) throws RateLimitedException {
            return null;
        }

        @NotNull
        @Override
        public CompletableFuture<T> submit(boolean b) {
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class MockOptRestAction<T> extends MockRestAction<Optional<T>> {
        final T mock;

        public MockOptRestAction(T mockObject) {
            this.mock = mockObject;
        }

        public MockOptRestAction() {
            this.mock = null;
        }

        @Override
        public Optional<T> complete(boolean b) throws RateLimitedException {
            return Optional.ofNullable(this.mock);
        }

        @NotNull
        @Override
        public CompletableFuture<Optional<T>> submit(boolean b) {
            return CompletableFuture.completedFuture(Optional.ofNullable(this.mock));
        }

        @Override
        public void queue(@Nullable Consumer<? super Optional<T>> consumer, @Nullable Consumer<? super Throwable> failure) {
            if(consumer != null) consumer.accept(Optional.ofNullable(this.mock));
            if(failure != null) failure.accept(null);
        }
    }
}
