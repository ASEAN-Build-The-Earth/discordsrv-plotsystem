package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Submit a plot for review.
 */
public final class PlotSubmitEvent extends PlotEvent {

    /**
     * Create a new plot event by plot ID
     *
     * @param plotID The plot ID in integer
     */
    public PlotSubmitEvent(int plotID) {
        super(plotID);
    }
}