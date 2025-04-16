package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

import github.tintinkung.discordps.commands.events.SetupHelpEvent;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;


public final class SetupCommand extends CommandData {

    public static final String SETUP = "setup";

    // ::/setup webhook <name> <channel>
    public static final String WEBHOOK = "webhook";
    public static final String WEBHOOK_NAME = "name";
    public static final String WEBHOOK_CHANNEL = "channel";

    // ::/setup help
    public static final String HELP = "help";

    private final SetupWebhookCommand setupWebhookCommand;
    private final SetupHelpCommand setupHelpCommand;

    public SetupCommand(boolean defaultEnabled) {
        super(SetupCommand.SETUP, "Setup Discord Plot-System Configuration");
        this.addSubcommands(
            setupWebhookCommand = new SetupWebhookCommand(WEBHOOK, WEBHOOK_CHANNEL, WEBHOOK_NAME),
            setupHelpCommand = new SetupHelpCommand(HELP)
        );
        this.setDefaultEnabled(defaultEnabled);
    }

    /**
     * Get the sub command "webhook"
     * @return The subcommand as event handler
     */
    public SetupWebhookEvent getWebhookCommand() {
        return setupWebhookCommand;
    }

    /**
     * Get the sub command "channel"
     * @return The subcommand as event handler
     */
    public SetupHelpEvent getHelpCommand() {
        return setupHelpCommand;
    }
}
