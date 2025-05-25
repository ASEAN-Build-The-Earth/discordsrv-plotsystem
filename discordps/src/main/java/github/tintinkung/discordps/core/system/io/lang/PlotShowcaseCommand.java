package github.tintinkung.discordps.core.system.io.lang;

import github.tintinkung.discordps.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum PlotShowcaseCommand implements SystemLang {
    DESC(         "description.command"),
    DESC_PLOT_ID( "description.plot-id"),

    EMBED_SHOWCASE_INFO(       "embeds.showcase-info"),
    EMBED_NOTHING_TO_SHOWCASE( "embeds.nothing-to-showcase"),
    EMBED_PLOT_NOT_ARCHIVED(   "embeds.plot-not-archived"),
    EMBED_ON_SHOWCASE(         "embeds.on-showcase"),
    EMBED_ON_SHOWCASE_SUCCESS( "embeds.showcase-success"),

    MESSAGE_THREAD_ID(          "messages.thread-id"),
    MESSAGE_SHOWCASED_TO(       "messages.showcased-to"),
    MESSAGE_PLOT_TITLE(         "messages.plot-title"),
    MESSAGE_OWNER(              "messages.owner"),
    MESSAGE_OWNER_AVATAR(       "messages.owner-avatar"),
    MESSAGE_IMAGE_FILES(        "messages.image-files"),
    MESSAGE_LOCATION(           "messages.location"),
    MESSAGE_GOOGLE_MAP_LINK(    "messages.google-map-link"),
    MESSAGE_SHOWCASE_FAILED(    "messages.error-failed-to-showcase"),
    MESSAGE_SQL_DELETE_ERROR(   "messages.sql-delete-error"),
    MESSAGE_VALIDATION_ERROR(   "messages.validation-error"),
    MESSAGE_DATA_RETURNED_NULL( "messages.plot-data-returned-null"),
    MESSAGE_DATA_UNKNOWN_OWNER( "messages.plot-data-no-owner");

    private final String path;

    PlotShowcaseCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.PLOT_SHOWCASE + this.path;
    }
}
