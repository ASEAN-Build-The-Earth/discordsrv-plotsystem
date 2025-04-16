package github.tintinkung.discordps;

public abstract class ConfigPaths {
    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "url";
    public static final String DATABASE_NAME = DATABASE + "name";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";
    public static final String DATABASE_WEBHOOK_TABLE = DATABASE + "webhook-table";

    // Database
    private static final String CHANNEL = "channel.";
    public static final String DISCORD_SRV = CHANNEL + "discord-srv";
    public static final String PLOT_STATUS = CHANNEL + "plot-status";
    public static final String PLOT_NOTIFY = CHANNEL + "plot-notify";

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


    // Status Tags
    private static final String EMBED = "embed-color.";
    public static final String EMBED_COLOR_FINISHED = EMBED + "finished";
    public static final String EMBED_COLOR_REJECTED = EMBED + "rejected";
    public static final String EMBED_COLOR_APPROVED = EMBED + "approved";
    public static final String EMBED_COLOR_ARCHIVED = EMBED + "archived";
    public static final String EMBED_COLOR_ON_GOING = EMBED + "on-going";
}
