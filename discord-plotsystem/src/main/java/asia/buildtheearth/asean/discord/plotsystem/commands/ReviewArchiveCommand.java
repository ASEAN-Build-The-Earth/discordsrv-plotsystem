package asia.buildtheearth.asean.discord.plotsystem.commands;

import org.jetbrains.annotations.NotNull;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewCommand.DESC_COMMAND_ARCHIVE;

final class ReviewArchiveCommand extends PlotArchiveCommand {

    public ReviewArchiveCommand(@NotNull String name, @NotNull String plotID, @NotNull String override) {
        super(name, plotID, override);

        this.setDescription(getLangManager().get(DESC_COMMAND_ARCHIVE));
    }
}
