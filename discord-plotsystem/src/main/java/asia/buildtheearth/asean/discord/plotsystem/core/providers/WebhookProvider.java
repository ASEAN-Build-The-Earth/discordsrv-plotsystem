package asia.buildtheearth.asean.discord.plotsystem.core.providers;

/**
 * Webhook provider, provide webhook reference and validation method.
 *
 * @see #getWebhook()
 */
public interface WebhookProvider {

    /**
     * Get the webhook application ID,
     * this is the user ID of said webhook.
     *
     * @return The ID as unsigned long snowflake
     */
    long getWebhookID();

    /**
     * Get the channel this webhook is hooked to.
     *
     * @return Channel ID as unsigned long snowflake
     */
    long getChannelID();

    /**
     * Get the guild this webhook is hooked to.
     *
     * @return Guild ID as unsigned long snowflake
     */
    long getGuildID();

    /**
     * Get the webhook reference, this is used to resolve for actual webhook entity.
     *
     * @return The webhook reference which contain resolve method to retrieve webhook entity.
     */
    github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook.WebhookReference getWebhookReference();

    /**
     * Get the webhook instance
     *
     * @return The webhook entity
     */
    github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook getWebhook();

    /**
     * Get JDA instance
     *
     * @return The instance as implementation interface
     */
    github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl getJDA();
}
