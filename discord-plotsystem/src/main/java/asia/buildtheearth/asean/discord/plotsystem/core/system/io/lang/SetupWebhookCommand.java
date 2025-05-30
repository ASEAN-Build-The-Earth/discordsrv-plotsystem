package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum SetupWebhookCommand implements SystemLang {
    DESC(         "description.command"),
    DESC_CHANNEL( "description.channel"),
    DESC_NAME(    "description.name"),

    EMBED_ALREADY_CONFIGURED(        "embeds.already-configured"),
    EMBED_BAD_USERNAME_ERROR(        "embeds.bad-username"),
    EMBED_BAD_CHANNEL_ERROR(         "embeds.bad-channel"),
    EMBED_CANT_VERIFY_CHANNEL_ERROR( "embeds.cant-verify-channel"),
    EMBED_INTERNAL_EXCEPTION(        "embeds.internal-exception"),
    EMBED_CONFIRM_SETTINGS(          "embeds.confirm-settings"),
    EMBED_CREATED_NO_PERMISSION(     "embeds.created-with-no-perms"),
    EMBED_IMAGE_DOWNLOAD_FAILED(     "embeds.image-download-failed"),
    EMBED_SETUP_AVATAR_IMAGE(        "embeds.setup-avatar-image"),
    EMBED_UPLOADING_ATTACHMENT(      "embeds.uploading-attachments"),
    EMBED_FILE_NOT_FOUND(            "embeds.file-not-found"),
    EMBED_CREATED_SUCCESS(           "embeds.created-success"),
    EMBED_CREATED_INFO(              "embeds.created-info"),
    EMBED_CREATE_CONFIRMATION(       "embeds.created-confirmation"),

    MESSAGE_WEBHOOK_NAME("messages.webhook-name"),
    MESSAGE_WEBHOOK_CHANNEL("messages.webhook-channel");

    private final String path;

    SetupWebhookCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.SETUP_WEBHOOK + this.path;
    }
}
