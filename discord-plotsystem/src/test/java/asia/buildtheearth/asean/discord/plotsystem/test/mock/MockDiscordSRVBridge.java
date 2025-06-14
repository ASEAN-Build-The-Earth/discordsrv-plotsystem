package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.DiscordSRVBridge;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;

public interface MockDiscordSRVBridge extends DiscordSRVBridge {

    @Override
    default void subscribeSRV(Object listener) {
        MockDiscordSRV.api.subscribe(listener);
    }

    @Override
    default boolean unsubscribeSRV(Object listener) {
        return MockDiscordSRV.api.unsubscribe(listener);
    }

    @Override
    default void  addSlashCommandProvider(SlashCommandProvider provider) {
        MockDiscordSRV.api.addSlashCommandProvider(provider);
    }

    @Override
    default Guild getMainGuild() {
        return MockDiscordSRV.getPlugin().getMainGuild();
    }

    @Override
    default JDA getJDA() {
        return MockDiscordSRV.getPlugin().getJda();
    }

    @Override
    default void updateSlashCommands() { }
}
