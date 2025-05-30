package asia.buildtheearth.asean.discord.plotsystem.core.system;

sealed abstract class AbstractShowcaseWebhook permits ShowcaseWebhook {

    /**
     * The webhook instance, provide all functionality in webhook management.
     */
    protected final ForumWebhook webhook;

    /**
     * Initialize webhook instance
     *
     * @param webhook The forum webhook manager instance
     */
    protected AbstractShowcaseWebhook(ForumWebhook webhook) {
        this.webhook = webhook;
    }
}
