package asia.buildtheearth.asean.discord.plotsystem.api;

import asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent;
import org.bukkit.plugin.Plugin;

/**
 * Plugin's API interface.
 *
 * @see #isReady()
 * @see #subscribe(Object)
 * @see #unsubscribe(Object)
 * @see #callEvent(ApiEvent)
 */
public interface DiscordPlotSystem extends Plugin {

    /**
     * Check is the plugin fully ready.
     *
     * <p>If and only if:</p>
     * <ul>
     *     <li>The main webhook has been initialized</li>
     *     <li>This plugin has subscribed to DiscordSRV</li>
     *     <li>This plugin is not shutting down</li>
     *     <li>IF debugging is enabled in the config file, Then the debugger MUST not detect any error</li>
     * </ul>
     * @return If the plugin is fully ready and functional.
     */
    boolean isReady();

    /**
     * Subscribe the given instance to DiscordPlotSystem events
     *
     * @param listener the instance to subscribe DiscordPlotSystem events to
     * @throws IllegalArgumentException if the object has zero methods that are annotated with {@link ApiSubscribe}
     */
    void subscribe(Object listener);

    /**
     * Unsubscribe the given instance from DiscordPlotSystem events
     *
     * @param listener the instance to unsubscribe DiscordPlotSystem events from
     * @return whether the instance was a listener
     */
    boolean unsubscribe(Object listener);

    /**
     * Call the given event to all subscribed API listeners
     *
     * @param event the event to be called
     * @return the event that was called
     * @param <E> The type of the API event to call
     * @see ApiEvent
     */
    <E extends ApiEvent> E callEvent(E event);
}
