package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum PlotCommand implements SystemLang {
    DESC("description"),
    EMPTY_ACTION_EXCEPTION("messages.empty-action-exception"),
    NULL_ACTION_EXCEPTION("messages.null-action-exception"),
    UNKNOWN_OWNER_EXCEPTION("messages.unknown-owner-exception");

    private final String path;
    PlotCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.PLOT_CONTROL_COMMAND + this.path;
    }
}
