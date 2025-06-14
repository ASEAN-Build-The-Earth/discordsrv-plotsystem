package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public @Nullable PlotSystemListener getPlotSystemListener() {
        return plotSystemListener;
    }

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
