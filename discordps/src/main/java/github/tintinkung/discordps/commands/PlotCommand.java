package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.PlotArchiveEvent;
import github.tintinkung.discordps.commands.events.PlotDeleteEvent;
import github.tintinkung.discordps.commands.events.PlotFetchEvent;
import github.tintinkung.discordps.commands.events.PlotShowcaseEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotArchive;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;
import github.tintinkung.discordps.commands.interactions.OnPlotFetch;
import github.tintinkung.discordps.commands.interactions.OnPlotShowcase;
import github.tintinkung.discordps.commands.providers.SlashCommand;
import static github.tintinkung.discordps.core.system.io.lang.PlotCommand.DESC;

public final class PlotCommand extends CommandData {

    // Common parameters
    // TODO: rename to /plotctl as for plot controls commands
    public static final String PLOT = "plot";
    public static final String PLOT_ID = "id";
    public static final String PLOT_OVERRIDE = "override";

    // Plot Archive command
    public static final String ARCHIVE = "archive";

    // Plot Fetch command
    public static final String FETCH = "fetch";

    // Plot Delete command
    public static final String DELETE = "delete";

    // Plot Showcase command
    public static final String SHOWCASE = "showcase";

    private final PlotArchiveCommand plotArchiveCommand;
    private final PlotFetchCommand plotFetchCommand;
    private final PlotDeleteCommand plotDeleteCommand;
    private final PlotShowcaseCommand plotShowcaseCommand;

    public PlotCommand(boolean defaultEnabled) {
        super(PLOT, DiscordPS.getSystemLang().get(DESC));
        this.addSubcommands(
                plotArchiveCommand = new PlotArchiveCommand(ARCHIVE, PLOT_ID, PLOT_OVERRIDE),
                plotFetchCommand = new PlotFetchCommand(FETCH, PLOT_ID, PLOT_OVERRIDE),
                plotDeleteCommand = new PlotDeleteCommand(DELETE, PLOT_ID),
                plotShowcaseCommand = new PlotShowcaseCommand(SHOWCASE, PLOT_ID)
        );
        this.setDefaultEnabled(defaultEnabled);
    }

    public SlashCommand<OnPlotFetch> getFetchCommand() {
        return this.plotFetchCommand;
    }

    public SlashCommand<OnPlotArchive> getArchiveCommand() {
        return this.plotArchiveCommand;
    }

    public SlashCommand<OnPlotDelete> getDeleteCommand() {
        return this.plotDeleteCommand;
    }

    public SlashCommand<OnPlotShowcase> getShowcaseCommand() {
        return this.plotShowcaseCommand;
    }

    /**
     * Get the sub command "archive"
     *
     * @return The subcommand as event handler
     */
    public PlotArchiveEvent getArchiveEvent() {
        return this.plotArchiveCommand;
    }

    /**
     * Get the sub command "fetch"
     *
     * @return The subcommand as event handler
     */
    public PlotFetchEvent getFetchEvent() {
        return this.plotFetchCommand;
    }

    /**
     * Get the sub command "delete"
     *
     * @return The subcommand as event handler
     */
    public PlotDeleteEvent getDeleteEvent() {
        return this.plotDeleteCommand;
    }


    /**
     * Get the sub command "showcase"
     *
     * @return The subcommand as event handler
     */
    public PlotShowcaseEvent getShowcaseEvent() {
        return this.plotShowcaseCommand;
    }
}
