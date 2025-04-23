package github.tintinkung.discordps;

import github.scarsz.discordsrv.api.events.Event;
import github.scarsz.discordsrv.dependencies.google.common.util.concurrent.ThreadFactoryBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.json.JSONObject;
import github.scarsz.discordsrv.dependencies.kyori.adventure.platform.bukkit.BukkitAudiences;
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType;
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody;
import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.api.DiscordPlotSystemAPI;
import github.tintinkung.discordps.api.events.ApiEvent;
import github.tintinkung.discordps.core.listeners.DiscordSRVListener;
import github.tintinkung.discordps.core.listeners.PluginLoadedListener;
import github.scarsz.discordsrv.DiscordSRV;


import github.tintinkung.discordps.core.database.DatabaseConnection;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import github.tintinkung.discordps.core.system.ForumWebhook;
import github.tintinkung.discordps.core.system.PlotSystemWebhook;
import github.tintinkung.discordps.utils.CoordinatesUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;
import github.scarsz.discordsrv.dependencies.kyori.adventure.audience.Audience;

public final class DiscordPS extends DiscordPlotSystemAPI {
    private static final String VERSION = "1.0.6";
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

    public static final int LOADING_TIMEOUT = 5000;


    private static final Debug debugger = new Debug();

    private YamlConfiguration config;
    private YamlConfiguration webhookConfig;

    private boolean debuggingEnabled = true;

    private DiscordSRVListener discordSrvHook = null;

    private PlotSystemWebhook webhook = null;

    private String shuttingDown = null;

    public @NotNull YamlConfiguration getConfig() {
        return config;
    }

    public @NotNull YamlConfiguration getWebhookConfig() {
        return webhookConfig;
    }

    public boolean isDebuggingEnabled() {
        return this.debuggingEnabled;
    }

    public PlotSystemWebhook getWebhook() {
        return this.webhook;
    }

    public static Debug getDebugger() {
        return debugger;
    }

    public static DiscordPS getPlugin() {
        return (DiscordPS) plugin;
    }


    @Override
    public void onEnable() {
        // Initialize plugin reference
        plugin = this;

        // Create configs
        createConfig();

        Thread initThread = createInitThread();
        initThread.start();

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

                if(isShuttingDown()) {
                    DiscordPS.warning("==============================================================");
                    DiscordPS.warning(shuttingDown);
                    DiscordPS.warning(". . . Disabling DiscordPlotSystem V" + VERSION);
                    DiscordPS.warning("==============================================================");
                }
                else {
                    // Proper (or force) shutdown with no error message
                    DiscordPS.info("Disabling DiscordPlotSystem V" + VERSION);
                }

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

        Plugin discordSRV = getServer().getPluginManager().getPlugin(DISCORD_SRV_SYMBOL);
        Plugin plotSystem = getServer().getPluginManager().getPlugin(PLOT_SYSTEM_SYMBOL);

        if(plotSystem != null) {
            DiscordPS.info("Plot-System is loaded");

            if(plotSystem.isEnabled()) subscribeToPlotSystemUtil(plotSystem);
            else {
                Bukkit.getPluginManager().registerEvents(new PluginLoadedListener(this), this);
                DiscordPS.info("Registered listener to wait for Plot-System to load");
            }
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

//        if(plotSystem != null && !plotSystem.isEnabled()) {
//            // Subscribe to DiscordSRV later if it somehow hasn't enabled yet.
//            Bukkit.getPluginManager().registerEvents(new PluginLoadedListener(this), this);
//            DiscordPS.info("Registered listener to wait for Plot-System to load");
//        }

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

        // Load config from resource to the plugin
        this.config = new YamlConfiguration();
        this.webhookConfig = new YamlConfiguration();
        try {
            this.config.load(createConfig);
            this.webhookConfig.load(webhookConfig);

            this.debuggingEnabled = this.config.getBoolean("debugging", true);

        } catch (Exception ex) {
            DiscordPS.error(
                Debug.Error.CONFIG_FILE_FAILED_TO_LOAD,
                "Internal Error occurred when loading config file", ex
            );
        }
    }

    public void subscribeToDiscordSRV(Plugin plugin) {
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

        DiscordSRV.api.subscribe(discordSrvHook = new DiscordSRVListener(this));
        DiscordPS.info("Subscribed to DiscordSRV: Plot System will be manage by its JDA instance.");
    }

    public void subscribeToPlotSystemUtil(Plugin plugin) {
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


    public static RestAction<Optional<DataObject>> initWebhook(String channelID, String name, String avatarURL, boolean allowSecondAttempt) {
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

                if (allowSecondAttempt) return initWebhook(channelID, name, avatarURL, false)
                    .completeAfter(5, TimeUnit.SECONDS);
                request.cancel();

                return Optional.empty();
            }

            return response.optObject();
        });
    }

    public boolean isShuttingDown() {
        return shuttingDown != null;
    }

    /**
     * If and only if
     * <ul>
     *     <li>the main webhook ({@link ForumWebhook}) has been initialized</li>
     *     <li>This plugin has subscribed to {@link DiscordSRV}</li>
     *     <li>This plugin is not shutting down</li>
     *     <li>IF debugging is enabled in the config file, Then the {@link Debug} debugger MUST not detect any error</li>
     * </ul>
     * @return If the plugin is fully ready and functional.
     */
    @Override
    public boolean isReady() {
        return getWebhook() != null
            && isDiscordSrvHookEnabled()
            && !isShuttingDown()
            && (!isDebuggingEnabled() || !getDebugger().hasAnyError());
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

    public void exitSlashCommand(long interactionID) {
        if(!isDiscordSrvHookEnabled()) return;
        if(discordSrvHook.getPluginSlashCommand() == null) return;

        DiscordPS.info("Slash command exited: " + interactionID);
        discordSrvHook
            .getPluginSlashCommand()
            .getInteractions()
            .removeInteraction(interactionID);
    }

    @Override
    public <E extends ApiEvent> E callEvent(E event) {
        if(!isReady() || (isReady() && discordSrvHook.getPlotSystemListener() == null)) return null;

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
