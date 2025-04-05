package github.tintinkung.discordps.core.listeners;


import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.WebhookDeliver;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.tintinkung.discordps.core.WebhookManager;

@SuppressWarnings("unused")
public class DiscordSRVListener {
    private final DiscordPS plugin;

    public DiscordSRVListener(DiscordPS plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when DiscordSRV's JDA instance is ready and the plugin has fully loaded.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        DiscordPS.info("[DiscordPS] JDA Is Ready");

        DiscordSRV.getPlugin().getJda().addEventListener(new DiscordDisconnectListener());
        DiscordPS.api.subscribe(new PlotSubmitListener());

        try {
            WebhookManager.validateWebhook();
            WebhookManager.validateStatusTags();
        }
        catch (RuntimeException ex) {
            plugin.disablePlugin();
            DiscordPS.error(ex);
            DiscordPS.error("==============================================================");
            DiscordPS.error("Failed to initialize Webhook references. please check DiscordPlotSystem config file");
            DiscordPS.error("...disabling plugin");
            DiscordPS.error("==============================================================");
            return;
        }

        // WebhookDeliver.fetchSubmittedPlots();
        // WebhookDeliver.sendTestEmbed();
    }

    @Subscribe
    public void onDiscordChat(DiscordGuildMessageSentEvent event) {
        DiscordPS.info("[DiscordPS] DiscordGuildMessageSentEvent: " + event.getChannel().getId());

        // if (event.getChannel().getId().equals("PLACEHOLDER_CONFIG")) {
            // event.setCancelled(true); // Cancel this message from getting sent to global chat.
        //    DiscordPS.info("[DiscordPS] Webhook created on messageID: " + event.getMessage().getId());

            // Handle this on the main thread next tick.
            // plugin.sync().run(() -> plugin.submitMessageFromDiscord(event.getAuthor(), event.getMessage()));
        // }
    }
}
