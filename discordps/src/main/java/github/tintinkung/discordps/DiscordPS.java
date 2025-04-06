package github.tintinkung.discordps;

import github.scarsz.discordsrv.api.events.Event;
import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.core.api.ApiManager;
import github.tintinkung.discordps.core.api.DiscordPlotSystem;
import github.tintinkung.discordps.core.api.DiscordPlotSystemAPI;
import github.tintinkung.discordps.core.listeners.DiscordSRVListener;
import github.tintinkung.discordps.core.listeners.PluginLoadedListener;
import github.scarsz.discordsrv.DiscordSRV;


import github.tintinkung.discordps.core.database.DatabaseConnection;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import github.tintinkung.discordps.core.utils.CoordinatesFormat;
import github.tintinkung.discordps.core.utils.CoordinatesConversion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;

public final class DiscordPS extends DiscordPlotSystemAPI implements DiscordPlotSystem {
    private static final String VERSION = "1.0.3";

    public static final String PS_UTIL = "com.alpsbte.plotsystem.utils.conversion.CoordinateConversion";
    public static final String PS_UTIL_CONVERT_TO_GEO = "convertToGeo";
    public static final String PS_UTIL_FORMAT_NUMERIC = "formatGeoCoordinatesNumeric";

    public static final String PLOT_SYSTEM_SYMBOL = "Plot-System"; // PlotSystem main class symbol
    public static final String DISCORD_SRV_SYMBOL = "DiscordSRV"; // DiscordSRV main class symbol

    public static final int LOADING_TIMEOUT = 5000;

    private YamlConfiguration config;
    private DiscordSRVListener discordSrvHook;

    private CoordinatesConversion coordinatesConversion;
    private CoordinatesFormat coordinatesFormat;

    public @NotNull YamlConfiguration getConfig() {
        return config;
    }

    public static DiscordPS getPlugin() {
        return (DiscordPS) plugin;
    }

    @Override
    public void onEnable() {

        plugin = this;

        // Create configs
        createConfig();

        Thread initThread = getInitThread();
        initThread.start();

        // Register Events
        // getServer().getPluginManager().registerEvents(new EventListener(), this);


        // Plugin startup logic
        Bukkit.getConsoleSender().sendMessage(text("[", NamedTextColor.DARK_GRAY)
                .append(text("Discord Plot System", NamedTextColor.AQUA))
                .append(text("v" + VERSION, NamedTextColor.GOLD))
                .append(text("] Loaded successfully!")).content());
    }

    public void disablePlugin() {
        SchedulerUtil.runTask(
        this,
            () -> Bukkit.getPluginManager().disablePlugin(this)
        );
    }

    private @NotNull Thread getInitThread() {
        Thread initThread = new Thread(this::init, "DiscordPlotSystem - Initialization");
        initThread.setUncaughtExceptionHandler((t, e) -> {
            // make DiscordSRV go red in /plugins
            disablePlugin();
            error(e);
            error("==============================================================");
            error("DiscordPlotSystem failed to load properly: " + e.getMessage());
            error("==============================================================");
        });
        return initThread;
    }

    private void init() {

        Plugin discordSRV = getServer().getPluginManager().getPlugin(DISCORD_SRV_SYMBOL);
        Plugin plotSystem = getServer().getPluginManager().getPlugin(PLOT_SYSTEM_SYMBOL);

        if(plotSystem != null) {
            DiscordPS.info("PlotSystem is loaded (enabled: " + getServer().getPluginManager().isPluginEnabled(plotSystem) + ")");
            subscribeToPlotSystemUtil();
        } else {
            DiscordPS.warning("PlotSystem is not enabled: continuing without coordinate conversion support");
        }

        if (discordSRV != null) {
            DiscordPS.info("DiscordSRV is loaded (enabled: " + getServer().getPluginManager().isPluginEnabled(discordSRV) + ")");
            subscribeToDiscordSRV(discordSRV);
        } else {
            DiscordPS.error("DiscordSRV is not enabled: continuing without discord support");
            DiscordPS.error("DiscordSRV is not currently enabled (Plot System will not be manage).");
        }

        if(discordSRV == null || plotSystem == null) {

            // Subscribe to DiscordSRV later if it somehow hasn't enabled yet.
            Bukkit.getPluginManager().registerEvents(new PluginLoadedListener(this), this);

            // Timeout if it takes too long to load
            SchedulerUtil.runTaskLater(this, () -> {
                if (!isDiscordSrvHookEnabled()) {
                    DiscordPS.error(new TimeoutException("DiscordSRV never loaded. timed out."));
                    this.disablePlugin();
                }
                if(!isPlotSystemHookEnabled()) {
                    DiscordPS.warning("Plot-System never loaded: continuing without coordinate conversion support.");
                }
            }, LOADING_TIMEOUT);
        }

        // Initialize database connection
        try {
            DatabaseConnection.InitializeDatabase();
            DiscordPS.info("Successfully initialized database connection.");
        } catch (Exception ex) {
            DiscordPS.error("Could not initialize database connection.");
            DiscordPS.error(ex.getMessage(), ex);

            this.disablePlugin();
        }
    }

    private void createConfig() {
        File createConfig = new File(getDataFolder(), "config.yml");
        if (!createConfig.exists()) {
            createConfig.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        config = new YamlConfiguration();
        try {
            config.load(createConfig);
        } catch (Exception e) {
            DiscordPS.error("An error occurred!", e);
        }
    }

    public void subscribeToDiscordSRV(Plugin plugin) {
        DiscordPS.info("subscribing to DiscordSRV: " + plugin);

        if (!DISCORD_SRV_SYMBOL.equals(plugin.getName()) || !(plugin instanceof DiscordSRV)) {
            DiscordPS.error(new IllegalArgumentException("Not DiscordSRV: " + plugin));
            return;
        }

        if (isDiscordSrvHookEnabled()) {
            DiscordPS.error(new IllegalStateException(
                "Already subscribed to DiscordSRV. Did the server reload? ... If so, don't do that!"
            ));
            return;
        }

        DiscordSRV.api.subscribe(discordSrvHook = new DiscordSRVListener(this));
        DiscordPS.info("Subscribed to DiscordSRV: Plot System will be manage by its JDA instance.");
    }

    public void subscribeToPlotSystemUtil() {
        try {
            Class<?> conversionUtil = Class.forName(PS_UTIL);
            Method convertToGeo = conversionUtil.getMethod(PS_UTIL_CONVERT_TO_GEO, double.class, double.class);
            Method formatGeoCoordinatesNumeric = conversionUtil.getMethod(PS_UTIL_FORMAT_NUMERIC, double[].class);

            coordinatesConversion = (xCords, yCords) -> {
                try {
                    return (double[]) convertToGeo.invoke(null, xCords, yCords);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Access error on method: " + formatGeoCoordinatesNumeric.getName(), ex);
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause(); // Get original exception
                    throw new RuntimeException("Method call failed due to: " + cause.getMessage(), cause);
                }
            };

            coordinatesFormat = (coordinates) -> {
                try {
                    return (String) formatGeoCoordinatesNumeric.invoke(null, (Object) coordinates);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("Access error on method: " + formatGeoCoordinatesNumeric.getName(), ex);
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause(); // Get original exception
                    throw new RuntimeException("Method call failed due to: " + cause.getMessage(), cause);
                }
            };

            DiscordPS.info("Successfully validated Plot-System symbol reference.");
        }
        catch (ClassNotFoundException | NoSuchMethodException ex) {
            DiscordPS.error("Failed to get Plot-System class reference, coordinates conversion will be disabled", ex);
        }
    }

    public String formatGeoCoordinatesNumeric(double[] coordinates) throws RuntimeException {
        if(coordinatesFormat == null) throw new RuntimeException("Not Subscribed to PlotSystem");
        return this.coordinatesFormat.formatGeoCoordinatesNumeric(coordinates);
    }

    public double[] convertToGeo(double xCords, double yCords) throws RuntimeException {
        if(coordinatesFormat == null) throw new RuntimeException("Not Subscribed to PlotSystem");
        return this.coordinatesConversion.convertToGeo(xCords, yCords);
    }

    @Override
    public boolean isDiscordSrvHookEnabled() {
        return discordSrvHook != null;
    }

    @Override
    public boolean isPlotSystemHookEnabled() {
        return coordinatesFormat != null && coordinatesConversion != null;
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        return DiscordSRV.api.callEvent(event);
    }

    @Nullable
    public TextChannel getStatusChannelOrNull() {
        return getDiscordChannelOrNull(config.getString(ConfigPaths.PLOT_STATUS));
    }

    @Nullable
    public TextChannel getDiscordChannelOrNull(String channelID) {
        return isDiscordSrvHookEnabled() ? DiscordSRV.getPlugin().getJda().getTextChannelById(channelID) : null;
    }

    @Nullable
    public TextChannel getDiscordChannelOrNull(long channelID) {
        return isDiscordSrvHookEnabled() ? DiscordSRV.getPlugin().getJda().getTextChannelById(channelID) : null;
    }

    // log messages
    public static void logThrowable(Throwable throwable, Consumer<String> logger) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        for (String line : stringWriter.toString().split("\n")) logger.accept(line);
    }

    public static void info(String message) {
        getPlugin().getLogger().info(message);
    }
    public static void warning(String message) {
        getPlugin().getLogger().warning(message);
    }
    public static void error(String message) {
        getPlugin().getLogger().severe(message);
    }
    public static void error(Throwable throwable) {
        logThrowable(throwable, DiscordPS::error);
    }
    public static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
}
