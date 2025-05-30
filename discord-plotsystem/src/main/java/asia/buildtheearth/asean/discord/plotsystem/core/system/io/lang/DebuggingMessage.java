package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Debugging messages for Discord-PlotSystem Debug system.
 * Note that enum value is 1:1 associated with debugger's error groups
 *
 * @see asia.buildtheearth.asean.discord.plotsystem.Debug.ErrorGroup
 */
public enum DebuggingMessage {
    PLUGIN_VALIDATION("debugging.plugin-validation."),
    DATABASE_CONNECTION("debugging.database-connection."),
    CONFIG_VALIDATION("debugging.config-validation."),
    WEBHOOK_REFS_VALIDATION("debugging.webhook-refs-validation."),
    WEBHOOK_CHANNEL_VALIDATION("debugging.webhook-channel-validation.");

    private final String path;
    private static final String FAILED = "failed";
    private static final String PASSED = "passed";

    DebuggingMessage(String path) {
        this.path = path;
    }

    @Contract(pure = true)
    public @NotNull SystemLang getPassed() {
        return () -> this.path + PASSED;
    }

    @Contract(pure = true)
    public @NotNull SystemLang getFailed() {
        return () -> this.path + FAILED;
    }
}