package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.commands.SlashCommand;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.ReviewEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnReview;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewCommand.DESC_COMMAND;

/**
 * Review command data.
 *
 * <p>Currently has 3 commands:<ul>
 *     <li>{@code /review edit} Edit a private review of a plot.</li>
 *     <li>{@code /review send} Send a new review to a plot publicly. </li>
 *     <li>{@code /review archive} Archive a reviewed plot.</li>
 * </ul></p>
 * <p>Note: {@code /review archive} inherit all functionality from {@link PlotArchiveCommand}</p>
 *
 * @see #getEditEvent()
 * @see #getSendEvent()
 * @see #getArchiveCommand()
 */
public class ReviewCommand extends CommandData {

    /** Review command signature: {@code review} */
    public static final String REVIEW = "review";

    /** Review command's parameter signature: {@code id} */
    public static final String PLOT_ID = "id";

    /** Subcommand signature: {@code edit} */
    public static final String EDIT = "edit";

    /** Subcommand signature: {@code send} */
    public static final String SEND = "send";

    /** Subcommand signature: {@code archive} */
    public static final String ARCHIVE = "archive";

    private final ReviewEditCommand reviewEditCommand;
    private final ReviewSendCommand reviewSendCommand;
    private final ReviewArchiveCommand reviewArchiveCommand;

    /**
     * Initialize review command data creating all of its subcommands.
     */
    public ReviewCommand() {
        super(REVIEW, DiscordPS.getSystemLang().get(DESC_COMMAND));
        this.addSubcommands(
            reviewEditCommand = new ReviewEditCommand(EDIT, PLOT_ID),
            reviewSendCommand = new ReviewSendCommand(SEND, PLOT_ID),
            reviewArchiveCommand = new ReviewArchiveCommand(ARCHIVE, PlotCommand.PLOT_ID, PlotCommand.PLOT_OVERRIDE)
        );
    }

    /**
     * Get the subcommand {@code send} as a trigger interface
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnReview> getSendCommand() {
        return this.reviewSendCommand;
    }

    /**
     * Get the subcommand {@code edit} as a trigger interface
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnReview> getEditCommand() {
        return this.reviewEditCommand;
    }

    /**
     * Get the subcommand {@code archive} as a trigger interface
     *
     * @return The subcommand as a trigger interface
     */
    public SlashCommand<OnPlotArchive> getArchiveCommand() {
        return this.reviewArchiveCommand;
    }

    /**
     * Get the sub command {@code edit} returning its event.
     *
     * @return The subcommand event interface
     * @see ReviewEvent
     */
    public ReviewEvent getEditEvent() {
        return this.reviewEditCommand;
    }

    /**
     * Get the sub command {@code send} returning its event.
     *
     * @return The subcommand event interface
     * @see ReviewEvent
     */
    public ReviewEvent getSendEvent() {
        return this.reviewSendCommand;
    }
}
