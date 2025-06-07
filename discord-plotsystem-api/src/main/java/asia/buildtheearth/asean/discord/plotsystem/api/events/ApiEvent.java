package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Superclass of all DiscordPlotSystem API events.
 * Extended by all events dispatched through the DiscordPlotSystem event system.
 *
 * @see PlotEvent For all plot-related events
 * @see asia.buildtheearth.asean.discord.plotsystem.api.ApiSubscribe For how to listen to events
 */
public abstract class ApiEvent {
    /**
     * Constructs a new {@link ApiEvent}.
     * <p>Intended to be called by subclasses representing
     * specific API events dispatched through the DiscordPlotSystem event system.</p>
     */
    public ApiEvent() { }
}