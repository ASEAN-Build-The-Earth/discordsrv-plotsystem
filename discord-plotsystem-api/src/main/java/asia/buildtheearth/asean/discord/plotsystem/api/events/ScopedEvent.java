package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * System event that can be scoped with owner ID
 *
 * @deprecated API is deprecated for official Plot-System's API module.
 *             DiscordPlotSystemAPI will no longer release starting by 1.3.0.
 */
@Deprecated(since = "1.2.5")
public interface ScopedEvent {

    /**
     * The owner of this scoped event
     *
     * @return The scoped owner data as string
     */
    String getOwner();
}
