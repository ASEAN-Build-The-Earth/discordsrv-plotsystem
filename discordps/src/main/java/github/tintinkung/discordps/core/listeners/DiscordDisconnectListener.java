package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.dependencies.jda.api.events.DisconnectEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.ShutdownEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.requests.CloseCode;
import github.tintinkung.discordps.DiscordPS;
import org.jetbrains.annotations.NotNull;

/**
 * DiscordSRV on disconnect does not call any API event, so we have to handle it our own too.
 * @see <a href="https://github.com/DiscordSRV/DiscordSRV/blob/9d4734818ab27069d76f264a4cda74a699806770/src/main/java/github/scarsz/discordsrv/listeners/DiscordDisconnectListener.java#L33">
 *     github.scarsz.discordsrv.listeners.DiscordDisconnectListener
 * </a>
 */
public class DiscordDisconnectListener extends ListenerAdapter {

    public static CloseCode mostRecentCloseCode = null;

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        handleCode(event.getCloseCode());
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        handleCode(event.getCloseCode());
    }

    private void handleCode(CloseCode closeCode) {
        if (closeCode == null) {
            return;
        }
        mostRecentCloseCode = closeCode;
        if (closeCode == CloseCode.DISALLOWED_INTENTS || !closeCode.isReconnect()) {
            DiscordPS.error("Please check if DiscordSRV plugin is loaded correctly ...disabling plugin");
            DiscordPS.getPlugin().disablePlugin("Discord Plot System cannot connect to DiscordSRV");
        }
    }

}
