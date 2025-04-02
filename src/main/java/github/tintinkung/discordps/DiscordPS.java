package github.tintinkung.discordps;

import github.scarsz.discordsrv.dependencies.jda.api.entities.ChannelType;
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
import java.util.function.Consumer;

import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;

public final class DiscordPS extends JavaPlugin {
    private static final String VERSION = "1.4.2";

    public static final String DISCORD_SRV = "DiscordSRV"; // DiscordSRV main class symbol

    private static DiscordPS plugin;
    private YamlConfiguration config;
    private DiscordSRVListener discordSrvHook;

    @Override
    public void onEnable() {
        plugin = this;

        // Create configs
        createConfig();

        Plugin discordSrv = getServer().getPluginManager().getPlugin(DISCORD_SRV);

        if (discordSrv != null) {
            DiscordPS.info("DiscordSRV is enabled");
            subscribeToDiscordSrv(discordSrv);
        } else {
            DiscordPS.error("DiscordSRV is not enabled: continuing without discord support");

            getLogger().warning("DiscordSRV is not currently enabled (messages will NOT be sent to Discord).");
            getLogger().warning("Staff chat messages will still work in-game, however.");

            // Subscribe to DiscordSRV later if it somehow hasn't enabled yet.
            getServer().getPluginManager().registerEvents(new DiscordSRVLoadedListener(this), this);

            // getServer().getPluginManager().registerEvents(new EventListener(), this);
        }

        // Initialize database connection
        try {
            DatabaseConnection.InitializeDatabase();
            Bukkit.getConsoleSender().sendMessage("Successfully initialized database connection.");
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("Could not initialize database connection.");
            DiscordPS.error(ex.getMessage(), ex);

            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register Events
        // getServer().getPluginManager().registerEvents(new EventListener(), this);

        // Plugin startup logic
        Bukkit.getConsoleSender().sendMessage(text("[", NamedTextColor.DARK_GRAY)
                .append(text("Discord Plot System", NamedTextColor.AQUA))
                .append(text("v" + VERSION, NamedTextColor.GOLD))
                .append(text("] Loaded successfully!")).content());
    }

    public static DiscordPS getPlugin() { return plugin; }

    public @NotNull YamlConfiguration getConfig() {
        return config;
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

    public void subscribeToDiscordSrv(Plugin plugin) {
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

        DiscordSRV.info("Subscribed to DiscordSRV: messages will be sent to Discord");
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
