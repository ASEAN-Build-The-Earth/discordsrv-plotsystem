package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.ShowcaseWebhook;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockDiscordPS extends DiscordPS implements MockDiscordSRVBridge {
    private PlotSystemWebhook mockWebhook;
    private ShowcaseWebhook mockShowcase;

    public static final MockForumWebhook PLOT_SYSTEM = new MockForumWebhook() {
        @Override public WebhookProvider getProvider() { return MockWebhook.PLOT_SYSTEM; }
    };

    public static final MockForumWebhook SHOWCASE = new MockForumWebhook() {
        @Override public WebhookProvider getProvider() { return MockWebhook.SHOWCASE; }
    };

    @Override
    public void onEnable() {
        // Initialize plugin reference
        plugin = this;

        // Create configs
        createConfig();

        // Initialize plugin
        Thread initThread = createInitThread();
        initThread.start();

        // Thread have to be joined to prevent potential interruption from junit
        try { initThread.join(); }
        catch (InterruptedException ex) { throw new RuntimeException(ex); }
    }

    @Override
    public void initWebhook(PlotSystemWebhook webhook) {
        this.mockWebhook = new PlotSystemWebhook(this, PLOT_SYSTEM);
        super.initWebhook(webhook);
    }

    @Override
    public void initShowcase(ShowcaseWebhook webhook) {
        this.mockShowcase = new ShowcaseWebhook(SHOWCASE);
        super.initShowcase(webhook);
    }

    public @NotNull DiscordSRVListener initListenerHook() {
        if(isDiscordSrvHookEnabled())
            throw new IllegalArgumentException("Trying to re-assign plugin's MockDiscordSRVListener reference, ignoring.");
        return new MockDiscordSRVListener(this);
    }

    @Override
    public @Nullable PlotSystemWebhook getWebhook() {
        return this.mockWebhook;
    }

    @Override
    public @Nullable ShowcaseWebhook getShowcase() {
        return this.mockShowcase;
    }

    @Override
    public @Nullable Member getAsDiscordMember(@NotNull OfflinePlayer player) {
        return null;
    }
}
