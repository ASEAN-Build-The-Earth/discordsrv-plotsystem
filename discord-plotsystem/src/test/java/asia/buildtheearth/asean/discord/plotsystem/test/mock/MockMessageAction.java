package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.RateLimitedException;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.MessageAction;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.restaction.MessageActionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MockMessageAction extends MessageActionImpl {
    public MockMessageAction(JDA api, String messageId, MessageChannel channel) {
        super(api, messageId, channel);
    }

    @NotNull
    @Override
    public JDA getJDA() {
        return DiscordPS.getPlugin().getJDA();
    }

    @Override
    public void queue(@Nullable Consumer<? super Message> consumer, @Nullable Consumer<? super Throwable> failure) {
        if(consumer != null) consumer.accept(null);
        if(failure != null) failure.accept(null);
    }

    @Override
    public Message complete(boolean b) throws RateLimitedException {
        return null;
    }

    @NotNull
    @Override
    public CompletableFuture<Message> submit(boolean b) {
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    @Override
    public MessageAction setEmbeds(@NotNull MessageEmbed... embeds) {
        return this;
    }
}
