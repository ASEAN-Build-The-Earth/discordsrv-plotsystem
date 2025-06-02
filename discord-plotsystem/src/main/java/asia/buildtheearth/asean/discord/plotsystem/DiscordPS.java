package asia.buildtheearth.asean.discord.plotsystem;

import asia.buildtheearth.asean.discord.commands.interactions.InteractionEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.google.common.util.concurrent.ThreadFactoryBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.json.JSONObject;
import github.scarsz.discordsrv.dependencies.kyori.adventure.platform.bukkit.BukkitAudiences;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType;
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody;
import github.scarsz.discordsrv.util.SchedulerUtil;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.database.DatabaseConnection;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.PluginListenerProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.ShowcaseWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LangConfiguration;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LangManager;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PluginMessage;
import asia.buildtheearth.asean.discord.plotsystem.utils.CoordinatesUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;

/**
 * Main class and implementations for this plugin.
 */
public final class DiscordPS extends DiscordPlotSystemAPI {
    private static final String VERSION = "1.1.0";
    private static final String DISCORD_SRV_VERSION = "1.29.0";

    /**
     * Plot-System plugin Util we referenced to use (CoordinateConversion class)
     * considering that there are running Plot-System instance the server this bot is running on.
     * The method we use are convertToGeo else we need to make an api call for it.
     */
    private static final String PS_UTIL = "com.alpsbte.plotsystem.utils.conversion.CoordinateConversion";
    private static final String PS_UTIL_CONVERT_TO_GEO = "convertToGeo";

    public static final String PLOT_SYSTEM_SYMBOL = "Plot-System"; // PlotSystem main class symbol
    public static final String DISCORD_SRV_SYMBOL = "DiscordSRV"; // DiscordSRV main class symbol

    private static final Debug debugger = new Debug();

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

    @Override
    public void onEnable() {
        // Initialize plugin reference
        plugin = this;

        // Create configs
        createConfig();

        // Initialize plugin
        Thread initThread = createInitThread();
        initThread.start();

        // Startup Message
        try(BukkitAudiences bukkit = BukkitAudiences.create(this)) {
            bukkit.console().sendMessage(text("[", NamedTextColor.GREEN)
                    .append(text("Discord Plot System", NamedTextColor.AQUA))
                    .append(text(" v" + VERSION, NamedTextColor.GOLD))
                    .append(text("] Enabling plugin...", NamedTextColor.GREEN)));
        }
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
                    // Clear DiscordSRV event listener
                    if(discordSrvHook.hasSubscribed()) {
                        DiscordSRV
                            .getPlugin()
                            .getJda()
                            .removeEventListener(discordSrvHook.getEventListener());
                    }
                    // Clear slash command
                    if(discordSrvHook.getPluginSlashCommand() != null) {
                        discordSrvHook.getPluginSlashCommand().clearCommands();
                        DiscordSRV.api.removeSlashCommandProvider(discordSrvHook.getPluginSlashCommand());
                    }
                    DiscordSRV.api.unsubscribe(discordSrvHook);
                }

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

    private @NotNull Thread createInitThread() {
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

    private void init() {
        // Initialize database connection
        try {
            if(DatabaseConnection.InitializeDatabase()) {
                DiscordPS.info("Successfully initialized database connection.");
            } else {
                // returned false: handled error
                DiscordPS.error(Debug.Error.DATABASE_NOT_INITIALIZED, "Could not initialize database connection due to a misconfigured config file.");
            }
        }
        catch (Exception ex) { // Exception thrown: Unknown error occurred
            DiscordPS.error(Debug.Error.DATABASE_NOT_INITIALIZED, ex.getMessage(), ex);
        }

        org.bukkit.plugin.Plugin discordSRV = getServer().getPluginManager().getPlugin(DISCORD_SRV_SYMBOL);
        org.bukkit.plugin.Plugin plotSystem = getServer().getPluginManager().getPlugin(PLOT_SYSTEM_SYMBOL);

        if(plotSystem != null) {
            DiscordPS.info("Plot-System is loaded");

            subscribeToPlotSystemUtil(plotSystem);
        }
        else DiscordPS.warning(Debug.Warning.PLOT_SYSTEM_NOT_DETECTED);


        if (discordSRV != null) {
            DiscordPS.info("DiscordSRV is loaded");
            subscribeToDiscordSRV(discordSRV);
        }
        else { // Fatal error if DiscordSRV does not exist
            this.disablePlugin(Debug.Error.DISCORD_SRV_NOT_DETECTED.getDefaultMessage());
            return;
        }

        // If DiscordSRV JDA is ready before this plugin finish initializing
        if(DiscordSRV.isReady && !discordSrvHook.hasSubscribed()) {
            DiscordPS.info("JDA Has started, subscribing to its instance");
            discordSrvHook.subscribeAndValidateJDA();
        }
    }

    private void createConfig() {
        File createConfig = new File(getDataFolder(), "config.yml");
        if (!createConfig.exists()) {
            createConfig.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        File webhookConfig = new File(getDataFolder(), "webhook.yml");
        if (!webhookConfig.exists()) {
            saveResource("webhook.yml", false);
        }

        File showcaseConfig = new File(getDataFolder(), "showcase.yml");
        if (!showcaseConfig.exists()) {
            saveResource("showcase.yml", false);
        }

        // Load config from resource to the plugin
        this.config = new YamlConfiguration();
        this.webhookConfig = new YamlConfiguration();
        this.showcaseConfig = new YamlConfiguration();
        this.langConfig = new LangConfiguration(this);
        try {
            this.langConfig.initLanguageFiles();
            this.config.load(createConfig);
            this.webhookConfig.load(webhookConfig);
            this.showcaseConfig.load(showcaseConfig);

            this.debuggingEnabled = this.config.getBoolean("debugging", true);

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
                + ". Error may occur if the API is different from expected version."
            );
        }

        DiscordSRV.api.subscribe(discordSrvHook = new DiscordSRVListener(this));
        DiscordPS.info("Subscribed to DiscordSRV: Plot System will be manage by its JDA instance.");
    }

    /**
     * Subscribe this plugin to Plot-System instance statically.
     * Currently using the {@link CoordinatesUtil coordinates conversion} implementation.
     *
     * <pre>com.alpsbte.plotsystem.utils.conversion.CoordinateConversion.convertToGeo</pre>
     *
     * @param plugin The Plot-System plugin instance
     */
    public void subscribeToPlotSystemUtil(@NotNull org.bukkit.plugin.Plugin plugin) {
        try {
            // Find class symbol without triggering its static initializer
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            Class<?> conversionUtil = Class.forName(PS_UTIL, false, classLoader);
            Method convertToGeo = conversionUtil.getMethod(PS_UTIL_CONVERT_TO_GEO, double.class, double.class);

            CoordinatesUtil.initCoordinatesFunction((xCords, yCords) -> {
                try {
                    if(!plugin.isEnabled()) throw new RuntimeException("Plot-System has not been enabled to use this method");
                    return (double[]) convertToGeo.invoke(null, xCords, yCords);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Access error on method: " + convertToGeo.getName(), ex);
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException("Method call failed due to: " + ex.getCause().getMessage(), ex);
                }
            });

            DiscordPS.info("Successfully validated Plot-System symbol reference.");
        }
        catch (ClassNotFoundException | NoSuchMethodException ex) {
            DiscordPS.warning(Debug.Warning.PLOT_SYSTEM_SYMBOL_NOT_FOUND, ex.getMessage());
        }
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

        return new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, requestBody, (response, request) -> {
            if (response.code == 404) {
                // 404 = Invalid Request (most likely bad permissions)
                DiscordPS.error(
                    "Webhook POST returned 404"
                    + (allowSecondAttempt? " ... retrying in 5 seconds" : "")
                );

                if (allowSecondAttempt) return createWebhook(channelID, name, avatarURL, false)
                    .completeAfter(5, TimeUnit.SECONDS);
                request.cancel();

                return Optional.empty();
            }

            return response.optObject();
        });
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

    public void initWebhook(PlotSystemWebhook webhook) {
        if(this.webhook != null) {
            DiscordPS.error("[Internal] Trying to re-assign plugin's PlotSystemWebhook reference, ignoring.");
            return;
        }
        this.webhook = webhook;
    }

    public void initShowcase(ShowcaseWebhook showcase) {
        if(this.showcase != null) {
            DiscordPS.error("[Internal] Trying to re-assign plugin's ShowcaseWebhook reference, ignoring.");
            return;
        }
        this.showcase = showcase;
    }

    public void exitSlashCommand(long interactionID) {
        if(!isDiscordSrvHookEnabled()) return;
        if(discordSrvHook.getPluginSlashCommand() == null) return;

        discordSrvHook.getPluginSlashCommand().removeInteraction(interactionID);
    }

    @Override
    public <E extends ApiEvent> @Nullable E callEvent(E event) {
        if(!isReady() || (isReady() && discordSrvHook.getPlotSystemListener() == null))
            return null;

        return super.callEvent(event);
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
