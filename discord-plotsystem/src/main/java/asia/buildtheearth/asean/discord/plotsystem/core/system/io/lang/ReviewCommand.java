package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum ReviewCommand implements SystemLang {
    DESC_COMMAND("description.command"),
    DESC_PLOT_ID("description.plot-id"),
    DESC_COMMAND_EDIT("description.command-edit"),
    DESC_COMMAND_SEND("description.command-send"),
    DESC_COMMAND_ARCHIVE("description.command-archive");

    private final String path;

    ReviewCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.REVIEW_COMMAND + this.path;
    }
}
