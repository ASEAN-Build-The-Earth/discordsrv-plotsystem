package github.tintinkung.discordps;

import github.tintinkung.discordps.utils.ComponentUtil.ComponentID;
import github.tintinkung.discordps.utils.ComponentUtil.ComponentIDWithPayload;

import static github.tintinkung.discordps.utils.ComponentUtil.newComponentID;


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


    // ^(?<plugin>\w+)/(?<type>\w+)/(?<id>\w+)(?:/(?<params>.*?))?/?$

    public static final String HELP_BUTTON = "HelpButton";

    public static final String WEBHOOK_AVATAR_FILE = "discordps-webhook-avatar";

    /**
     * Button ID when an image is confirmed
     */
    public static final String CONFIRM_AVATAR_BUTTON = "ConfirmWebhookAvatar";

    /**
     *
     */
    public static final String PROVIDED_IMAGE_BUTTON = "ProvidedWebhookImage";

    /**
     * Button ID when setup command is confirmed
     */
    public static final String CONFIRM_CONFIG_BUTTON = "ConfirmWebhookConfig";
    public static final String CANCEL_CONFIG_BUTTON = "CancelWebhookConfig";



    // Setup Webhook command events
    public static final ComponentID NEW_CONFIRM_AVATAR_BUTTON = (t, u) -> newComponentID(CONFIRM_AVATAR_BUTTON, t, u);
    public static final ComponentID NEW_PROVIDED_IMAGE_BUTTON = (t, u) -> newComponentID(PROVIDED_IMAGE_BUTTON, t, u);
    public static final ComponentID NEW_CONFIRM_CONFIG_BUTTON = (t, u) -> newComponentID(CONFIRM_CONFIG_BUTTON, t, u);
    public static final ComponentID NEW_CANCEL_CONFIG_BUTTON = (t, u) -> newComponentID(CANCEL_CONFIG_BUTTON, t, u);

    // Plot-System button events
    public static final ComponentIDWithPayload<Integer> NEW_PLOT_HELP_BUTTON = (a, b, c) -> newComponentID(HELP_BUTTON, a, b, c);
}
