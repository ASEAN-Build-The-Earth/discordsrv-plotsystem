package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.commands.interactions.Interaction;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.commands.providers.SlashCommand;


public final class SetupCommand extends CommandData {

    public static final String SETUP = "setup";

    /**
     * {@code /setup webhook <name> <channel>}
     */
    public static final String WEBHOOK = "webhook";
    public static final String WEBHOOK_YML = "webhook.yml";

    /**
     * {@code /setup showcase <name> <channel>}
     */
    public static final String SHOWCASE = "showcase";
    public static final String SHOWCASE_YML = "showcase.yml";

    /**
     * {@code /setup help}
     */
    public static final String HELP = "help";

    public static final String WEBHOOK_NAME = "name";
    public static final String WEBHOOK_CHANNEL = "channel";

    private final SetupWebhookCommand setupWebhookCommand;
    private final SetupShowcaseCommand setupShowcaseCommand;
    private final SetupHelpCommand setupHelpCommand;

    public SetupCommand(boolean defaultEnabled) {
        super(SetupCommand.SETUP, "Setup Discord Plot-System Configuration");
        this.addSubcommands(
            setupWebhookCommand = new SetupWebhookCommand(WEBHOOK, WEBHOOK_YML, WEBHOOK_CHANNEL, WEBHOOK_NAME),
            setupShowcaseCommand = new SetupShowcaseCommand(SHOWCASE, SHOWCASE_YML, WEBHOOK_CHANNEL, WEBHOOK_NAME),
            setupHelpCommand = new SetupHelpCommand(HELP)
        );
        this.setDefaultEnabled(defaultEnabled);
    }

    /**
     * Get the command trigger for sub command "help"
     *
     * @return The slash command trigger
     */
    public SlashCommand<Interaction> getHelpCommand() {
        return setupHelpCommand;
    }

    /**
     * Get the command trigger for sub command "webhook"
     *
     * @return The slash command trigger
     */
    public SlashCommand<OnSetupWebhook> getWebhookCommand() {
        return setupWebhookCommand;
    }


    /**
     * Get the command trigger for sub command "showcase"
     *
     * @return The slash command trigger
     */
    public SlashCommand<OnSetupWebhook> getShowcaseCommand() {
        return setupShowcaseCommand;
    }

    /**
     * Get the sub command "webhook"
     *
     * @return The subcommand as event handler
     */
    public SetupWebhookEvent getWebhookEvent() {
        return setupWebhookCommand;
    }
}
