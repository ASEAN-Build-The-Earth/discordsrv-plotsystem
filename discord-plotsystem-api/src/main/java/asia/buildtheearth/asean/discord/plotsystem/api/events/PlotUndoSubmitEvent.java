package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Undo a submitted plot and mark as ongoing.
 */
public final class PlotUndoSubmitEvent extends PlotUndoEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotUndoSubmitEvent(int plotID) {
        super(plotID);
    }
}
