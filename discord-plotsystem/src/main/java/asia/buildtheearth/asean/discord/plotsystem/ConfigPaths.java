package asia.buildtheearth.asean.discord.plotsystem;

/**
 * Config file {@code config.yml} yaml path constants
 */
public abstract class ConfigPaths {
    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "url";
    public static final String DATABASE_NAME = DATABASE + "name";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";
    public static final String DATABASE_WEBHOOK_TABLE = DATABASE + "webhook-table";

    // Plot Forum Setting
    private static final String PLOT = "plot-forum-settings.";
    private static final String PLOT_STARTER_POST = PLOT + "starter-post.";
    private static final String PLOT_RECLAIM_SAME = PLOT + "reclaim-in-same-thread.";
    public static final String PLOT_RECLAIM_SAME_ENABLE = PLOT_RECLAIM_SAME + "enabled";
    /**
     * NOT IMPLEMENTED
     * {@snippet :
     * """
     * # Only leave the starter post
     * # Warning: this plugin don't keep message history, use at own risk.
     * purge-all-messages: false
     * """}
     */
    public static final String PLOT_RECLAIM_PURGE_ALL_MESSAGES = PLOT_RECLAIM_SAME + "purge-all-messages";
    /**
     * NOT IMPLEMENTED
     * {@snippet :
     * """
     * # Clear each status embeds (the embed with help button)
     * clear-status-embed: false
     * """}
     */
    public static final String PLOT_RECLAIM_CLEAR_STATUS_EMBED = PLOT_RECLAIM_SAME + "clear-status-embed";
    public static final String PLOT_RECLAIM_CLEAR_NOTIFICATION = PLOT_RECLAIM_SAME + "clear-notification";
    public static final String PLOT_KEEP_ABANDONED_USER = PLOT_STARTER_POST + "keep-abandoned-user";
    public static final String PLOT_KEEP_PLOT_HISTORIES = PLOT_STARTER_POST + "keep-plot-histories";

    // Notifications
    public static final String NOTIFICATION = "system-notification.";
    public static final String NOTIFICATION_CHANNEL = NOTIFICATION + "channel-id";
    public static final String NOTIFICATION_CONTENT = NOTIFICATION + "notification-content";
    public static final String NOTIFICATION_ERRORS = NOTIFICATION + "notify-system-errors";
    public static final String NOTIFICATION_PLUGIN = NOTIFICATION + "notify-plugin-states";

    // Webhook
    private static final String WEBHOOK = "webhook.";
    public static final String WEBHOOK_NAME = WEBHOOK + "name";
    public static final String WEBHOOK_ID = WEBHOOK + "id";
    public static final String WEBHOOK_TOKEN = WEBHOOK + "token";
    public static final String WEBHOOK_CHANNEL_ID = WEBHOOK + "channel-id";
    public static final String WEBHOOK_GUILD_ID = WEBHOOK + "guild-id";
    public static final String WEBHOOK_URL = WEBHOOK + "url";

    // Status Tags
    private static final String TAG = "available-tag.";
    public static final String TAG_FINISHED = TAG + "finished";
    public static final String TAG_REJECTED = TAG + "rejected";
    public static final String TAG_APPROVED = TAG + "approved";
    public static final String TAG_ARCHIVED = TAG + "archived";
    public static final String TAG_ON_GOING = TAG + "on-going";
    public static final String TAG_ABANDONED = TAG + "abandoned";


    // Status Tags
    private static final String EMBED = "embed-color.";
    public static final String EMBED_COLOR_FINISHED = EMBED + "finished";
    public static final String EMBED_COLOR_REJECTED = EMBED + "rejected";
    public static final String EMBED_COLOR_APPROVED = EMBED + "approved";
    public static final String EMBED_COLOR_ARCHIVED = EMBED + "archived";
    public static final String EMBED_COLOR_ON_GOING = EMBED + "on-going";
    public static final String EMBED_COLOR_ABANDONED = EMBED + "abandoned";
}
