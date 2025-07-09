package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordCommandListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordEventListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhookValidator;
import github.scarsz.discordsrv.dependencies.kyori.adventure.platform.bukkit.BukkitAudiences;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextComponent;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.REPOSITORY_RELEASE_URL;
import static asia.buildtheearth.asean.discord.plotsystem.Debug.Warning.UPDATE_CHECKING_FAILED;
import static asia.buildtheearth.asean.discord.plotsystem.DiscordPS.VERSION;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PluginMessage.*;
import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;

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
    protected @NotNull DiscordEventListener newEventListener(@NotNull DiscordSRVListener listener) {
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
    protected @NotNull DiscordCommandListener newSlashCommandListener() {
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
    protected @NotNull PlotSystemListener newPlotSystemListener(@NotNull PlotSystemWebhook webhook) {
        if(getPlotSystemListener() != null) throw new IllegalArgumentException("[Internal] Trying to initialize plugin listener that already exist");
        return this.plotSystemListener = new PlotSystemListener(webhook);
    }

    /**
     * Create a new {@linkplain PlotSystemWebhookValidator}.
     *
     * @return A newly created validator using the plugin's webhook instance.
     */
    @Contract("-> new")
    protected @NotNull PlotSystemWebhookValidator newWebhookValidator() {
        if(this.plugin.getWebhook() == null) throw new IllegalArgumentException("[Internal] Trying to initialize plot-system webhook validator but its instance is null");
        return new PlotSystemWebhookValidator(this.plugin.getWebhook(),  this.plugin.getConfig());
    }

    /**
     * Simple blocking message when the plugin is fully ready.
     *
     * <p>Supplied by update checker action</p>
     *
     * @param availableUpdate Available update if the plugin is not up-to-date
     * @param failure If a failure occurred trying to fetch for any updates
     */
    protected void notifyPluginReady(@Nullable String availableUpdate, @Nullable Throwable failure) {
        try(BukkitAudiences bukkit = BukkitAudiences.create(this.plugin)) {
            Component readyMessage = Component.empty()
                    .append(text("[").color(NamedTextColor.GREEN))
                    .append(text("Discord Plot-System ", NamedTextColor.AQUA))
                    .append(text("v" + VERSION, NamedTextColor.GOLD))
                    .append(text("]", NamedTextColor.GREEN)).appendNewline()
                    .append(text("------------- Plugin is ready! -------------", NamedTextColor.GREEN));
            if(failure != null) {
                readyMessage = readyMessage
                        .appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                        .append(text("Updates: ", NamedTextColor.GOLD))
                        .append(text("Update check failed", NamedTextColor.RED))
                        .appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                        .append(text("Look it up at: ", NamedTextColor.DARK_AQUA))
                        .append(text(REPOSITORY_RELEASE_URL, NamedTextColor.BLUE));
                if(!DiscordPS.getDebugger().hasWarning(UPDATE_CHECKING_FAILED)) {
                    String error = "Unknown exception has thrown trying to check for update";
                    DiscordPS.error(error); DiscordPS.warning(UPDATE_CHECKING_FAILED, error);
                }
                Notification.notify(PLUGIN_UPDATE_EXCEPTION);
            }
            else if(availableUpdate != null) {
                readyMessage = readyMessage.appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                        .append(text("Updates: ", NamedTextColor.GOLD))
                        .append(text("Update is available!", NamedTextColor.YELLOW))
                        .appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                        .append(text("New version: ", NamedTextColor.GREEN))
                        .append(text(availableUpdate, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                        .append(text("Look it up at: ", NamedTextColor.GREEN))
                        .append(text(REPOSITORY_RELEASE_URL, NamedTextColor.BLUE));
                Notification.notify(PLUGIN_UPDATE_AVAILABLE);
            }
            else {
                readyMessage = readyMessage.appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                        .append(text("Updates: ", NamedTextColor.GOLD))
                        .append(text("Plugin is up-to-date!", NamedTextColor.GREEN, TextDecoration.BOLD));
                Notification.sendMessage(DiscordPS.getSystemLang().getEmbed(PLUGIN_UPDATE_UP_TO_DATE).title());
            }

            readyMessage = readyMessage.appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                    .append(text("Maintainer: ", NamedTextColor.GRAY))
                    .append(text("@tintinkung from ASEAN BTE", NamedTextColor.LIGHT_PURPLE))
                    .appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                    .append(text("Github: ", NamedTextColor.GRAY))
                    .append(text("https://github.com/ASEAN-Build-The-Earth", NamedTextColor.AQUA))
                    .appendNewline().append(text("> ", NamedTextColor.DARK_GRAY))
                    .append(text("Need help? Use /setup help", NamedTextColor.GRAY))
                    .appendNewline().append(text("--------------------------------------------", NamedTextColor.GREEN));

            bukkit.console().sendMessage(readyMessage);
        }
    }

    /**
     * Huge debugging message when the plugin is not ready.
     *
     * @return Formatted message with colors.
     */
    protected @NotNull Component createDebuggingMessage() {
        TextComponent LINE = text("!!! ", NamedTextColor.RED);

        // Brief title
        Component notReadyMessage = text("[DiscordPlotSystem]", NamedTextColor.AQUA).appendSpace()
                .append(text("is NOT ready!", NamedTextColor.RED, TextDecoration.BOLD)).appendNewline()
                .append(text("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", NamedTextColor.RED)).appendNewline()
                .append(LINE).append(text("There are unresolved configuration or runtime errors blocking the application process.", NamedTextColor.YELLOW)).appendNewline()
                .append(LINE).append(text("The plugin will remain running for debugging.", NamedTextColor.GREEN)).appendNewline()
                .append(LINE).append(text("Use the command '/setup help' in your discord server to see the configuration checklists.", NamedTextColor.GREEN)).appendNewline()
                .append(LINE).append(text("All Occurred Errors:", NamedTextColor.YELLOW)).appendNewline();

        // Important error: this make the application fails
        for(Map.Entry<Debug.Error, String> entry : DiscordPS.getDebugger().allThrownErrors()) {
            notReadyMessage = notReadyMessage.append(LINE)
                    .append(text("[ERROR]", NamedTextColor.RED, TextDecoration.BOLD))
                    .appendSpace()
                    .append(text(entry.getKey().name(), NamedTextColor.GOLD))
                    .append(text(":", NamedTextColor.GRAY))
                    .appendNewline()
                    .append(LINE).append(text("        └─ ", NamedTextColor.DARK_GRAY))
                    .append(text(entry.getValue(), NamedTextColor.DARK_RED))
                    .appendNewline();
        }

        // Occurred warnings
        for(Map.Entry<Debug.Warning, String> entry : DiscordPS.getDebugger().allThrownWarnings()) {
            notReadyMessage = notReadyMessage.append(LINE)
                    .append(text("[WARNING]", NamedTextColor.YELLOW))
                    .appendSpace()
                    .append(text(entry.getKey().name(), NamedTextColor.GOLD))
                    .append(text(":", NamedTextColor.GRAY))
                    .appendNewline()
                    .append(LINE).append(text("          └─ ", NamedTextColor.DARK_GRAY))
                    .append(text(entry.getValue(), NamedTextColor.DARK_GREEN))
                    .appendNewline();
        }
        notReadyMessage = notReadyMessage.append(text("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", NamedTextColor.RED));
        return notReadyMessage;
    }
}
