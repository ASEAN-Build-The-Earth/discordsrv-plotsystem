package github.tintinkung.discordps.commands.interactions;

import github.tintinkung.discordps.commands.events.SetupWebhookEvent;

/**
 * Payload for {@link SetupWebhookEvent SetupWebhookEvent}
 * Interactions. Created as final class for abstraction visibility.
 */
public final class OnSetupWebhook implements InteractionPayload {
    public final long userID;
    public final long eventID;
    public final String webhookName;
    public final long webhookChannel;

    /**
     * @param eventID The event ID snowflake which the payload originate from.
     * @param webhookName The given webhook channel name by user.
     * @param webhookChannel The given webhook channel ID by user.
     */
    public OnSetupWebhook(long userID, long eventID, String webhookName, long webhookChannel) {
        this.userID = userID;
        this.eventID = eventID;
        this.webhookName = webhookName;
        this.webhookChannel = webhookChannel;
    }
    
    @Override
    public String toString() {
        return "OnSetupWebhook{" +
                "webhookName='" + webhookName + '\'' +
                ", webhookChannel=" + webhookChannel +
                '}';
    }
}