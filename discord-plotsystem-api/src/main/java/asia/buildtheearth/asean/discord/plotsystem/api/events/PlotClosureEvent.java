package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Plot event that will close
 * all plot interactions of the given plot ID.
 *
 * @see PlotAbandonedEvent
 * @see PlotArchiveEvent
 */
public abstract sealed class PlotClosureEvent extends PlotEvent
    permits PlotAbandonedEvent, PlotArchiveEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotClosureEvent(int plotID) {
        super(plotID);
    }
}
