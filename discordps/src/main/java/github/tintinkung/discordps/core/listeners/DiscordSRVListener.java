package github.tintinkung.discordps.core.listeners;


import github.scarsz.discordsrv.DiscordSRV;
import github.tintinkung.discordps.Debug;
import github.tintinkung.discordps.DiscordPS;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.tintinkung.discordps.core.providers.PluginProvider;
import github.tintinkung.discordps.commands.SetupCommand;
import github.tintinkung.discordps.core.system.ForumWebhook;
import github.tintinkung.discordps.core.system.PlotSystemWebhook;

@SuppressWarnings("unused")
final public class DiscordSRVListener extends PluginProvider {
    private DiscordEventListener eventListener;
    private DiscordCommandListener pluginSlashCommand;

    public DiscordSRVListener(DiscordPS plugin) {
        super(plugin);
    }

    public boolean isReady() {
        return eventListener != null;
    }

    public boolean hasCommandsRegistered() {
        return pluginSlashCommand != null;
    }

    public DiscordEventListener getEventListener() {
        return eventListener;
    }

    public DiscordCommandListener getPluginSlashCommand() {
        return pluginSlashCommand;
    }

    /**
     * Called when DiscordSRV's JDA instance is ready and the plugin has fully loaded.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        DiscordPS.info("[DiscordPS] JDA Is Ready");

        // If this plugin finish initializing before DiscordSRV JDA instance
        if(!isReady()) {
            subscribeAndValidateJDA();
        }
        // WebhookDeliver.sendTestEmbed();
    }

    @Subscribe
    public void onDiscordChat(DiscordGuildMessageSentEvent event) {
        DiscordPS.debug("[DiscordPS] DiscordGuildMessageSentEvent: " + event.getChannel().getId());

        // if (event.getChannel().getId().equals("PLACEHOLDER_CONFIG")) {
            // event.setCancelled(true); // Cancel this message from getting sent to global chat.
        //    DiscordPS.info("[DiscordPS] Webhook created on messageID: " + event.getMessage().getId());

            // Handle this on the main thread next tick.
            // plugin.sync().run(() -> plugin.submitMessageFromDiscord(event.getAuthor(), event.getMessage()));
        // }
    }

    public void subscribeAndValidateJDA() {
        // Subscribe to event listeners
        DiscordSRV.getPlugin().getJda().addEventListener(eventListener = new DiscordEventListener(this));
        DiscordPS.getInstance().subscribe(new PlotSubmitListener());

        // Initialize and Add our slash command data in
        pluginSlashCommand = new DiscordCommandListener(this.plugin);
        pluginSlashCommand.register(new SetupCommand(
                DiscordPS.getPlugin().isDebuggingEnabled()),
                DiscordSRV.getPlugin().getMainGuild().getId()
        );

        // Register slash command provider
        DiscordSRV.api.addSlashCommandProvider(this.pluginSlashCommand);

        // Fetch it so the command appears on our server
        // This resolve to a PUT request to discord API
        // UPDATE_GUILD_COMMANDS = new Route(Method.PUT, "applications/{application_id}/guilds/{guild_id}/commands");
        DiscordSRV.api.updateSlashCommands();


        // Validate for webhook reference
        try {
            ForumWebhook forumWebhook = new ForumWebhook(
                    DiscordSRV.getPlugin().getJda(),
                    DiscordPS.getPlugin().getWebhookConfig(),
                    DiscordPS.getPlugin().getConfig()
            );
            DiscordPS.getPlugin().initWebhook(new PlotSystemWebhook(forumWebhook));

            // Everything should be ready by now
            if(DiscordPS.getPlugin().getWebhook() != null) {
                DiscordPS.getPlugin().getWebhook().getProvider().validateWebhook();
            }
        }
        catch (RuntimeException ex) {
            DiscordPS.error(
                Debug.Error.WEBHOOK_NOT_CONFIGURED,
                "Failed to initialize webhook reference, maybe it is un-configured?", ex
            );
        }

        if(!DiscordPS.getPlugin().isReady()) {
            DiscordPS.warning("==============================================================");
            DiscordPS.warning("Discord-PlotSystem is not Ready!");
            DiscordPS.warning("There are unresolved configuration or runtime errors blocking the application process.");
            DiscordPS.warning("The plugin will remain running for debugging.");
            DiscordPS.warning("Use the command '/setup help' in your discord server to see the configuration checklists.");
            DiscordPS.warning("All Occurred Errors:");
            DiscordPS.getDebugger().allThrownErrors().forEach(entry -> {
                DiscordPS.warning("[ERROR] " + entry.getKey() + ": " + entry.getValue());
            });
            DiscordPS.warning("==============================================================");
        }
        else {
            DiscordPS.info("Discord-PlotSystem is fully configured! The application is ready.");
        }

        if(DiscordPS.getPlugin().isReady())
            DiscordPS.getPlugin().getWebhook().fetchLatestPlot();
    }

}
