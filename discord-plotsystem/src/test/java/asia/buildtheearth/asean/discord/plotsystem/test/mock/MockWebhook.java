package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook;
import github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl;

import java.util.concurrent.CompletableFuture;

public abstract class MockWebhook {
    public static final PlotSystemWebhook PLOT_SYSTEM = new PlotSystemWebhook();
    public static final ShowcaseWebhook SHOWCASE = new ShowcaseWebhook();

    public static class PlotSystemWebhook extends AbstractWebhookProvider {

        @Override
        public long getWebhookID() {
            return MockSnowflake.WEBHOOK_ID.getMockID();
        }

        @Override
        public long getChannelID() {
            return MockSnowflake.WEBHOOK_CHANNEL_ID.getMockID();
        }

        @Override
        public long getGuildID() {
            return MockSnowflake.MAIN_GUILD.getMockID();
        }
    }

    public static class ShowcaseWebhook extends AbstractWebhookProvider {

        @Override
        public long getWebhookID() {
            return MockSnowflake.SHOWCASE_ID.getMockID();
        }

        @Override
        public long getChannelID() {
            return MockSnowflake.SHOWCASE_CHANNEL_ID.getMockID();
        }

        @Override
        public long getGuildID() {
            return MockSnowflake.MAIN_GUILD.getMockID();
        }
    }

    private abstract static class AbstractWebhookProvider implements WebhookProvider {
        public JDAImpl getJDA() {
            return (JDAImpl) DiscordPS.getPlugin().getJDA();
        }

        /**
         * Assume the webhook validation is successfully validated since unit test does not cover API calls.
         *
         * @param others other webhook to validate with within the same request call
         * @return Instant completed future
         */
        @Override
        public CompletableFuture<Void> validateWebhook(WebhookProvider... others) {
            return CompletableFuture.completedFuture(null);
        }

        /**
         * We only use this to resolve the instance for {@link #getWebhook()} that is unused.
         *
         * @return {@code null}
         */
        @Override
        public Webhook.WebhookReference getWebhookReference() {
            return null;
        }

        /**
         * We don't use this at all since we use raw API call from webhook ID .
         *
         * @return {@code null}
         */
        @Override
        public Webhook getWebhook() {
            return null;
        }
    }
}
