package github.tintinkung.discordps.api;

import github.tintinkung.discordps.api.events.ApiEvent;
import org.bukkit.plugin.Plugin;

public interface DiscordPlotSystem extends Plugin {

    /**
     * Subscribe the given instance to DiscordPlotSystem events
     * @param listener the instance to subscribe DiscordSRV events to
     * @throws IllegalArgumentException if the object has zero methods that are annotated with {@link ApiSubscribe}
     */
    void subscribe(Object listener);

    /**
     * Unsubscribe the given instance from DiscordSRV events
     * @param listener the instance to unsubscribe DiscordSRV events from
     * @return whether the instance was a listener
     */
    boolean unsubscribe(Object listener);

    /**
     * Call the given event to all subscribed API listeners
     * @param event the event to be called
     * @return the event that was called
     */
    <E extends ApiEvent> E callEvent(E event);
}
