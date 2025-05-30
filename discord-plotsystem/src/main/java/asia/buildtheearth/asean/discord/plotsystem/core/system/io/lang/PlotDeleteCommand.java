package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum PlotDeleteCommand implements SystemLang {
    DESC(         "description.command"),
    DESC_PLOT_ID( "description.plot-id"),

    EMBED_DELETE_INFO(          "embeds.delete-info"),
    EMBED_NOTHING_TO_DELETE(    "embeds.nothing-to-delete"),
    EMBED_DELETED_INFO(         "embeds.deleted-info"),

    MESSAGE_SQL_GET_ERROR(    "messages.sql-get-error"),
    MESSAGE_SQL_DELETE_ERROR( "messages.sql-delete-error"),
    MESSAGE_VALIDATION_ERROR( "messages.validation-error");

    private final String path;

    PlotDeleteCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.PLOT_DELETE + this.path;
    }
}
