package github.tintinkung.discordps.core.listeners;


import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kyori.adventure.platform.bukkit.BukkitAudiences;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextComponent;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.NamedTextColor;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.format.TextDecoration;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.Debug;
import github.tintinkung.discordps.DiscordPS;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.tintinkung.discordps.commands.PlotCommand;
import github.tintinkung.discordps.core.providers.PluginListenerProvider;
import github.tintinkung.discordps.commands.SetupCommand;
import github.tintinkung.discordps.core.system.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component.text;
import static github.tintinkung.discordps.core.system.io.lang.Notification.PluginMessage.PLUGIN_STARTED;

@SuppressWarnings("unused")
final public class DiscordSRVListener extends PluginListenerProvider {

    private boolean subscribed = false;

    public DiscordSRVListener(DiscordPS plugin) {
        super(plugin);
    }

    @Override
    public boolean hasSubscribed() {
        return subscribed;
    }

    /**
     * Called when DiscordSRV's JDA instance is ready and the plugin has fully loaded.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        DiscordPS.info("DiscordSRV JDA Is Ready");

        // If this plugin finish initializing before DiscordSRV JDA instance
        if(!hasSubscribed()) {
            subscribeAndValidateJDA();
        }
    }

    /**
     * Entry point for all plugin functionality,
     * connect the plugin to DiscordSRV's JDA instance
     * then initialize and validate for all required plugin data.
     *
     * <p>Note: Must be called after DiscordSRV ready event</p>
     */
    public void subscribeAndValidateJDA() {
        this.subscribed = true;

        // Subscribe to event listeners
        DiscordSRV.getPlugin().getJda().addEventListener(this.newEventListener(this));

        // Initialize and Add our slash command data in
        newSlashCommandListener().register(DiscordSRV.getPlugin().getMainGuild().getId(),
            new SetupCommand(DiscordPS.getPlugin().isDebuggingEnabled()),
            new PlotCommand(DiscordPS.getPlugin().isDebuggingEnabled())
        );

        Checks.notNull(this.getPluginSlashCommand(), "Plugin Slash Command Provider");

        // Register slash command provider
        DiscordSRV.api.addSlashCommandProvider(this.getPluginSlashCommand());

        // Fetch it so the command appears on our server
        // This resolve to a PUT request to discord API
        // UPDATE_GUILD_COMMANDS = new Route(Method.PUT, "applications/{application_id}/guilds/{guild_id}/commands");
        DiscordSRV.api.updateSlashCommands();


        // Initialize main webhook
        try {
            ForumWebhookImpl forumWebhook = new ForumWebhookImpl(
                DiscordSRV.getPlugin().getJda(),
                this.plugin.getWebhookConfig(),
                this.plugin.getConfig()
            );
            this.plugin.initWebhook(new PlotSystemWebhook(forumWebhook));
        }
        catch (RuntimeException ex) {
            DiscordPS.error(
                Debug.Error.WEBHOOK_NOT_CONFIGURED,
                "Failed to initialize and validate webhook reference, maybe it is un-configured?", ex
            );
        }

        // Initialize showcase webhook
        try {
            ForumWebhookImpl showcaseWebhook = new ForumWebhookImpl(
                DiscordSRV.getPlugin().getJda(),
                this.plugin.getShowcaseConfig(),
                this.plugin.getConfig()
            );
            this.plugin.initShowcase(new ShowcaseWebhook(showcaseWebhook));
        }
        catch (RuntimeException ex) {
            DiscordPS.warning(Debug.Warning.SHOWCASE_WEBHOOK_NOT_CONFIGURED);
        }

        // Webhook is initialized, validate its functionalities
        if(this.plugin.getWebhook() != null) {
            CompletableFuture<Void> validation = this.plugin.getShowcase() != null
                ? this.plugin
                    .getWebhook()
                    .getProvider()
                    .validateWebhook(this.plugin.getShowcase().getProvider())
                : this.plugin.getWebhook().getProvider().validateWebhook();

            validation.orTimeout(5, TimeUnit.SECONDS).thenRun(() -> {
                if(this.plugin.isReady()) {
                    DiscordPS.getInstance().subscribe(this.newPlotSystemListener(this.plugin.getWebhook()));

                    DiscordPS.info("Discord-PlotSystem is fully configured! The application is ready.");

                    Notification.notify(PLUGIN_STARTED);
                }
                else this.onPluginNotReady();
            });
        }
        else this.onPluginNotReady();
    }

    /**
     * Print blocking debugging message on server console
     *
     * @see #createDebuggingMessage()
     */
    private void onPluginNotReady() {
        try(BukkitAudiences bukkit = BukkitAudiences.create(this.plugin)) {
            Component debuggingMessage = this.createDebuggingMessage();
            bukkit.console().sendMessage(debuggingMessage);
        }
    }

    /**
     * Huge debugging message when the plugin is not ready.
     *
     * @return Formatted message with colors.
     */
    private @NotNull Component createDebuggingMessage() {
        TextComponent LINE = text("!!! ", NamedTextColor.RED);

        // Brief title
        Component notReadyMessage = text("[DiscordPlotSystem]", NamedTextColor.AQUA).appendSpace()
                .append(text("is NOT ready!", NamedTextColor.RED, TextDecoration.BOLD)).appendNewline()
                .append(text("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", NamedTextColor.RED)).appendNewline()
                .append(LINE).append(text("There are unresolved configuration or runtime errors blocking the application process.", NamedTextColor.YELLOW)).appendNewline()
                .append(LINE).append(text("The plugin will remain running for debugging.", NamedTextColor.GREEN)).appendNewline()
                .append(LINE).append(text("Use the command '/setup help' in your discord server to see the configuration checklists.", NamedTextColor.GREEN)).appendNewline()
                .append(LINE).append(text("All Occurred Errors:", NamedTextColor.YELLOW)).appendNewline();

        // Important error: this make the application fails
        for(Map.Entry<Debug.Error, String> entry : DiscordPS.getDebugger().allThrownErrors()) {
            notReadyMessage = notReadyMessage.append(LINE)
                .append(text("[ERROR]", NamedTextColor.RED, TextDecoration.BOLD))
                .appendSpace()
                .append(text(entry.getKey().name(), NamedTextColor.GOLD))
                .append(text(":", NamedTextColor.GRAY))
                .appendNewline()
                .append(LINE).append(text("        └─ ", NamedTextColor.DARK_GRAY))
                .append(text(entry.getValue(), NamedTextColor.DARK_RED))
                .appendNewline();
        }

        // Occurred warnings
        for(Map.Entry<Debug.Warning, String> entry : DiscordPS.getDebugger().allThrownWarnings()) {
            notReadyMessage = notReadyMessage.append(LINE)
                .append(text("[WARNING]", NamedTextColor.YELLOW))
                .appendSpace()
                .append(text(entry.getKey().name(), NamedTextColor.GOLD))
                .append(text(":", NamedTextColor.GRAY))
                .appendNewline()
                .append(LINE).append(text("          └─ ", NamedTextColor.DARK_GRAY))
                .append(text(entry.getValue(), NamedTextColor.DARK_GREEN))
                .appendNewline();
        }
        notReadyMessage = notReadyMessage.append(text("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", NamedTextColor.RED));
        return notReadyMessage;
    }
}
