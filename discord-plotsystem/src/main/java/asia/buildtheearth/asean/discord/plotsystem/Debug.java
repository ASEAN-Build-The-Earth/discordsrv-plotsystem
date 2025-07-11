package asia.buildtheearth.asean.discord.plotsystem;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;


/**
 * Static debugger for {@link DiscordPS} plugin
 * Track handled errors only.
 */
public final class Debug {

    /**
     * Categorized error groups for debugging.
     *
     * @see asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.DebuggingMessage
     */
    public enum ErrorGroup {
        PLUGIN_VALIDATION,
        DATABASE_CONNECTION,
        CONFIG_VALIDATION,
        WEBHOOK_REFS_VALIDATION,
        WEBHOOK_CHANNEL_VALIDATION
    }

    /**
     * Non-fatal error that may occur throughout the application.
     */
    public enum Warning {
        DISCORD_SRV_VERSION_NOT_MATCHED("Detected DiscordSRV with unmatched version"),
        WEBHOOK_NOT_REGISTERED_DETECTED("Detected un-registered webhook created by this bot."),
        SHOWCASE_WEBHOOK_NOT_CONFIGURED("Showcase webhook is not configured! use the command `/setup showcase` to set it up."),
        NOTIFICATION_CHANNEL_NOT_SET("Plot-System notification channel is not set"),
        RUNTIME_SQL_EXCEPTION("Unknown SQL Exception Occurred. Please check the log files"),
        UPDATE_CHECKING_FAILED("Update checker failed to retrieve latest plugin version"),
        DATABASE_STRUCTURE_NOT_MATCH("JDBC table does not match expected structure. Required manual validation.");

        private final String defaultMessage;

        Warning(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    /**
     * Fatal error that will make DiscordPlotSystem in a not-ready state
     */
    public enum Error {
        // SEVERE
        DISCORD_SRV_NOT_DETECTED(
                ErrorGroup.PLUGIN_VALIDATION,
                "DiscordSRV plugin is required to run the system but it is not detected."),
        DISCORD_SRV_FAILED_TO_SUBSCRIBE(
                ErrorGroup.PLUGIN_VALIDATION,
                "Failed to subscribe to DiscordSRV API. Discord event listener will not work."),

        // HANDLED
        DATABASE_NOT_INITIALIZED(
                ErrorGroup.DATABASE_CONNECTION,
                "Database was not initialized."),

        // Webhook Response validation
        WEBHOOK_CAN_NOT_MANAGE(
                ErrorGroup.WEBHOOK_REFS_VALIDATION,
                "Unable to manage webhooks guild-wide, please allow the permission MANAGE_WEBHOOKS."),
        WEBHOOK_VALIDATION_UNKNOWN_EXCEPTION(
                ErrorGroup.WEBHOOK_REFS_VALIDATION,
                "Internal error while trying to validate webhook."),
        WEBHOOK_NOT_FOUND_IN_GUILD(
                ErrorGroup.WEBHOOK_REFS_VALIDATION,
                "Configured webhook is not found in the main guild."),
        WEBHOOK_NOT_CONFIGURED(
                ErrorGroup.WEBHOOK_REFS_VALIDATION,
                "Webhook is missing in config."),
        WEBHOOK_CHANNEL_NOT_FORUM(
                ErrorGroup.WEBHOOK_REFS_VALIDATION,
                "Webhook channel must be a forum channel."),

        // Webhook References confirmation
        WEBHOOK_TAG_VALIDATION_FAILED(
                ErrorGroup.WEBHOOK_CHANNEL_VALIDATION,
                "Failed to validate for webhook forum tags"),
        WEBHOOK_TAG_CONFIGURATION_FAILED(
                ErrorGroup.WEBHOOK_CHANNEL_VALIDATION,
                "Failed to parse the config file for available tags field, is the config.yml edited?"),
        // Config file (config.yml & webhook.yml)
        CONFIG_FILE_FAILED_TO_LOAD(
                ErrorGroup.CONFIG_VALIDATION,
                "Config file could not load from resources.");

        private final String defaultMessage;
        private final ErrorGroup group;

        Error(ErrorGroup group, String defaultMessage) {
            this.group = group;
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }

        public ErrorGroup getGroup() {
            return group;
        }
    }


    private final EnumMap<Warning, String> warning;
    private final EnumMap<Error, String> error;
    private final EnumMap<ErrorGroup, Error> errorGroup;

    /**
     * Create a new debugger.
     * This will construct a new map {@link EnumMap} for {@link Error}
     */
    Debug() {
        this.error = new EnumMap<>(Error.class);
        this.errorGroup = new EnumMap<>(ErrorGroup.class);
        this.warning = new EnumMap<>(Warning.class);
    }

    /**
     * Put a new handled error using its default message.
     * @param error The error signature
     */
    void putError(@NotNull Error error) {
        this.error.put(error, error.getDefaultMessage());
        this.errorGroup.put(error.group, error);
    }

    /**
     * Put a new error using a provided message detail.
     * @param error The error signature.
     * @param message The detailed message.
     */
    void putError(@NotNull Error error, String message) {
        this.error.put(error, message);
        this.errorGroup.put(error.group, error);
    }
    /**
     * Put a new handled warning using its default message.
     * @param warning The warning signature
     */
    void putWarning(@NotNull Warning warning) {
        this.warning.put(warning, warning.getDefaultMessage());
    }

    /**
     * Put a new warning using a provided message detail.
     * @param warning The warning signature.
     * @param message The detailed message.
     */
    void putWarning(@NotNull Warning warning, String message) {
        this.warning.put(warning, message);
    }

    /**
     * Mark a warning resolved, then this warning will not appear in the thrown list.
     *
     * @param warning The warning to resolve
     */
    void resolveWarning(@NotNull Warning warning) { this.warning.remove(warning); }

    /**
     * Mark an error resolved, then this error will not appear in the thrown list.
     *
     * @param error The error to resolve
     */
    void resolveError(@NotNull Error error) { this.error.remove(error); }

    @Contract(pure = true)
    public @NotNull Set<Map.Entry<Error, String>> allThrownErrors() {
        return this.error.entrySet();
    }

    @Contract(pure = true)
    public @NotNull Set<Error> allThrownType() {
        return this.error.keySet();
    }

    public boolean hasError(Error error) {
        return this.error.containsKey(error);
    }

    public boolean hasWarning(Warning warning) {
        return this.warning.containsKey(warning);
    }

    public boolean hasGroup(ErrorGroup group) {
        return this.errorGroup.containsKey(group);
    }

    public boolean hasAnyError() {
        return  !this.error.isEmpty() && !this.errorGroup.isEmpty();
    }

    public boolean hasAnyWarning() {
        return  !this.warning.isEmpty();
    }

    @Contract(pure = true)
    public @NotNull Set<Map.Entry<Warning, String>> allThrownWarnings() {
        return this.warning.entrySet();
    }
}
