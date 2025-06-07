package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Base class for all plot-system-related events.
 *
 * <p>Abstract Subclasses (internal grouping events)</p>
 * <ul>
 *     <li>{@link PlotReviewEvent} – events related to reviewing plots.</li>
 *     <li>{@link PlotClosureEvent} – events indicating that a plot is being closed or finalized.</li>
 *     <li>{@link PlotUndoEvent} – events triggered when undoing user actions.</li>
 * </ul>
 *
 * @see PlotCreateEvent
 * @see PlotSubmitEvent
 * @see PlotAbandonedEvent
 * @see PlotReclaimEvent
 * @see PlotApprovedEvent
 * @see PlotRejectedEvent
 * @see PlotFeedbackEvent
 * @see PlotReclaimEvent
 * @see PlotUndoSubmitEvent
 * @see PlotUndoReviewEvent
 * @see PlotArchiveEvent
 * @see PlotNotificationEvent
 */
public abstract class PlotEvent extends ApiEvent {

    private final int plotID;

    /**
     * Create a new plot event by plot ID
     * @param plotID The plot ID in integer
     */
    public PlotEvent(int plotID) {
        this.plotID = plotID;
    }

    /**
     * Get the plot ID of this plot Event
     *
     * @return The plot ID in integer
     */
    public int getPlotID() {
        return plotID;
    }
}
