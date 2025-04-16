package github.tintinkung.discordps.core.listeners;

import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.providers.PluginProvider;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class PluginLoadedListener extends PluginProvider implements Listener {

    public PluginLoadedListener(DiscordPS plugin) {
        super(plugin);
    }

    public void onPluginEnable(PluginEnableEvent event) {
        if (DiscordPS.DISCORD_SRV_SYMBOL.equals(event.getPlugin().getName())) {
            DiscordPS.info("DiscordSRV loaded late: " + event.getPlugin());
            plugin.subscribeToDiscordSRV(event.getPlugin());
        }

        if (DiscordPS.PLOT_SYSTEM_SYMBOL.equals(event.getPlugin().getName())) {
            DiscordPS.info("Plot-System loaded late: " + event.getPlugin());
            plugin.subscribeToPlotSystemUtil();
        }
    }
}