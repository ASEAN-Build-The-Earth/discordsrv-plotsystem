package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Abandon a plot and close it by {@link AbandonType}.
 */
public final class PlotAbandonedEvent extends PlotClosureEvent {
    private final AbandonType type;

    /**
     * Create a new plot abandon event with specify type
     *
     * @param plotID The plot ID that is being abandoned
     * @param type Enum type as by inactivity or manually
     */
    public PlotAbandonedEvent(int plotID, AbandonType type) {
        super(plotID);
        this.type = type;
    }

    /**
     * Create a new plot abandon event with default type to {@link AbandonType#SYSTEM}
     *
     * @param plotID The plot ID that is being abandoned
     */
    public PlotAbandonedEvent(int plotID) {
        super(plotID);
        this.type = AbandonType.SYSTEM;
    }

    /**
     * Get the abandoned type of this event
     *
     * @return Enum type as by inactivity or manually
     * @see AbandonType
     */
    public AbandonType getType() {
        return this.type;
    }
}
