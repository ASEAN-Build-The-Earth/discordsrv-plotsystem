package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Undo a plot review and mark the plot back as finished.
 */
public final class PlotUndoReviewEvent extends PlotUndoEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotUndoReviewEvent(int plotID) {
        super(plotID);
    }
}
