package github.tintinkung.discordps.core.listeners;


import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.WebhookDeliver;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;

@SuppressWarnings("unused")
public class DiscordSRVListener {
    private final DiscordPS plugin;

    public DiscordSRVListener(DiscordPS plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        DiscordPS.info("[DiscordPS] JDA Is Ready");

        WebhookDeliver.sendTestEmbed();
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
