package github.tintinkung.discordps.api;

import github.tintinkung.discordps.api.events.ApiEvent;
import org.bukkit.plugin.Plugin;

public interface DiscordPlotSystem extends Plugin {

    /**
     * Call the given event to all subscribed API listeners
     * @param event the event to be called
     * @return the event that was called
     */
    <E extends ApiEvent> E callEvent(E event);


    /**
     * Unsubscribe the given instance from DiscordSRV events
     * @param listener the instance to unsubscribe DiscordSRV events from
     * @return whether the instance was a listener
     */
    boolean unsubscribe(Object listener);
}
