package asia.buildtheearth.asean.discord.plotsystem.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotArchiveEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotDeleteEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotFetchEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotShowcaseEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotDelete;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotFetch;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotShowcase;
import asia.buildtheearth.asean.discord.commands.SlashCommand;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotCommand.DESC;

/**
 * Plot Control command's data, parent command of all plot controls related commands.
 *
 * <p>This command contains dangerous operations and is intended to be privately usable in the discord server,
 * do not expose this command to un-trusted roles.</p>
 *
 * <p>For archiving and showcasing a plot, please refer to the command {@link ReviewCommand}
 * as it expose command {@code /review archive} to safely archive the plot.</p>
 *
 * <p>Subcommands include the following:</p>
 * <ul>
 *     <li>{@link PlotArchiveEvent} {@code /plotctl archive <plot_id> <override>}<br/>
 *     Interactively archive a plot.</li>
 *     <li>{@link PlotFetchEvent} {@code /plotctl fetch <plot_id> <override>}<br/>
 *     Interactively update/fetch a plot to new status.</li>
 *     <li>{@link PlotDeleteEvent} {@code /plotctl delete <plot_id>}<br/>
 *     Delete a plot from being tracked by the system, does not delete thread.</li>
 *     <li>{@link PlotShowcaseEvent} {@code /plotctl showcase <plot_id>}<br/>
 *     Showcase an archived plot to configured showcase webhook.</li>
 * </ul>
 *
 * @see PlotArchiveEvent
 * @see PlotFetchEvent
 * @see PlotDeleteEvent
 * @see PlotShowcaseEvent
 */
public final class PlotCommand extends CommandData {
    /** Plot command signature: {@code plotctl} */
    public static final String PLOT_CTL = "plotctl";

    /** Plot command's parameter signature: {@code id} */
    public static final String PLOT_ID = "id";

    /** Plot command's parameter signature: {@code override} */
    public static final String PLOT_OVERRIDE = "override";

    /** Plot Archive command signature: {@code archive} */
    public static final String ARCHIVE = "archive";

    /** Plot Fetch command signature: {@code fetch} */
    public static final String FETCH = "fetch";

    /** Plot Delete command signature: {@code delete} */
    public static final String DELETE = "delete";

    /** Plot Showcase command signature: {@code showcase} */
    public static final String SHOWCASE = "showcase";

    private final PlotArchiveCommand plotArchiveCommand;
    private final PlotFetchCommand plotFetchCommand;
    private final PlotDeleteCommand plotDeleteCommand;
    private final PlotShowcaseCommand plotShowcaseCommand;

    /**
     * Initialize plot command data creating all of its subcommands.
     */
    public PlotCommand() {
        super(PLOT_CTL, DiscordPS.getSystemLang().get(DESC));
        this.addSubcommands(
                plotArchiveCommand = new PlotArchiveCommand(ARCHIVE, PLOT_ID, PLOT_OVERRIDE),
                plotFetchCommand = new PlotFetchCommand(FETCH, PLOT_ID, PLOT_OVERRIDE),
                plotDeleteCommand = new PlotDeleteCommand(DELETE, PLOT_ID),
                plotShowcaseCommand = new PlotShowcaseCommand(SHOWCASE, PLOT_ID)
        );
    }

    /**
     * Get the sub command {@code fetch} returning its event.
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnPlotFetch> getFetchCommand() {
        return this.plotFetchCommand;
    }

    /**
     * Get the sub command {@code archive}
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnPlotArchive> getArchiveCommand() {
        return this.plotArchiveCommand;
    }

    /**
     * Get the sub command {@code delete}
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnPlotDelete> getDeleteCommand() {
        return this.plotDeleteCommand;
    }

    /**
     * Get the sub command {@code showcase}
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnPlotShowcase> getShowcaseCommand() {
        return this.plotShowcaseCommand;
    }

    /**
     * Get the sub command {@code archive} returning its event.
     *
     * @return The subcommand event interface
     * @see PlotArchiveEvent
     */
    public PlotArchiveEvent getArchiveEvent() {
        return this.plotArchiveCommand;
    }

    /**
     * Get the sub command {@code fetch} returning its event.
     *
     * @return The subcommand event interface
     * @see PlotFetchEvent
     */
    public PlotFetchEvent getFetchEvent() {
        return this.plotFetchCommand;
    }

    /**
     * Get the sub command {@code delete} returning its event.
     *
     * @return The subcommand event interface
     * @see PlotDeleteEvent
     */
    public PlotDeleteEvent getDeleteEvent() {
        return this.plotDeleteCommand;
    }


    /**
     * Get the sub command {@code showcase} returning its event.
     *
     * @return The subcommand event interface
     * @see PlotShowcaseEvent
     */
    public PlotShowcaseEvent getShowcaseEvent() {
        return this.plotShowcaseCommand;
    }
}
