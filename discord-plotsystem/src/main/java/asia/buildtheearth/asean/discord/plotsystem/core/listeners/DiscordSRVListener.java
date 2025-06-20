package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.commands.ReviewCommand;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kyori.adventure.platform.bukkit.BukkitAudiences;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component;
import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.PluginListenerProvider;
import asia.buildtheearth.asean.discord.plotsystem.commands.SetupCommand;
import asia.buildtheearth.asean.discord.plotsystem.core.system.*;
import github.scarsz.discordsrv.dependencies.okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.REPOSITORY_RELEASE_URL;
import static asia.buildtheearth.asean.discord.plotsystem.Debug.Warning.UPDATE_CHECKING_FAILED;
import static asia.buildtheearth.asean.discord.plotsystem.DiscordPS.VERSION;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PluginMessage.*;

/**
 * Main listener and starting point of the plugin.
 *
 * <p>Listen for {@link DiscordReadyEvent} and subscribe the the JDA instance,
 * initializing plugin validation and mark the plugin as ready (or not if an error occurred)</p>
 *
 * @see #onDiscordReady(DiscordReadyEvent)
 * @see #subscribeAndValidateJDA()
 */
@SuppressWarnings("unused")
public class DiscordSRVListener extends PluginListenerProvider {

    private boolean subscribed = false;

    /**
     * Initialize a main listener for {@link DiscordSRV}
     *
     * @param plugin The main plugin instance
     */
    public DiscordSRVListener(DiscordPS plugin) {
        super(plugin);
    }

    @Override
    public boolean hasSubscribed() {
        return this.subscribed;
    }

    /**
     * Called when DiscordSRV's JDA instance is ready and the plugin has fully loaded.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        DiscordPS.info("DiscordSRV JDA Is Ready");

        // If this plugin already finish initializing before DiscordSRV JDA instance
        if(hasSubscribed()) return;

        // Subscribe to JDA
        subscribeAndValidateJDA();
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
        this.plugin.getJDA().addEventListener(this.newEventListener(this));

        // Initialize and Add our slash command data in
        newSlashCommandListener().register(
            this.plugin.getMainGuild().getId(),
            new SetupCommand(),
            new PlotCommand(),
            new ReviewCommand()
        );

        Checks.notNull(this.getPluginSlashCommand(), "Plugin Slash Command Provider");

        // Register slash command provider
        this.plugin.addSlashCommandProvider(this.getPluginSlashCommand());

        // Fetch it so the command appears on our server
        // This resolve to a PUT request to discord API
        // UPDATE_GUILD_COMMANDS = new Route(Method.PUT, "applications/{application_id}/guilds/{guild_id}/commands");
        this.plugin.updateSlashCommands();


        // Initialize main webhook
        try {
            ForumWebhookImpl forumWebhook = new ForumWebhookImpl(
                this.plugin.getJDA(),
                this.plugin.getWebhookConfig(),
                this.plugin.getConfig()
            );
            this.plugin.initWebhook(new PlotSystemWebhook(this.plugin, forumWebhook));
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
                this.plugin.getJDA(),
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

            validation.orTimeout(60, TimeUnit.SECONDS).whenComplete((ok, error) -> {

                if(error != null) {
                    DiscordPS.error(Debug.Error.WEBHOOK_VALIDATION_UNKNOWN_EXCEPTION, error);
                    this.onPluginNotReady();
                    return;
                }

                if(this.plugin.isReady()) this.onPluginReady();
                else this.onPluginNotReady();
            });
        }
        else this.onPluginNotReady();
    }

    /**
     * Subscribe the plugin with plot-system listener,
     * then print blocking message to notify the plugin status.
     *
     * @see DiscordPS#subscribe(Object)
     * @see #notifyPluginReady(String, Throwable)
     */
    private void onPluginReady() {
        DiscordPS.getPlugin().subscribe(this.newPlotSystemListener(this.plugin.getWebhook()));

        Notification.notify(PLUGIN_STARTED);

        this.checkForUpdate()
            .orTimeout(10, TimeUnit.SECONDS)
            .whenComplete(this::notifyPluginReady);
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
     * Checks for available plugin updates and logs the result to the console.
     *
     * <p>Checks {@value Constants#REPOSITORY_RELEASE_URL} to determine the latest version.
     * If the current version {@link DiscordPS#VERSION} is outdated, it logs a warning.
     *
     * @return A nullable string where a non-null value indicates a new version is available.
     * @throws RuntimeException if the HTTP client fails to fetch update data.
     */
    private @NotNull CompletableFuture<@Nullable String> checkForUpdate() {
        final CompletableFuture<@Nullable String> action = new CompletableFuture<>();

        try {
            DiscordPS.getPlugin()
                .getJDA()
                .getHttpClient()
                .newCall(new Request.Builder().url(REPOSITORY_RELEASE_URL).build())
                .enqueue(new UpdateChecker(action));
        }
        catch (RuntimeException ex) { action.completeExceptionally(ex); }

        return action;
    }

    /**
     * Response callback to handle update checker request.
     *
     * @param action Completable action that will be resolved on the callback of the update checker.
     */
    private record UpdateChecker(CompletableFuture<String> action) implements Callback {
        private void fail(String failure) {
            DiscordPS.warning(UPDATE_CHECKING_FAILED, failure);
            this.action.completeExceptionally(new RuntimeException(failure));
        }

        @Override
        public void onFailure(Call call, IOException exception) {
            DiscordPS.error("Update checker returned an IOException", exception);
            this.fail("Update checker HTTP call execution returned IOException.");
            call.cancel();
        }

        /**
         * Expecting the URL to be redirected to the latest tag released,
         * where the last path endpoint contain the version string.
         *
         * @param call The initial http client's call
         * @param response The http response of said client
         */
        @Override
        public void onResponse(@NotNull Call call, Response response) {
            if(response == null) {
                this.fail("Got no response from Update checker URL; cannot determine latest plugin version.");
                call.cancel();
                return;
            }
            else if(response.priorResponse() == null) this.fail("Update checker response not found; cannot determine latest plugin version.");
            else if(!response.priorResponse().isRedirect()) this.fail("Update checker URL is not redirected; cannot determine latest plugin version.");
            else if (!response.isSuccessful()) this.fail("Update checker HTTP call was unsuccessful.");


            if(action.isCompletedExceptionally() || action.isCancelled()) {
                if(response.priorResponse() != null)
                    response.priorResponse().close();
                response.close();
                call.cancel();
                return;
            }

            String latestVersion = response.request().url().pathSegments().getLast().trim()
                    // Trim out potential un-wanted string
                    .replace("/", "")
                    .replace("v", "")
                    .replace("V", "");

            if (latestVersion.equals(VERSION))
                this.action.complete(null); // Plugin is up-to-date
            else this.action.complete(latestVersion); // Newer version available

            response.close();
        }
    }
}
