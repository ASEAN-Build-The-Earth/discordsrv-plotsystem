package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Superclass of all DiscordPlotSystem API events.
 * Extended by all events dispatched through the DiscordPlotSystem event system.
 *
 * @deprecated API is deprecated for official Plot-System's API module.
 *             DiscordPlotSystemAPI will no longer release starting by 1.3.0.
 */
@Deprecated(since = "1.2.5")
public abstract class ApiEvent {
    /**
     * Constructs a new {@link ApiEvent}.
     * <p>Intended to be called by subclasses representing
     * specific API events dispatched through the DiscordPlotSystem event system.</p>
     */
    public ApiEvent() { }
}