package github.tintinkung.discordps.api;


import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * DiscordPlotSystem super class, acts like a wrapper for API
 * providing static {@link DiscordPlotSystemAPI#getInstance()} instance.
 */
public abstract class DiscordPlotSystemAPI extends JavaPlugin {

    protected static DiscordPlotSystem plugin;

    /**
     * Get the DiscordPlotSystem instance that supports API call.
     * @return {@link DiscordPlotSystem}, Null if the plugin has never been enabled.
     */
    public static DiscordPlotSystem getInstance() {
        return plugin;
    }

    protected static void logThrowable(Throwable throwable, Consumer<String> logger) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        for (String line : stringWriter.toString().split("\n")) logger.accept(line);
    }

    protected static void info(String message) {
        getInstance().getLogger().info(message);
    }

    protected static void warning(String message) {
        getInstance().getLogger().warning(message);
    }

    protected static void error(String message) {
        getInstance().getLogger().severe(message);
    }

    protected static void error(Throwable throwable) {
        logThrowable(throwable, DiscordPlotSystemAPI::error);
    }

    protected static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
}