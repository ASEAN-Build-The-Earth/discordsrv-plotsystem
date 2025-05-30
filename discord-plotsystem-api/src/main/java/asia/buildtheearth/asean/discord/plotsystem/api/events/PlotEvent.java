package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Superclass of all plot-system related events.
 *
 * <p>Sub Events (Abstract):</p>
 * <ul><li>{@link PlotReviewEvent}</li>
 * <li>{@link PlotClosureEvent}</li>
 * <li>{@link PlotUndoEvent}</li></ul>
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
