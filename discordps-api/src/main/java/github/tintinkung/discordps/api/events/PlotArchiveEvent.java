package github.tintinkung.discordps.api.events;

/**
 * Archive event for usage internally.
 */
public class PlotArchiveEvent extends PlotEvent implements ScopedEvent {

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
