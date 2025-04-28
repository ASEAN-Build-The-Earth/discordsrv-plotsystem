package github.tintinkung.discordps;

import github.tintinkung.discordps.utils.ComponentUtil.ComponentID;
import github.tintinkung.discordps.utils.ComponentUtil.ComponentIDWithPayload;

import static github.tintinkung.discordps.utils.ComponentUtil.newComponentID;

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
     * Per-User avatar image cache stored in: <pre>plugin/media/cache/UUID/avatar-image.xxx</pre>
     */
    public static final String BUILDER_AVATAR_FILE = "avatar-image";

    /**
     * Per-Plot image files stored in: <pre>plugin/media/plot-xx/plot-image.xxx</pre>
     */
    public static final String PLOT_IMAGE_FILE = "plot-image";

    /**
     * Button ID when an image is confirmed
     */
    public static final String CONFIRM_AVATAR_BUTTON = "ConfirmWebhookAvatar";

    /**
     * Button ID when user provided an avatar image setting up webhook
     */
    public static final String PROVIDED_IMAGE_BUTTON = "ProvidedWebhookImage";

    /**
     * Button ID when setup command is confirmed
     */
    public static final String CONFIRM_CONFIG_BUTTON = "ConfirmWebhookConfig";

    /**
     * Button ID to cancel setup webhook events
     */
    public static final String CANCEL_CONFIG_BUTTON = "CancelWebhookConfig";

    /**
     * "Help" button for every plot in the tracker forum
     */
    public static final String HELP_BUTTON = "HelpButton";

    /**
     * "View Feedback" button for every plot in the tracker forum
     */
    public static final String FEEDBACK_BUTTON = "FeedbackButton";


    // Setup Webhook command events
    public static final ComponentID NEW_CONFIRM_AVATAR_BUTTON = (t, u) -> newComponentID(CONFIRM_AVATAR_BUTTON, t, u);
    public static final ComponentID NEW_PROVIDED_IMAGE_BUTTON = (t, u) -> newComponentID(PROVIDED_IMAGE_BUTTON, t, u);
    public static final ComponentID NEW_CONFIRM_CONFIG_BUTTON = (t, u) -> newComponentID(CONFIRM_CONFIG_BUTTON, t, u);
    public static final ComponentID NEW_CANCEL_CONFIG_BUTTON = (t, u) -> newComponentID(CANCEL_CONFIG_BUTTON, t, u);

    // Plot-System button events
    public static final ComponentIDWithPayload<Integer> NEW_PLOT_HELP_BUTTON = (a, b, c) -> newComponentID(HELP_BUTTON, a, b, c);
    public static final ComponentIDWithPayload<Integer> NEW_FEEDBACK_BUTTON = (a, b, c) -> newComponentID(FEEDBACK_BUTTON, a, b, c);
}
