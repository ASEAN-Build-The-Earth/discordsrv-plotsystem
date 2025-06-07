package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;

/**
 * YAML Language path constants
 */
public class LangPaths {
    /**
     * System slash command language configurations
     */
    private static final String SLASH_COMMANDS = "slash-commands.";

    /**
     * Prefix that will be used to archive a plot
     */
    public static final SystemLang ARCHIVED_PREFIX = () -> "archive-prefix";

    /**
     * Setup command parent language description
     *
     * @see asia.buildtheearth.asean.discord.plotsystem.commands.SetupCommand
     */
    public static final SystemLang SETUP_COMMAND = () -> SLASH_COMMANDS + "setup-command.description";

    /**
     * Setup-showcase command, this is used as standalone description field
     * because all its functionality is inherited from {@link SetupWebhookCommand}
     */
    public static final SystemLang SETUP_SHOWCASE = () ->  SLASH_COMMANDS + "setup-showcase.description";

    /**
     * Review command parent's description message.
     */
    public static final String REVIEW_COMMAND = SLASH_COMMANDS + "review-command.";

    /**
     * Plot command
     *
     * @see PlotCommand
     * @see asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand
     */
    static final String PLOT_CONTROL_COMMAND = SLASH_COMMANDS + "plot-control-command.";

    /**
     * @see CommandInteractions
     */
    static final String COMMAND_INTERACTIONS = SLASH_COMMANDS + "interactions.";

    /**
     * @see SetupWebhookCommand
     */
    static final String SETUP_WEBHOOK = SLASH_COMMANDS + "setup-webhook.";

    /**
     * @see SetupHelpCommand
     */
    static final String SETUP_HELP = SLASH_COMMANDS + "setup-help.";

    /**
     * @see PlotFetchCommand
     */
    static final String PLOT_FETCH = SLASH_COMMANDS + "plot-control-fetch.";

    /**
     * @see PlotDeleteCommand
     */
    static final String PLOT_DELETE = SLASH_COMMANDS + "plot-control-delete.";

    /**
     * @see PlotArchiveCommand
     */
    static final String PLOT_ARCHIVE = SLASH_COMMANDS + "plot-control-archive.";

    /**
     * @see PlotShowcaseCommand
     */
    static final String PLOT_SHOWCASE = SLASH_COMMANDS + "plot-control-showcase.";

    /**
     * @see ReviewEditCommand
     */
    static final String REVIEW_EDIT = SLASH_COMMANDS + "review-edit.";

    /**
     * @see Notification
     */
    static final String SYSTEM_NOTIFICATION = "system-notification.";
}
