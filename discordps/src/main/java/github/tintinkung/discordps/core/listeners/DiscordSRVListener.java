package github.tintinkung.discordps.core.listeners;


import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ErrorResponseException;
import github.tintinkung.discordps.DiscordPS;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.tintinkung.discordps.core.WebhookManager;

@SuppressWarnings("unused")
public class DiscordSRVListener {
    private final DiscordPS plugin;
    private DiscordDisconnectListener onDiscordDisconnect;

    public DiscordSRVListener(DiscordPS plugin) {
        this.plugin = plugin;
    }

    public boolean isReady() {
        return onDiscordDisconnect != null;
    }

    public DiscordDisconnectListener getOnDiscordDisconnect() {
        return onDiscordDisconnect;
    }

    /**
     * Called when DiscordSRV's JDA instance is ready and the plugin has fully loaded.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        DiscordPS.info("[DiscordPS] JDA Is Ready");

        try {
            WebhookManager.validateWebhook();
            WebhookManager.validateStatusTags();
        }
        catch (RuntimeException ex) {
            if(ex instanceof ErrorResponseException) {
                DiscordPS.error("Likely to be Response exception code: " + ((ErrorResponseException) ex).getErrorCode());
            }
            DiscordPS.error(ex);

            plugin.disablePlugin("Failed to initialize Webhook references. please check DiscordPlotSystem config file");

            return;
        }

        DiscordSRV.getPlugin().getJda().addEventListener(onDiscordDisconnect = new DiscordDisconnectListener());
        DiscordPS.getInstance().subscribe(new PlotSubmitListener());

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
