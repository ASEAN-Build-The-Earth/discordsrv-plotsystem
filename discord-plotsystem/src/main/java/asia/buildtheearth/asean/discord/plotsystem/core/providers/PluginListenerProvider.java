package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordCommandListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordEventListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PluginListenerProvider extends PluginProvider {

    /**
     * Discord (JDA) related event listener
     */
    private DiscordEventListener eventListener;

    /**
     * Slash Command Provider
     */
    private DiscordCommandListener pluginSlashCommand;

    /**
     * Plot-System event listener
     */
    private PlotSystemListener plotSystemListener;

    /**
     * Constructs a new {@code PluginProvider} with the specified {@link DiscordPS} plugin instance.
     *
     * @param plugin the plugin instance to be provided; must not be {@code null}
     */
    public PluginListenerProvider(DiscordPS plugin) {
        super(plugin);
    }

    /**
     * Is the listener subscribed.
     *
     * @return Whether all the event listener is initialized
     */
    public boolean hasSubscribed() {
        return this.eventListener != null && this.pluginSlashCommand != null;
    }

    public @Nullable PlotSystemListener getPlotSystemListener() {
        return plotSystemListener;
    }

    public @Nullable DiscordEventListener getEventListener() {
        return eventListener;
    }

    public @Nullable DiscordCommandListener getPluginSlashCommand() {
        return pluginSlashCommand;
    }

    /**
     * Initializes a new Discord-related event listener.
     * After calling this method, {@link #getEventListener()} will return a non-null instance.
     *
     * @param listener The DiscordSRV listener provider.
     * @return A newly created {@link DiscordEventListener} instance.
     */
    @Contract("_ -> new")
    public @NotNull DiscordEventListener newEventListener(@NotNull DiscordSRVListener listener) {
        if(getEventListener() != null) throw new IllegalArgumentException("[Internal] Trying to initialize plugin listener that already exist");
        return this.eventListener = new DiscordEventListener(listener);
    }

    /**
     * Initializes a new Discord's slash command event listener.
     * After calling this method, {@link #getPluginSlashCommand()} will return a non-null instance.
     *
     * @return A newly created {@link DiscordCommandListener} instance.
     */
    @Contract("-> new")
    public @NotNull DiscordCommandListener newSlashCommandListener() {
        if(getPluginSlashCommand() != null) throw new IllegalArgumentException("[Internal] Trying to initialize plugin listener that already exist");
        return this.pluginSlashCommand = new DiscordCommandListener(this.plugin);
    }

    /**
     * Initializes a new plot-system event listener.
     * After calling this method, {@link #getPlotSystemListener()} will return a non-null instance.
     *
     * @param webhook The non-null webhook instance the listener will be used to interact with events.
     * @return A newly created {@link PlotSystemListener} instance.
     */
    @Contract("_ -> new")
    public @NotNull PlotSystemListener newPlotSystemListener(@NotNull PlotSystemWebhook webhook) {
        if(getPlotSystemListener() != null) throw new IllegalArgumentException("[Internal] Trying to initialize plugin listener that already exist");
        return this.plotSystemListener = new PlotSystemListener(webhook);
    }
}
