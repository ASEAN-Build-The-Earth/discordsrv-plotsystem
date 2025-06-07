package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Review the plot and mark as approved.
 *
 * @see PlotFeedbackEvent To set review message
 */
public final class PlotApprovedEvent extends PlotReviewEvent{

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotApprovedEvent(int plotID) {
        super(plotID);
    }
}
