package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * All available undo events.
 *
 * @see PlotUndoSubmitEvent
 * @see PlotUndoReviewEvent
 * @deprecated API is deprecated for official Plot-System's API module.
 *             DiscordPlotSystemAPI will no longer release starting by 1.3.0.
 */
@Deprecated(since = "1.2.5")
public abstract sealed class PlotUndoEvent extends PlotEvent
    permits PlotUndoSubmitEvent, PlotUndoReviewEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotUndoEvent(int plotID) {
        super(plotID);
    }
}
