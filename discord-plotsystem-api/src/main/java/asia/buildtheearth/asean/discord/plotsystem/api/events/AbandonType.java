package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Represents the reason or source of a plot abandonment.
 * <p>Used by {@link PlotAbandonedEvent} to indicate how or why
 * a plot was marked as abandoned.</p>
 *
 * @see PlotAbandonedEvent
 */
public enum AbandonType {

    /**
     * System automatically abandoned the plot due to inactivity.
     */
    INACTIVE,

    /**
     * Plot was manually abandoned by the owner.
     */
    MANUALLY,

    /**
     * Plot was abandoned via an external command (e.g., forced by an admin).
     */
    COMMANDS,

    /**
     * Plot was abandoned by the system with an unspecified or unknown reason.
     */
    SYSTEM
}
