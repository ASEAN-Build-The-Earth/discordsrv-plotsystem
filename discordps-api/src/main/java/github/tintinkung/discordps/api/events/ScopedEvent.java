package github.tintinkung.discordps.api.events;

/**
 * System event that can be scoped with owner ID
 */
public interface ScopedEvent {

    /**
     * The owner of this scoped event
     *
     * @return The scoped owner data as string
     */
    String getOwner();
}
