package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Review related events
 *
 * @see PlotFeedbackEvent
 * @see PlotApprovedEvent
 * @see PlotRejectedEvent
 */
public abstract sealed class PlotReviewEvent extends PlotEvent
    permits PlotApprovedEvent, PlotRejectedEvent, PlotFeedbackEvent {

    public PlotReviewEvent(int plotID) {
        super(plotID);
    }
}
