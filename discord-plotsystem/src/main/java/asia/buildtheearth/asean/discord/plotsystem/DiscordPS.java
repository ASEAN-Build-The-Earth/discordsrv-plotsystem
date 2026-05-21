package asia.buildtheearth.asean.discord.plotsystem;

import asia.buildtheearth.asean.discord.DiscordSRVBridge;
import asia.buildtheearth.asean.discord.plotsystem.core.system.ForumWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.*;
import github.scarsz.discordsrv.dependencies.google.common.util.concurrent.ThreadFactoryBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.json.JSONObject;
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType;
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody;
import github.scarsz.discordsrv.util.SchedulerUtil;
import github.scarsz.discordsrv.DiscordSRV;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent;
import asia.buildtheearth.asean.discord.commands.interactions.InteractionEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.database.DatabaseConnection;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.PluginListenerProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.ShowcaseWebhook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PluginMessage;

/**
 * Main entry point and implementation class for the Discord Plot-System plugin.
 *
 * <p><strong>Note:</strong> This class is the core implementation of the API and should generally not be used directly.
 * Use {@link DiscordPlotSystemAPI} instead to interact with the plugin externally.</p>
 *
 * <p><strong>Package Overview:</strong></p>
 *
 * <p><strong>{@code asia.buildtheearth.asean.discord.plotsystem.core}</strong><br/> Core logic and system internals</p>
 * <ul><li>{@code system} — Handles internal plugin mechanics,
 *     including Plot-System webhook and event coordination</li>
 *     <li>{@code providers} — Abstract base providers used across the plugin
 *     (e.g. for data creation and external integration)</li>
 *     <li>{@code listeners} — Contains all internal event listeners:
 *         <ul><li>{@link DiscordSRVListener DiscordSRVListener}
 *             — Main startup hook; listens for JDA readiness to initialize the plugin</li>
 *             <li>{@link asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener PlotSystemListener}
 *             — Listens for plot-related API events via {@link DiscordPlotSystemAPI}</li>
 *             <li>{@link asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordCommandListener DiscordCommandListener}
 *             — Handles Discord slash command executions</li>
 *          </ul>
 *     </li>
 *     <li>{@code database} — Manages database operations:
 *     <ul><li>{@link asia.buildtheearth.asean.discord.plotsystem.core.database.DatabaseConnection DatabaseConnection}
 *         — The main class that initialize database connection</li>
 *         <li>{@link asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus ThreadStatus}
 *         — Stores the current status of each plot</li>
 *         <li>{@link asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry WebhookEntry}
 *         — Represents a full database record per plot</li>
 *      </ul>
 * </li></ul>
 *
 * <p><strong>{@code asia.buildtheearth.asean.discord.plotsystem.commands}</strong><br/> Registered commands exposed to Discord</p>
 * <ul>
 *     <li>{@link asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand PlotCommand}
 *     — User-facing plot control commands</li>
 *     <li>{@link asia.buildtheearth.asean.discord.plotsystem.commands.ReviewCommand ReviewCommand}
 *     — Commands for plot reviewing and moderation</li>
 *     <li>{@link asia.buildtheearth.asean.discord.plotsystem.commands.SetupCommand SetupCommand}
 *     — Commands to initialize and configure the plugin</li>
 * </ul>
 *
 * <p><strong>{@code asia.buildtheearth.asean.discord.plotsystem.utils}</strong><br/>
 * General-purpose utilities that support the rest of the system, not critical to core operations</p>
 *
 * @see DiscordPlotSystemAPI
 */
public class DiscordPS extends DiscordPlotSystemAPI implements DiscordSRVBridge {
    public static final String VERSION = "1.2.3";
    public static final String DISCORD_SRV_VERSION = "1.29.0";

    public static final String DISCORD_SRV_SYMBOL = "DiscordSRV"; // DiscordSRV main class symbol

    private static final Debug debugger = new Debug();

    private static final LangMetadata metadata = new LangMetadata();

    private YamlConfiguration config;
    private YamlConfiguration webhookConfig;
    private YamlConfiguration showcaseConfig;
    private LangConfiguration langConfig;

    private boolean debuggingEnabled = true;

    private DiscordSRVListener discordSrvHook = null;

    private PlotSystemWebhook webhook = null;

    private ShowcaseWebhook showcase = null;

    private String shuttingDown = null;

    public @NotNull YamlConfiguration getConfig() {
        return config;
    }

    public @NotNull YamlConfiguration getWebhookConfig() {
        return webhookConfig;
    }

    public @NotNull YamlConfiguration getShowcaseConfig() {
        return showcaseConfig;
    }

    public boolean isDebuggingEnabled() {
        return this.debuggingEnabled;
    }

    public PlotSystemWebhook getWebhook() {
        return this.webhook;
    }

    public @Nullable ShowcaseWebhook getShowcase() {
        return this.showcase;
    }

    public static Debug getDebugger() {
        return debugger;
    }

    public static DiscordPS getPlugin() {
        return (DiscordPS) plugin;
    }

    public static @NotNull LangManager<SystemLang> getSystemLang() {
        return getPlugin().langConfig.getSystemLang();
    }

    public static @NotNull LangManager<MessageLang> getMessagesLang() {
        return getPlugin().langConfig.getMessagesLang();
    }

    public static LangMetadata getMetadata() {
       return metadata;
    }

    @Override
    public void onEnable() {
        // Initialize plugin reference
        plugin = this;

        // Create configs
        createConfig();

        // Initialize plugin
        Thread initThread = createInitThread();
        initThread.start();
    }

    @Override
    public void onDisable() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordPlotSystem - Shutdown").build();
        try(final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory)) {
            executor.invokeAll(Collections.singletonList(() -> {
                if(isShuttingDown()) {
                    DiscordPS.warning("==============================================================");
                    DiscordPS.warning(shuttingDown);
                    DiscordPS.warning(". . . Disabling DiscordPlotSystem V" + VERSION);
                    DiscordPS.warning("==============================================================");
                    Notification.notify(PluginMessage.PLUGIN_STOPPING_ON_ERROR, shuttingDown);
                }
                else {
                    // Proper (or force) shutdown with no error message
                    DiscordPS.info("Disabling DiscordPlotSystem V" + VERSION);
                    Notification.notify(PluginMessage.PLUGIN_STOPPING_GRACEFUL);
                }

                // Unsubscribe to DiscordSRV
                if(isDiscordSrvHookEnabled()) {
                    try {
                        // Clear DiscordSRV event listener
                        if(discordSrvHook.hasSubscribed()) {
                            this.getJDA().removeEventListener(discordSrvHook.getEventListener());
                        }
                        // Clear slash command
                        if(discordSrvHook.getPluginSlashCommand() != null) {
                            discordSrvHook.getPluginSlashCommand().clearCommands();
                            this.removeSlashCommandProvider(discordSrvHook.getPluginSlashCommand());
                        }
                        this.unsubscribe(discordSrvHook.getPlotSystemListener());
                        this.unsubscribeSRV(discordSrvHook);
                    }
                    catch (RuntimeException ex) {
                        DiscordPS.error("Exception occurred trying to unsubscribe from DiscordSRV", ex);
                    }
                }

                // Close all database connections
                DatabaseConnection.shutdown();

                // shutdown scheduler tasks
                SchedulerUtil.cancelTasks(this);

                // unregister event listeners because of garbage reloading plugins
                HandlerList.unregisterAll(this);

                return null;
            }), 15, TimeUnit.SECONDS);

            executor.shutdownNow();
        } catch (InterruptedException | NullPointerException ex) {
            error(ex);
            DiscordPS.warning("==============================================================");
            DiscordPS.warning("Failed to shutdown DiscordPlotSystem properly");
            DiscordPS.warning(". . . Disabling DiscordPlotSystem V" + VERSION);
            DiscordPS.warning("==============================================================");
        }
        super.onDisable();
    }

    /**
     * Forcefully disable this plugin
     *
     * @param shutdownMessage The shutdown reason to log to console
     */
    public void disablePlugin(@NotNull String shutdownMessage) {
        this.shuttingDown = shutdownMessage;
        SchedulerUtil.runTask(
        this,
            () -> Bukkit.getPluginManager().disablePlugin(this)
        );
    }

    protected @NotNull Thread createInitThread() {
        Thread initThread = new Thread(this::init, "DiscordPlotSystem - Initialization");
        initThread.setUncaughtExceptionHandler((t, e) -> {
            DiscordPS.error("[DiscordPlotSystem - Initialization] ERROR: Uncaught exception");
            DiscordPS.error("[DiscordPlotSystem - Initialization] ERROR: " + e, e);
            for(StackTraceElement ex : e.getStackTrace()) {
                DiscordPS.error(ex.toString());
            }

            disablePlugin("DiscordPlotSystem failed to load properly: " + e);
        });
        return initThread;
    }

    protected void init() {
        // Initialize database connection
        try {
            if(DatabaseConnection.InitializeDatabase()) {
                DiscordPS.info("Successfully initialized database connection.");
                debugger.resolveError(Debug.Error.DATABASE_NOT_INITIALIZED);
            } else {
                // returned false: handled error
                DiscordPS.error(Debug.Error.DATABASE_NOT_INITIALIZED, "Could not initialize database connection due to a misconfigured config file.");
            }
        }
        catch (Exception ex) { // Exception thrown: Unknown error occurred
            DiscordPS.error(Debug.Error.DATABASE_NOT_INITIALIZED, ex.getMessage(), ex);
        }

        org.bukkit.plugin.Plugin discordSRV = getServer().getPluginManager().getPlugin(DISCORD_SRV_SYMBOL);

        if (discordSRV != null) {
            DiscordPS.info("DiscordSRV is loaded");
            subscribeToDiscordSRV(discordSRV);
        }
        else { // Fatal error if DiscordSRV does not exist
            this.disablePlugin(Debug.Error.DISCORD_SRV_NOT_DETECTED.getDefaultMessage());
            return;
        }

        // If DiscordSRV JDA is ready before this plugin finish initializing
        if(this.discordIsReady() && !discordSrvHook.hasSubscribed()) {
            DiscordPS.info("JDA Has started, subscribing to its instance");
            discordSrvHook.subscribeAndValidateJDA();
        }
    }

    protected void createConfig() {
        this.config = new YamlConfiguration();
        this.webhookConfig = new YamlConfiguration();
        this.showcaseConfig = new YamlConfiguration();
        this.langConfig = new LangConfiguration(this);

        // Load config from resource to the plugin
        loadConfig(true);
    }

    protected File prepareConfigFile(String location, boolean create) throws IOException {
        File file = new File(plugin.getDataFolder(), location);

        if (!file.exists()) {
            if(!create)
                throw new IOException("Language file at '" + location + "' does not exist to load.");

            saveResource(location, false);
        }

        return file;
    }

    public void loadConfig(boolean create) {
        try {
            File pluginConfig = prepareConfigFile("config.yml", create);
            File webhookConfig = prepareConfigFile("webhook.yml", create);
            File showcaseConfig = prepareConfigFile("showcase.yml", create);

            this.langConfig.loadLanguageFiles(create);
            this.config.load(pluginConfig);
            this.webhookConfig.load(webhookConfig);
            this.showcaseConfig.load(showcaseConfig);

            this.debuggingEnabled = this.config.getBoolean("debugging", true);

            DiscordPS.getDebugger().resolveError(Debug.Error.CONFIG_FILE_FAILED_TO_LOAD);
        } catch (Exception ex) {
            DiscordPS.error(
                Debug.Error.CONFIG_FILE_FAILED_TO_LOAD,
                "Internal Error occurred when loading config file", ex
            );
        }
    }

    /**
     * Subscribe to DiscordSRV instance.
     *
     * @param plugin The DiscordSRV plugin instance
     * @see github.scarsz.discordsrv.api.ApiManager#subscribe(Object)
     */
    public void subscribeToDiscordSRV(@NotNull org.bukkit.plugin.Plugin plugin) {
        DiscordPS.info("subscribing to DiscordSRV: " + plugin);

        if (!DISCORD_SRV_SYMBOL.equals(plugin.getName()) || !(plugin instanceof DiscordSRV)) {
            DiscordPS.error(
                Debug.Error.DISCORD_SRV_FAILED_TO_SUBSCRIBE,
                "Expected DiscordSRV class symbol is not DiscordSRV: " + plugin
            );
            return;
        }

        if (isDiscordSrvHookEnabled()) {
            DiscordPS.error(
                Debug.Error.DISCORD_SRV_FAILED_TO_SUBSCRIBE,
                "Already subscribed to DiscordSRV. Did the server reload?"
            );
            return;
        }

        if(!plugin.getDescription().getVersion().equalsIgnoreCase(DISCORD_SRV_VERSION)) {
            DiscordPS.warning(Debug.Warning.DISCORD_SRV_VERSION_NOT_MATCHED,
                "Detected DiscordSRV version unmatched the plugin's API version. "
                + "Expected: " + DISCORD_SRV_VERSION + ", Got: " + plugin.getDescription().getVersion()
                + ". Error may occur if the API contain different implementations from expected version."
            );
        }

        this.subscribeSRV(this.getListenerHook());
        DiscordPS.info("Subscribed to DiscordSRV: Plot System will be manage by its JDA instance.");
    }

    /**
     * Create a guild base discord-webhook to be managed by this plugin.
     *
     * @param channelID The channelID which the webhook will be linked with
     * @param name The webhook display name
     * @param avatarURL The webhook avatar image, accepting as data URI string
     * @param allowSecondAttempt Retry on 404 error
     * @return The rest action which return the raw data object on complete
     */
    @Contract("_, _, _, _ -> new")
    @NotNull
    public static RestAction<Optional<DataObject>> createWebhook(String channelID,
                                                                 String name,
                                                                 String avatarURL,
                                                                 boolean allowSecondAttempt) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("avatar", avatarURL);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json"), jsonObject.toString());
        Route.CompiledRoute route = Route.Channels.CREATE_WEBHOOK.compile(channelID);
        ForumWebhook.ObjectResponse<DataObject> response = new ForumWebhook.ObjectResponse<>(Function.identity());

        if(allowSecondAttempt) response.setRetryExecution(() -> createWebhook(channelID, name, avatarURL, false));

        return new RestActionImpl<>(DiscordPS.getPlugin().getJDA(), route, requestBody, response::execute);
    }

    /**
     * Is the server shutting down.
     * The result only updates when {@link #disablePlugin(String)} is called
     *
     * @return Whether this plugin is shutting down
     */
    public boolean isShuttingDown() {
        return this.shuttingDown != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        return getWebhook() != null
            && isDiscordSrvHookEnabled()
            && !isShuttingDown()
            && (!isDebuggingEnabled() || !getDebugger().hasAnyError());
    }

    /**
     * Get the plugin interaction provider,
     * this provides manual handle for triggering registered commands.
     *
     * @return The interaction provider as an optional
     * @see InteractionEvent#fromClass(Class) Get registered command
     */
    public Optional<InteractionEvent> getOptInteractionProvider() {
        return Optional.ofNullable(this.discordSrvHook).map(PluginListenerProvider::getPluginSlashCommand);
    }

    public boolean isDiscordSrvHookEnabled() {
        return discordSrvHook != null;
    }

    public @NotNull DiscordSRVListener initListenerHook() {
        if(isDiscordSrvHookEnabled())
            throw new IllegalArgumentException("[Internal] Trying to re-assign plugin's DiscordSRVListener reference, ignoring.");
        return new DiscordSRVListener(this);
    }

    public void assignWebhook(PlotSystemWebhook webhook) {
        this.webhook = webhook;
    }

    public void assignShowcase(ShowcaseWebhook showcase) {
        this.showcase = showcase;
    }

    public void exitSlashCommand(long interactionID) {
        if(!isDiscordSrvHookEnabled()) return;
        if(discordSrvHook.getPluginSlashCommand() == null) return;

        discordSrvHook.getPluginSlashCommand().removeInteraction(interactionID);
    }

    /**
     * Get the main listener of the plugin as {@link DiscordSRVListener}
     *
     * @return The listener instance if subscribed
     */
    public DiscordSRVListener getListenerHook() {
        if(!isDiscordSrvHookEnabled()) return this.discordSrvHook = initListenerHook();
        return this.discordSrvHook;
    }

    @Override
    public <E extends ApiEvent> @Nullable E callEvent(E event) {
        if(!isReady() || (isReady() && discordSrvHook.getPlotSystemListener() == null))
            return null;

        return super.callEvent(event);
    }

    /**
     * Safely retrieve player's linked account
     * and return null if no linked account exist or an error occurred.
     */
    @Override
    public @Nullable Member getAsDiscordMember(@NotNull OfflinePlayer player) {
        try { return DiscordSRVBridge.super.getAsDiscordMember(player); }
        catch (IllegalArgumentException ex) {
            DiscordPS.error("An exception occurred getting linked account for the player name: " + player.getName());
            return null;
        }
    }

    // Debugging messages
    public static void debug(String message) {
        if(getPlugin().isDebuggingEnabled()) DiscordPlotSystemAPI.info(message);
    }
    public static void info(String message) {
        DiscordPlotSystemAPI.info(message);
    }
    public static void warning(@NotNull Debug.Warning signature) {
        DiscordPlotSystemAPI.warning(signature.getDefaultMessage());
        getDebugger().putWarning(signature);
    }
    public static void warning(@NotNull Debug.Warning signature, String message) {
        DiscordPlotSystemAPI.warning(message);
        getDebugger().putWarning(signature, message);
    }
    public static void warning(String message) {
        DiscordPlotSystemAPI.warning(message);
    }
    public static void error(@NotNull Debug.Error signature) {
        DiscordPlotSystemAPI.error(signature.getDefaultMessage());
        getDebugger().putError(signature);
    }
    public static void error(@NotNull Debug.Error signature, Throwable throwable) {
        error(signature.getDefaultMessage(), throwable);
        getDebugger().putError(signature);
    }
    public static void error(@NotNull Debug.Error signature, String message, Throwable throwable) {
        error(message, throwable);
        getDebugger().putError(signature, message);
    }
    public static void error(@NotNull Debug.Error signature, String message) {
        DiscordPlotSystemAPI.error(message);
        getDebugger().putError(signature, message);
    }
    public static void error(String message) {
        DiscordPlotSystemAPI.error(message);
    }
    public static void error(Throwable throwable) {
        logThrowable(throwable, DiscordPS::error);
    }
    public static void error(String message, Throwable throwable) {
        DiscordPlotSystemAPI.error(message);
        DiscordPlotSystemAPI.error(throwable);
    }
}
