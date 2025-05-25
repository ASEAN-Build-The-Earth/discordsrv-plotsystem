package github.tintinkung.discordps.core.system.io.lang;

import github.tintinkung.discordps.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum PlotFetchCommand implements SystemLang {
    DESC(          "description.command"),
    DESC_PLOT_ID(  "description.plot-id"),
    DESC_OVERRIDE( "description.override"),
    DESC_TAG(      "description.tag"),

    EMBED_CANNOT_MAKE_ARCHIVE("embeds.cannot-fetch-archive"),
    EMBED_STATUS_NOT_SET(     "embeds.status-not-set"),
    EMBED_ALREADY_REGISTERED( "embeds.already-registered"),
    EMBED_FETCH_SUCCESS(      "embeds.fetch-success"),
    EMBED_OVERRIDE_OPTIONS(   "embeds.override-options"),
    EMBED_UNTRACKED_OPTIONS(  "embeds.not-tracked-options"),
    EMBED_ON_CREATE(          "embeds.on-create"),
    EMBED_ON_CREATE_SUCCESS(  "embeds.on-create-successful"),

    MESSAGE_CONFIRM_FETCH("messages.confirm-fetch"),
    MESSAGE_PLOT_ID("messages.plot-id"),
    MESSAGE_OVERRIDE("messages.override"),
    MESSAGE_STATUS("messages.primary-status"),
    MESSAGE_OVERRIDE_ENABLED("messages.override-enabled"),
    MESSAGE_OVERRIDE_DISABLED("messages.override-disabled"),
    MESSAGE_SQL_GET_ERROR("messages.sql-get-error"),
    MESSAGE_SQL_PLOT_ERROR("messages.sql-plot-error"),
    MESSAGE_VALIDATION_ERROR("messages.error-override-validation");

    private final String path;

    PlotFetchCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.PLOT_FETCH + this.path;
    }
}
