package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

/**
 * Formatter tokens used in language file messages,
 * replaced at runtime with relevant values.
 *
 * <p>The available constants define how each formatting token is used in its context.</p>
 */
public abstract class Format {
    /** Replaced with the plot's numeric ID. */
    public static final String PLOT_ID = "{plotID}";

    /** Replaced with the formatted name of the plot's owner. */
    public static final String OWNER = "{owner}";

    /** Replaced with the Discord thread's snowflake ID. */
    public static final String THREAD_ID = "{threadID}";

    /** Replaced with the Discord message's snowflake ID. */
    public static final String MESSAGE_ID = "{messageID}";

    /** Replaced with the Discord member's user ID. */
    public static final String USER_ID = "{userID}";

    /** Replaced with a Discord snowflake timestamp. */
    public static final String TIMESTAMP = "{timestamp}";

    /** Replaced with the filename of a referenced {@link java.io.File}. */
    public static final String FILENAME = "{filename}";

    /** Replaced with the absolute path of a referenced file. */
    public static final String PATH = "{path}";

    /** Replaced with a custom-defined label message. */
    public static final String LABEL = "{label}";

    /** Replaced with the simple class name of an event (usually {@link Class#getSimpleName()}). */
    public static final String EVENT = "{event}";

    /** Replaced with an incrementing counter or index. */
    public static final String COUNT = "{count}";

    /** Replaced with country name. */
    public static final String COUNTRY = "{country}";

    /** Replaced with city project name. */
    public static final String CITY = "{city}";
}
