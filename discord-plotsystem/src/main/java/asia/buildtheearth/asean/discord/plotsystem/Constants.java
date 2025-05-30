package asia.buildtheearth.asean.discord.plotsystem;

import java.awt.Color;

/**
 * Constant values to use plugin-wide.
 */
public abstract class Constants {
    /**
     * (LEGACY) DiscordSRV(v1.29.0) webhook naming prefix "DiscordSRV ".
     * <a href="https://github.com/DiscordSRV/DiscordSRV/blob/9d4734818ab27069d76f264a4cda74a699806770/src/main/java/github/scarsz/discordsrv/util/WebhookUtil.java#L430">
     *     github.scarsz.discordsrv.util.WebhookUtil
     * </a>
     */
    public static final String DISCORD_SRV_WEBHOOK_PREFIX_LEGACY = "DiscordSRV ";

    /**
     * DiscordSRV(v1.29.0) webhook naming prefix "DSRV ".
     * <a href="https://github.com/DiscordSRV/DiscordSRV/blob/9d4734818ab27069d76f264a4cda74a699806770/src/main/java/github/scarsz/discordsrv/util/WebhookUtil.java#L431">
     *     github.scarsz.discordsrv.util.WebhookUtil
     * </a>
     */
    public static final String DISCORD_SRV_WEBHOOK_PREFIX = "DSRV ";

    /**
     * Webhook avatar file name for initial bot setup.
     * Must be placed in the root directory of plugin data folder.
     */
    public static final String WEBHOOK_AVATAR_FILE = "discordps-webhook-avatar";

    /**
     * Per-User avatar image cache stored in: <pre>plugin/media/cache/UUID/avatar-image-UUID.xxx</pre>
     */
    public static final String BUILDER_AVATAR_FILE = "avatar-image";

    /**
     * Per-Plot image files stored in: <pre>plugin/media/plot-xx/plot-image-xx.xxx</pre>
     */
    public static final String PLOT_IMAGE_FILE = "plot-image";

    public static final Color RED    = new Color(14495300);
    public static final Color ORANGE = new Color(14188300);
    public static final Color YELLOW = new Color(16632664);
    public static final Color GREEN  = new Color(7909721);
    public static final Color BLUE   = new Color(5614830);
    public static final Color PURPLE = new Color(11177686);
    public static final Color GRAY   = new Color(8421504);
}
