package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhookValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class MockDiscordSRVListener extends DiscordSRVListener {
    private PlotSystemListener plotSystemListener;

    public MockDiscordSRVListener(DiscordPS plugin) {
        super(plugin);
    }

    @Override
    public @NotNull PlotSystemListener newPlotSystemListener(@NotNull PlotSystemWebhook webhook) {
        if(getPlotSystemListener() != null) throw new IllegalArgumentException("[Internal] Trying to initialize plugin listener that already exist");
        return this.plotSystemListener = new MockPlotSystemListener(webhook);
    }

    /**
     * Plot-System webhook validation is NOT tested.
     *
     * @return A delegated validator that returns instant successful future on validation.
     */
    @Override
    protected @NotNull PlotSystemWebhookValidator newWebhookValidator() {
        return new PlotSystemWebhookValidator(this.plugin.getWebhook(),  this.plugin.getConfig()) {
            @Override
            public CompletableFuture<Void> validate(WebhookProvider... others) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    /**
     * Test plot-system listener.
     *
     * @return A mocked listener.
     * @see MockPlotSystemListener
     */
    @Override
    public @Nullable PlotSystemListener getPlotSystemListener() {
        return this.plotSystemListener;
    }

    /**
     * Mock for {@linkplain  PlotSystemListener}
     *
     * <p>Retain all functionality but the task queuing is delegated to be run instantly.</p>
     */
    public static class MockPlotSystemListener extends PlotSystemListener {

        public MockPlotSystemListener(PlotSystemWebhook webhook) {
            super(webhook);
        }

        @Override
        protected void queueTask(@NotNull Runnable task) {
            task.run();
        }
    }
}
