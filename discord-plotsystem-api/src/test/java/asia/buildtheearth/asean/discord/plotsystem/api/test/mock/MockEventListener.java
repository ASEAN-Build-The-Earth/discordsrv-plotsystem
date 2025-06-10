package asia.buildtheearth.asean.discord.plotsystem.api.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.api.ApiSubscribe;
import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple mock event listener to receive all callable {@link PlotEvent}.
 *
 * @see #popEvent(Class) Pop a received event
 * @see #received(Class) Check if an event is received
 */
public class MockEventListener {

    /**
     * Private event tracker, as a map of all received {@link PlotEvent} class and its event instance.
     */
    private final Map<Class<? extends  PlotEvent>, PlotEvent> receivedEvent = new HashMap<>();

    /**
     * Pop a received {@link PlotEvent} and return its instance.
     *
     * @param event The class of the received event to look for
     * @return The received instance if it has been received, else {@code Null}
     * @param <T> Type of the plot event to get
     */
    public <T extends PlotEvent> T popEvent(@NotNull Class<T> event) {
        PlotEvent received = this.receivedEvent.remove(event);
        return event.cast(received);
    }

    /**
     * Check if an event of type has been called and received.
     *
     * @param event The class of the event to look for
     * @return True if an event of the given class has been received
     * @param <T> Type of the plot event to get
     */
    public <T extends PlotEvent> boolean received(Class<T> event) {
        return this.receivedEvent.containsKey(event);
    }

    /**
     * Listener method for all {@link PlotEvent}
     *
     * @param event Listening event in the {@link PlotEvent} scope
     * @param <T> Type of the event that may get called
     */
    @ApiSubscribe
    @SuppressWarnings("unused")
    public <T extends PlotEvent> void onPlotEventCalled(@NotNull T event) {
        this.receivedEvent.put(event.getClass(), event);
    }
}
