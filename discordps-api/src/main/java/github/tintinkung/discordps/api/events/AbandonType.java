package github.tintinkung.discordps.api.events;

/**
 * Possible abandon type.
 * Currently, either by inactivity or manually
 */
public enum AbandonType {
    /**
     * System automated abandon event by inactivity
     */
    INACTIVE,
    /**
     * User manually abandon the plot
     */
    MANUALLY
}
