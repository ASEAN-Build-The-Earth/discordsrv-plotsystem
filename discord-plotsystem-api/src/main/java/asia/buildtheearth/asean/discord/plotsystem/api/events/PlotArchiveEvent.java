package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Archive event for usage internally.
 */
public final class PlotArchiveEvent extends PlotClosureEvent implements ScopedEvent {

    private final String owner;

    public PlotArchiveEvent(int plotID, String owner) {
        super(plotID);

        this.owner = owner;
    }

    @Override
    public String getOwner() {
        return owner;
    }
}
