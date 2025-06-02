package asia.buildtheearth.asean.discord.plotsystem.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.SetupWebhookEvent;
import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnSetupWebhook;
import asia.buildtheearth.asean.discord.commands.SlashCommand;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.LangPaths;

/**
 * Setup command data, parent command of all setup-related commands.
 *
 * <p>Subcommands include the following:</p>
 * <ul>
 *     <li>{@link SetupWebhookEvent} {@code /setup webhook <name> <channel>}<br/>
 *     Interactively setup main webhook configuration.</li>
 *     <li>{@link SetupWebhookEvent} {@code /setup showcase <name> <channel>}<br/>
 *     Interactively setup showcase webhook configuration, using the same events as the webhook command.</li>
 *     <li>{@link SlashCommand<Interaction>} {@code /setup help}<br/>
 *     Display plugin checklist messages with no interaction.</li>
 * </ul>
 */
public final class SetupCommand extends CommandData {

    /** The setup command signature: {@code setup} */
    public static final String SETUP = "setup";

    /** Webhook command signature: {@code /setup webhook <name> <channel>} */
    public static final String WEBHOOK = "webhook";

    /** The main webhook configuration output filename {@code webhook.yml}*/
    public static final String WEBHOOK_YML = "webhook.yml";

    /** Showcase command signature: {@code /setup showcase <name> <channel>} */
    public static final String SHOWCASE = "showcase";

    /** The showcase webhook configuration output filename {@code showcase.yml}*/
    public static final String SHOWCASE_YML = "showcase.yml";

    /** Help command signature: {@code /setup help} */
    public static final String HELP = "help";

    /** Webhook command's parameter: {@code name} */
    public static final String WEBHOOK_NAME = "name";

    /** Webhook command's parameter: {@code channel} */
    public static final String WEBHOOK_CHANNEL = "channel";

    private final SetupWebhookCommand setupWebhookCommand;
    private final SetupShowcaseCommand setupShowcaseCommand;
    private final SetupHelpCommand setupHelpCommand;

    /**
     * Initialize setup command data creating all of its subcommands.
     */
    public SetupCommand() {
        super(SetupCommand.SETUP, DiscordPS.getSystemLang().get(LangPaths.SETUP_COMMAND));
        this.addSubcommands(
            setupWebhookCommand = new SetupWebhookCommand(WEBHOOK, WEBHOOK_YML, WEBHOOK_CHANNEL, WEBHOOK_NAME),
            setupShowcaseCommand = new SetupShowcaseCommand(SHOWCASE, SHOWCASE_YML, WEBHOOK_CHANNEL, WEBHOOK_NAME),
            setupHelpCommand = new SetupHelpCommand(HELP)
        );
    }

    /**
     * Get the command trigger for sub command {@code help}
     *
     * @return The slash command trigger interface
     */
    public SlashCommand<Interaction> getHelpCommand() {
        return setupHelpCommand;
    }

    /**
     * Get the command trigger for sub command {@code webhook}
     *
     * @return The slash command trigger interface
     */
    public SlashCommand<OnSetupWebhook> getWebhookCommand() {
        return setupWebhookCommand;
    }


    /**
     * Get the command trigger for sub command {@code showcase}
     *
     * @return The slash command trigger interface
     */
    public SlashCommand<OnSetupWebhook> getShowcaseCommand() {
        return setupShowcaseCommand;
    }

    /**
     * Get the sub command {@code webhook} returning its event.
     *
     * @return The subcommand as event interface
     */
    public SetupWebhookEvent getWebhookEvent() {
        return setupWebhookCommand;
    }
}
