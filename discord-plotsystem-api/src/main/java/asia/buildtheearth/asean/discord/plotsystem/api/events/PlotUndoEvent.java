package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * All available undo events.
 *
 * @see PlotUndoSubmitEvent
 * @see PlotUndoReviewEvent
 */
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
