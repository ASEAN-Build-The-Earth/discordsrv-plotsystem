package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Review related events.
 *
 * @see PlotFeedbackEvent
 * @see PlotApprovedEvent
 * @see PlotRejectedEvent
 * @deprecated API is deprecated for official Plot-System's API module.
 *             DiscordPlotSystemAPI will no longer release starting by 1.3.0.
 */
@Deprecated(since = "1.2.5")
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
