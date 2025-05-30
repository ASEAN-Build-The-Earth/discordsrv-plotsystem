package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import asia.buildtheearth.asean.discord.plotsystem.commands.events.SetupWebhookEvent;

/**
 * Payload for {@link SetupWebhookEvent SetupWebhookEvent}
 * Interactions. Created as final class for abstraction visibility.
 */
public final class OnSetupWebhook extends Interaction {
    /**
     * The webhook display name to create as
     */
    public final String webhookName;

    /**
     * The channel ID where to webhook will be linked to
     */
    public final long webhookChannel;

    /**
     * The output configuration file that will be saved as
     */
    public final String outputFile;

    /**
     * @param eventID The event ID snowflake which the payload originate from.
     * @param webhookName The given webhook channel name by user.
     * @param webhookChannel The given webhook channel ID by user.
     */
    public OnSetupWebhook(long userID, long eventID, String webhookName, long webhookChannel, String outputFile) {
        super(userID, eventID);
        this.webhookName = webhookName;
        this.webhookChannel = webhookChannel;
        this.outputFile = outputFile;
    }
}