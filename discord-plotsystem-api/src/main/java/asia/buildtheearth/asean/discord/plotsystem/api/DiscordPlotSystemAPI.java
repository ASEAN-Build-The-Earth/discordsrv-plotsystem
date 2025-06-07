package asia.buildtheearth.asean.discord.plotsystem.api;

import asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * Main entry point for interacting with the Discord Plot System API.
 * <p>Use {@link DiscordPlotSystemAPI#getInstance()} to access plot-related events and API operations.</p>
 * <p>Use {@link DiscordPlotSystemAPI#registerProvider(PlotCreateProvider)} register a custom data provider for each plot creation.</p>
 *
 * <p>Registering data provider</p>
 * <blockquote>{@snippet :
 * DiscordPlotSystemAPI.registerProvider(new PlotCreateProvider() {
 *      @Override
 *     public PlotCreateData getData(Object rawData) {
 *         return null;
 *     }
 *     @Override
 *     public PlotCreateData getData(int plotID) {
 *         return null;
 *     }
 * });}</blockquote>
 *
 * <p>Creating a new plot</p>
 * <blockquote>{@snippet :
 * DiscordPlotSystemAPI.getInstance().callEvent(
 *     new asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent(1)
 * );}</blockquote>
 *
 * @see #getInstance()
 * @see #registerProvider(PlotCreateProvider)
 * @see #callEvent(ApiEvent)
 * @see asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent
 */
public abstract class DiscordPlotSystemAPI extends JavaPlugin implements DiscordPlotSystem {

    /**
     * Constructs a new {@link DiscordPlotSystemAPI} instance.
     * <p>Required to extends this class for Bukkit to initialize the plugin.</p>
     *
     * @see JavaPlugin
     * @see DiscordPlotSystem
     */
    public DiscordPlotSystemAPI() { }

    /**
     * The running plugin instance of this {@link JavaPlugin}
     */
    protected static DiscordPlotSystem plugin;

    /**
     * API manager for this plugin
     */
    private static final ApiManager api = new ApiManager();

    /**
     * Data provider for initializing plot creation
     *
     * @see #registerProvider(PlotCreateProvider)
     */
    private static PlotCreateProvider provider;

    /**
     * Get the DiscordPlotSystem instance that supports API call.
     * @return {@link DiscordPlotSystem}, Null if the plugin has never been enabled.
     */
    public static DiscordPlotSystem getInstance() {
        return plugin;
    }

    /**
     * Registers the {@link PlotCreateProvider} used to convert raw input into {@link PlotCreateData}.
     * <p>This must be called before {@link asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent#PlotCreateEvent(int, Object)} is fired,
     * as the event relies on the registered provider to convert raw data into a usable data.
     * </p>
     *
     * @param provider The {@link PlotCreateProvider} implementation to register.
     * @param <T> A type that implements {@link PlotCreateProvider}.
     */
    public static <T extends PlotCreateProvider> void registerProvider(T provider) {
        DiscordPlotSystemAPI.provider = provider;
    }

    /**
     * Retrieves the currently registered {@link PlotCreateProvider}.
     *
     * @return The registered provider instance.
     * @throws IllegalArgumentException If no provider has been registered.
     * @see #registerProvider(PlotCreateProvider)
     */
    public static PlotCreateProvider getDataProvider() throws IllegalArgumentException {
        if(DiscordPlotSystemAPI.provider == null) throw new IllegalArgumentException(
            "DiscordPlotSystem has no data provider registered! Cannot construct PlotCreateData. " +
            "Please call registerProvider(...) before using plot-related events.");
        return provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ApiEvent> E callEvent(E event) {
        return api.callEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unsubscribe(Object listener) {
        return api.unsubscribe(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(Object listener) {
        api.subscribe(listener);
    }

    /**
     * Print {@link Throwable} stacktrace to the given logger implementation.
     *
     * @param throwable The throwable to send.
     * @param logger The logger consumer, invoked on each line of the error stacktrace.
     */
    protected static void logThrowable(Throwable throwable, Consumer<String> logger) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        for (String line : stringWriter.toString().split("\n")) logger.accept(line);
    }

    /**
     * Get the plugin logger and send an info message.
     *
     * @param message The message to send.
     */
    protected static void info(String message) {
        getInstance().getLogger().info(message);
    }

    /**
     * Get the plugin logger and send a warning message.
     *
     * @param message The message to send.
     */
    protected static void warning(String message) {
        getInstance().getLogger().warning(message);
    }

    /**
     * Get the plugin logger and send an error message.
     *
     * @param message The message to send.
     */
    protected static void error(String message) {
        getInstance().getLogger().severe(message);
    }

    /**
     * Get the plugin logger and send an error stacktrace.
     *
     * @param throwable The throwable to send
     * @see #logThrowable(Throwable, Consumer)
     */
    protected static void error(Throwable throwable) {
        logThrowable(throwable, DiscordPlotSystemAPI::error);
    }

    /**
     * Get the plugin logger and send an error stacktrace.
     *
     * @param message The error description message.
     * @param throwable The throwable to send.
     */
    protected static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
}