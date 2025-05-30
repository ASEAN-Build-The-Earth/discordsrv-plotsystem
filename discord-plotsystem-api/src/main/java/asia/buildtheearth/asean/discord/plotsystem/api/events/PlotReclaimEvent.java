package asia.buildtheearth.asean.discord.plotsystem.api.events;

public final class PlotReclaimEvent extends PlotEvent implements ScopedEvent {

    private final String owner;

    public PlotReclaimEvent(int plotID, String owner) {
        super(plotID);

        this.owner = owner;
    }

    @Override
    public String getOwner() {
        return owner;
    }
}
