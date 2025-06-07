package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Review the plot and mark as rejected.
 *
 * @see PlotFeedbackEvent To set review message
 */
public final class PlotRejectedEvent extends PlotReviewEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotRejectedEvent(int plotID) {
        super(plotID);
    }
}
