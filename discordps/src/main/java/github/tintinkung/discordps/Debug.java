package github.tintinkung.discordps;


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

    public enum ErrorGroup {
        PLUGIN_VALIDATION(
                "DiscordSRV plugin validated successfully",
                "Failed to validate required plugin: DiscordSRV"
        ),
        DATABASE_CONNECTION(
                "Connected to the Plot-System database",
                "Failed to connect to the Plot-System database"
        ),
        CONFIG_VALIDATION(
                "Configuration files loaded successfully",
                "Failed to load one or more configuration files"
        ),
        WEBHOOK_REFS_VALIDATION(
                "Webhook configurations are valid",
                "Failed to validate webhook configurations"
        ),
        WEBHOOK_REFS_CONFIRMATION(
                "Webhook channel confirmed and operational",
                "The configured webhook has unresolved channel errors"
        );
        private final String unresolved;
        private final String resolved;

        ErrorGroup(String resolved, String unresolved) {
            this.unresolved = unresolved;
            this.resolved = resolved;
        }

        public String getUnresolved() {
            return unresolved;
        }

        public String getResolved() {
            return resolved;
        }
    }

    public enum Warning {

        WEBHOOK_NOT_REGISTERED_DETECTED(
                "Detected un-registered webhook created by this bot."),
        NOTIFICATION_CHANNEL_NOT_SET("Plot-System notification channel is not set"),
        RUNTIME_SQL_EXCEPTION("Unknown SQL Exception Occurred. Please check the log files"),

        PLOT_SYSTEM_NOT_DETECTED("Plot-System plugin is not detected. "
                + "You can still use this plugin as long as it is connected to the Plot-System database. "
                + "Plot-System related minecraft events may not be functional."),
        PLOT_SYSTEM_SYMBOL_NOT_FOUND("Failed to get Plot-System class reference, "
                + "coordinates conversion will be disabled");

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
                ErrorGroup.WEBHOOK_REFS_CONFIRMATION,
                "Failed to validate for webhook forum tags"),
        WEBHOOK_TAG_CONFIGURATION_FAILED(
                ErrorGroup.WEBHOOK_REFS_CONFIRMATION,
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


    private final EnumMap<Warning, String> warning;;

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
     * Mark a warning resolved, then this warning will not appear in all thrown list.
     * @param warning The warning to resolve
     */
    void resolveWarning(@NotNull Warning warning) { this.warning.remove(warning); }

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
