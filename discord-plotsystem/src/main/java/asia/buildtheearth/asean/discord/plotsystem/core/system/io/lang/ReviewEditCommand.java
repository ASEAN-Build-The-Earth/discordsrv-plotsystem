package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum ReviewEditCommand implements SystemLang {
    EMBED_REVIEW_INFO(          "embeds.review-info"),
    EMBED_NOTHING_TO_EDIT(      "embeds.nothing-to-edit"),
    EMBED_DELETED_INFO(         "embeds.deleted-info"),
    EMBED_EDIT_INSTRUCTION(     "embeds.edit-instruction"),
    EMBED_PROVIDED_ERROR(       "embeds.provided-message-error"),
    EMBED_PREV_MEDIA_FOUND(     "embeds.previous-media-found"),
    EMBED_OWNER_NO_DISCORD(     "embeds.selected-no-discord"),
    EMBED_MEDIA_FOLDER_CREATED( "embeds.media-folder-created"),
    EMBED_MEDIA_FOLDER_FAILED(  "embeds.media-folder-failed"),
    EMBED_REVIEW_EDIT_SUCCESS(  "embeds.review-edit-success"),
    EMBED_REVIEW_SEND_SUCCESS(  "embeds.review-send-success"),

    MESSAGE_REVIEW_CONFIRMATION( "messages.review-confirmation"),
    MESSAGE_NEW_REVIEW_PREVIEW(  "messages.new-review-preview"),
    MESSAGE_PREVIOUS_REVIEW(     "messages.previous-review"),
    MESSAGE_SAVED_ATTACHMENTS(   "messages.saved-attachments"),
    MESSAGE_PREV_MEDIA_DELETED(  "messages.previous-media-deleted"),
    MESSAGE_SQL_GET_ERROR(       "messages.sql-get-error"),
    MESSAGE_SQL_SAVE_ERROR(      "messages.sql-save-error"),
    MESSAGE_VALIDATION_ERROR(    "messages.validation-error"),
    MESSAGE_FEEDBACK_SEND_ERROR( "messages.feedback-send-error"),
    MESSAGE_PREVIEW_ERROR(       "messages.preview-error");

    private final String path;

    ReviewEditCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.REVIEW_EDIT + this.path;
    }
}
