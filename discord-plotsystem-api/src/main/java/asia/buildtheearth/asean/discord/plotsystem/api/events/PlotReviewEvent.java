package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Review related events.
 *
 * @see PlotFeedbackEvent
 * @see PlotApprovedEvent
 * @see PlotRejectedEvent
 */
public abstract sealed class PlotReviewEvent extends PlotEvent
    permits PlotApprovedEvent, PlotRejectedEvent, PlotFeedbackEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotReviewEvent(int plotID) {
        super(plotID);
    }
}
