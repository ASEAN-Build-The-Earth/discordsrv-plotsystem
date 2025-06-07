package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordPS.getPlugin(), () -> {

            // TODO: Notify if the joined player have un-reviewed plot

        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDisconnect(PlayerQuitEvent event) {

        // TODO: Notify if player left the game with new submitted plot
    }
}

