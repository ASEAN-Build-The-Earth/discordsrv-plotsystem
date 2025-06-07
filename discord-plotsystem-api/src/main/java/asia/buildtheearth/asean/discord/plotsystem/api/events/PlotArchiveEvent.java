package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Archive event for usage internally.
 */
public final class PlotArchiveEvent extends PlotClosureEvent implements ScopedEvent {

    private final String owner;

    /**
     * Construct a plot-archive event by an owner.
     *
     * @param plotID The plot ID in integer
     * @param owner The owner which archive this plot ID,
     *              can be any identifiable string value.
     */
    public PlotArchiveEvent(int plotID, String owner) {
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
