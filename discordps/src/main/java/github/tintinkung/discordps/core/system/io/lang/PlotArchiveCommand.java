package github.tintinkung.discordps.core.system.io.lang;

import github.tintinkung.discordps.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum PlotArchiveCommand implements SystemLang {
    DESC(          "description.command"),
    DESC_PLOT_ID(  "description.plot-id"),
    DESC_OVERRIDE( "description.override"),

    EMBED_IMAGE_NOT_SET(            "embeds.image-not-set"),
    EMBED_IMAGE_ATTACHED(           "embeds.image-attached"),
    EMBED_FOUND_THREAD(             "embeds.found-thread"),
    EMBED_NOTHING_TO_ARCHIVE(       "embeds.nothing-to-archive"),
    EMBED_ON_UPLOAD(                "embeds.on-upload"),
    EMBED_ON_ARCHIVE(               "embeds.on-archive"),
    EMBED_ON_IMAGE_DOWNLOAD_FAILED( "embeds.on-attachment-download-failed"),
    EMBED_SHOWCASE_THIS_PLOT(       "embeds.showcase-this-plot"),
    EMBED_MEDIA_FOLDER_CREATED(     "embeds.media-folder-created"),
    EMBED_MEDIA_PENDING_CACHE(      "embeds.media-pending-cache"),
    EMBED_MEDIA_FOLDER_FAILED(      "embeds.media-folder-failed"),
    EMBED_ARCHIVE_SUCCESSFUL(       "embeds.archive-successful"),

    MESSAGE_ON_LAYOUT_FAILED("messages.on-layout-failed"),
    MESSAGE_CANNOT_SHOWCASE("messages.cannot-showcase"),
    MESSAGE_INTERACTION_EMPTY("messages.interaction-empty"),
    MESSAGE_MEDIA_FOLDER_NOT_EXIST("messages.media-folder-not-exist"),
    MESSAGE_MEDIA_SAVED("messages.media-saved"),
    MESSAGE_MEDIA_CACHE_DELETED("messages.media-cache-deleted"),
    MESSAGE_CONFIRM_ARCHIVE("messages.confirm-archive"),
    MESSAGE_DETECTED_FILES("messages.detected-files"),
    MESSAGE_OVERRIDE_ENABLED("messages.archive-override-enabled"),
    MESSAGE_OVERRIDE_DISABLED("messages.archive-override-disabled"),

    ;

    private final String path;

    PlotArchiveCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.PLOT_ARCHIVE + this.path;
    }
}
