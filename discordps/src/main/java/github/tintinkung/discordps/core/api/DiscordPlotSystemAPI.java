package github.tintinkung.discordps.core.api;

import github.tintinkung.discordps.DiscordPS;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;


public abstract class DiscordPlotSystemAPI extends JavaPlugin {

    // log messages
    protected static DiscordPlotSystem plugin;
    public static final ApiManager api = new ApiManager();

    public static DiscordPlotSystem getInstance() {
        return plugin;
    }

    static void logThrowable(Throwable throwable, Consumer<String> logger) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        for (String line : stringWriter.toString().split("\n")) logger.accept(line);
    }

    public static void info(String message) {
        getInstance().getLogger().info(message);
    }
    public static void warning(String message) {
        getInstance().getLogger().warning(message);
    }
    public static void error(String message) {
        getInstance().getLogger().severe(message);
    }
    public static void error(Throwable throwable) {
        logThrowable(throwable, DiscordPS::error);
    }
    public static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
}