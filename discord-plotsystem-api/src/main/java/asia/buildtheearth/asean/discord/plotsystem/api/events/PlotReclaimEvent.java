package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Reclaim an abandoned plot with a new owner.
 */
public final class PlotReclaimEvent extends PlotEvent implements ScopedEvent {

    private final String owner;

    /**
     * Construct a plot-reclaim event by an owner.
     *
     * @param plotID The plot ID in integer
     * @param owner The owner which will be reclaiming this plot ID,
     *              can be any identifiable string value.
     */
    public PlotReclaimEvent(int plotID, String owner) {
        super(plotID);

        this.owner = owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOwner() {
        return owner;
    }
}
