package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import java.util.concurrent.CompletableFuture;

/**
 * Webhook provider, provide webhook reference and validation method.
 *
 * @see #getWebhook()
 * @see #validateWebhook(WebhookProvider...)
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
     * Validate this webhook integrity and output error/information to {@link asia.buildtheearth.asean.discord.plotsystem.Debug}.
     *
     * <p>The validation process is as follows:</p>
     * <ul>
     *     <li>Fetch the webhook guild if the webhook actually exist in the guild</li>
     *     <li>Check if this webhook is created by the bot itself (required by the system)</li>
     *     <li>Resolve status tags from config file (eg. name and color)</li>
     *     <li>Validate status tags if all of it exist by discord API</li>
     * </ul>
     *
     * @param others other webhook to validate with within the same request call
     * @return Future that complete when all validation action is completed
     */
    CompletableFuture<Void> validateWebhook(WebhookProvider... others);

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
