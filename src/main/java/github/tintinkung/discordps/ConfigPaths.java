package github.tintinkung.discordps;

public abstract class ConfigPaths {
    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "url";
    public static final String DATABASE_NAME = DATABASE + "name";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";

    // Database
    private static final String CHANNEL = "channel.";
    public static final String DISCORD_SRV = CHANNEL + "discord-srv";
    public static final String PLOT_STATUS = CHANNEL + "plot-status";
    public static final String PLOT_NOTIFY = CHANNEL + "plot-notify";

    // Webhook
    private static final String WEBHOOK = "webhook.";
    public static final String WEBHOOK_NAME = WEBHOOK + "name";
    public static final String WEBHOOK_ID = WEBHOOK + "id";
    public static final String WEBHOOK_CHANNEL_ID = WEBHOOK + "channel-id";
    public static final String WEBHOOK_URL = WEBHOOK + "url";
}
