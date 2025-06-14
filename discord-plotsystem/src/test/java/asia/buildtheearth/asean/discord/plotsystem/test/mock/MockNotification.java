package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;

import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.MessageAction;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.GuildImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.TextChannelImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MockNotification extends TextChannelImpl {
    public MockNotification(GuildImpl guild) {
        super(MockSnowflake.NOTIFICATION.getMockID(), guild);
    }

    @NotNull
    @Override
    public JDA getJDA() {
        return DiscordPS.getPlugin().getJDA();
    }

    @NotNull
    @Override
    public MessageAction sendMessage(@NotNull CharSequence text) {
        return new MockMessageAction(getJDA(), Long.toUnsignedString(MockSnowflake.getRandom()), this);
    }

    @NotNull
    @Override
    public MessageAction sendMessageEmbeds(@NotNull MessageEmbed embed, @NotNull MessageEmbed... other) {
        return new MockMessageAction(getJDA(), Long.toUnsignedString(MockSnowflake.getRandom()), this);
    }

    @NotNull
    @Override
    public MessageAction sendMessageEmbeds(@NotNull Collection<? extends MessageEmbed> embeds) {
        return new MockMessageAction(getJDA(), Long.toUnsignedString(MockSnowflake.getRandom()), this);
    }
}
