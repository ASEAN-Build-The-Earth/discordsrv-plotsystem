package asia.buildtheearth.asean.discord.plotsystem.api.events;

import java.time.temporal.TemporalAccessor;

/**
 * Special event triggered to notify a plot owner about upcoming abandonment due to inactivity.
 *
 * <p>This event is not dispatched automatically by the API and must be triggered manually
 * by the implementation based on custom inactivity tracking logic.</p>
 *
 * <p>The {@link TemporalAccessor} timestamp indicates the scheduled abandonment time,
 * allowing system to determine how much time remains before the plot is considered inactive.</p>
 *
 * @see PlotNotificationEvent
 */
public final class InactivityNoticeEvent extends PlotNotificationEvent {

    TemporalAccessor timestamp;

    /**
     * Constructs a new {@link InactivityNoticeEvent} for the given plot.
     *
     * @param plotID    The ID of the plot this notification relates to
     * @param timestamp The timestamp at which the plot is expected to be marked as abandoned
     */
    @SuppressWarnings("deprecation")
    public InactivityNoticeEvent(int plotID, TemporalAccessor timestamp) {
        super(plotID, NotificationType.ON_INACTIVITY);
        this.timestamp = timestamp;
    }

    /**
     * Gets the scheduled abandonment timestamp of the plot.
     *
     * @return A {@link TemporalAccessor} indicating when the plot will be considered abandoned
     */
    public TemporalAccessor getTimestamp() {
        return this.timestamp;
    }
}
