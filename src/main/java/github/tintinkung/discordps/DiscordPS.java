package github.tintinkung.discordps;

import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.core.listeners.DiscordSRVListener;
import github.tintinkung.discordps.core.listeners.DiscordSRVLoadedListener;
import github.scarsz.discordsrv.DiscordSRV;

import github.tintinkung.discordps.core.database.DatabaseConnection;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;

public final class DiscordPS extends JavaPlugin {
    private static final String VERSION = "1.4.2";

    public static final String DISCORD_SRV = "DiscordSRV"; // DiscordSRV main class symbol
    public static final int LOADING_TIMEOUT = 5000;

    private static DiscordPS plugin;
    private YamlConfiguration config;
    private DiscordSRVListener discordSrvHook;

    public static DiscordPS getPlugin() { return plugin; }

    public @NotNull YamlConfiguration getConfig() {
        return config;
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

        Plugin discordSRV = getServer().getPluginManager().getPlugin(DISCORD_SRV);

        if (discordSRV != null) {
            DiscordPS.info("DiscordSRV is enabled");
            subscribeToDiscordSRV(discordSRV);
        } else {
            DiscordPS.error("DiscordSRV is not enabled: continuing without discord support");

            DiscordPS.warning("DiscordSRV is not currently enabled (Plot System will not be manage).");

            // Subscribe to DiscordSRV later if it somehow hasn't enabled yet.
            Bukkit.getPluginManager().registerEvents(new DiscordSRVLoadedListener(this), this);

            // Timeout if it takes too long to load
            SchedulerUtil.runTaskLater(this, () -> {
                if (!isDiscordSrvHookEnabled()) {
                    DiscordPS.error(new TimeoutException("DiscordSRV never loaded. timed out."));
                    this.disablePlugin();
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
            return;
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

        if (!DISCORD_SRV.equals(plugin.getName()) || !(plugin instanceof DiscordSRV)) {
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

    public boolean isDiscordSrvHookEnabled() {
        return discordSrvHook != null;
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
        logThrowable(throwable, DiscordSRV::error);
    }
    public static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
}
