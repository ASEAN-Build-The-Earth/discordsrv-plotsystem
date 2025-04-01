package github.tintinkung.discordps.core.listeners;

import github.tintinkung.discordps.DiscordPS;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

@SuppressWarnings("unused")
public class DiscordSRVLoadedListener implements Listener {
    private final DiscordPS plugin;

    public DiscordSRVLoadedListener(DiscordPS plugin) {
        this.plugin = plugin;
    }

    public void onPluginEnable(PluginEnableEvent event) {
        if (DiscordPS.DISCORD_SRV.equals(event.getPlugin().getName())) {
            DiscordPS.info("DiscordSRV loaded late: " + event.getPlugin());
            plugin.subscribeToDiscordSrv(event.getPlugin());
        }
    }
}