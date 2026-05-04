package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.commands.SlashCommand;
import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.ConfigPaths;
import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.DatabaseConnection;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.NotificationProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhookValidator;
import asia.buildtheearth.asean.discord.plotsystem.core.system.ShowcaseWebhook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReloadCommand.DESC_COMMAND;

public final class ReloadCommand extends CommandData implements SlashCommand<Interaction> {

    /** The reload command signature: {@code reload} */
    public static final String RELOAD = "reload";

    public ReloadCommand() {
        super(RELOAD, DiscordPS.getSystemLang().get(DESC_COMMAND, "Reload all configurations of this plugin"));
    }

    @Override
    public void trigger(@NotNull InteractionHook hook, @Nullable Interaction ignored) {
        try {
            this.reloadPlugin(hook);
        } catch (InvalidConfigurationException ex) {
            hook.sendMessage("Invalid changes detected.\n ```" + ex.getMessage() + "```").setEphemeral(true).queue();
        }
    }

    public void reloadPlugin(@NotNull InteractionHook hook) throws InvalidConfigurationException {

        YamlConfiguration webhookConfig = new YamlConfiguration();
        YamlConfiguration showcaseConfig = new YamlConfiguration();
        YamlConfiguration config = new YamlConfiguration();

        webhookConfig.loadFromString(DiscordPS.getPlugin().getWebhookConfig().saveToString());
        showcaseConfig.loadFromString(DiscordPS.getPlugin().getShowcaseConfig().saveToString());
        config.loadFromString(DiscordPS.getPlugin().getConfig().saveToString());

        // This reload every config file and memo metadata
        DiscordPS.getPlugin().loadConfig(false);
        DiscordPS.getMetadata().reload();

        hook.sendMessage("reloaded all configurations").setEphemeral(true).queue();

        YamlConfiguration current = DiscordPS.getPlugin().getConfig();

        // Check if database config changed
        if(checkDatabaseConfig(config, current)) {
            hook.sendMessage("Database config changed... reloading.").setEphemeral(true).queue();
            // Initialize database connection
            try {
                if(DatabaseConnection.InitializeDatabase()) {
                    hook.sendMessage("Successfully initialized database connection.").setEphemeral(true).queue();
                    DiscordPS.info("Successfully initialized database connection.");
                    DiscordPS.getDebugger().resolveError(Debug.Error.DATABASE_NOT_INITIALIZED);
                } else {
                    // returned false: handled error
                    hook.sendMessage("Could not initialize database connection due to a misconfigured config file.").setEphemeral(true).queue();
                    DiscordPS.error(Debug.Error.DATABASE_NOT_INITIALIZED, "Could not initialize database connection due to a misconfigured config file.");
                }
            }
            catch (Exception ex) { // Exception thrown: Unknown error occurred
                hook.sendMessage("Could not initialize database with unknown error.\n```" + ex.getMessage() + "```").setEphemeral(true).queue();
                DiscordPS.error(Debug.Error.DATABASE_NOT_INITIALIZED, ex.getMessage(), ex);
            }
        }

        // Check if notification config changed
        if(checkNotificationConfig(config, current)) {
            NotificationProvider.assignNotification();
            NotificationProvider.getOpt().ifPresentOrElse(channel -> {
                hook.sendMessage("Assigned new notification channel to <#" + channel.getId() + ">").setEphemeral(true).queue();
            }, () -> {
                hook.sendMessage("Invalid changes in system notification channel, `/setup help` for more info.").setEphemeral(true).queue();
            });
        }

        Consumer<PlotSystemWebhook> reloadTags = webhook -> {
            hook.sendMessage("Webhook tags change detected... reloading.").setEphemeral(true).queue();

            new PlotSystemWebhookValidator(webhook, current)
                .validate()
                .handle((ok, error)
                    -> (error == null)
                    ? "Successfully reloaded webhook tags."
                    : "Error occurred reloading webhook tags, " + error.getMessage())
                .thenApply(hook::sendMessage)
                .thenAccept(RestAction::queue);
        };

        // Check if webhook config changed
        if(checkWebhookConfig(webhookConfig, DiscordPS.getPlugin().getWebhookConfig())
        || checkWebhookConfig(showcaseConfig, DiscordPS.getPlugin().getShowcaseConfig())) {

            // Check if webhook identity changed
            PlotSystemWebhook webhook = DiscordPS.getPlugin().getWebhook();
            ShowcaseWebhook showcase = DiscordPS.getPlugin().getShowcase();

            boolean webhookIsEqual = (webhook != null)
                && checkWebhookProvider(webhookConfig, webhook.getProvider())
                && (showcase != null)
                && checkWebhookProvider(showcaseConfig, showcase.getProvider());

            // Guard if reference is equal
            if(!webhookIsEqual) {
                // reload our webhook
                hook.sendMessage("Webhook change detected... reloading.").setEphemeral(true).queue();

                DiscordPS.getPlugin().getListenerHook().loadWebhook(
                        () -> hook.sendMessage("Successfully reloaded webhooks.").setEphemeral(true).queue(),
                        () -> hook.sendMessage("Failed to load webhooks, check `/setup help` for more info.").setEphemeral(true).queue()
                );
            }
            else {
                // Tags may be changed even if webhook identity isn't changes
                if(checkTags(config, current)) {
                    reloadTags.accept(webhook);
                }
            }
        }
        else {
            // Tags may be changed even if webhook identity isn't changes
            if(checkTags(config, current)) {
                reloadTags.accept(DiscordPS.getPlugin().getWebhook());
            }
        }
    }

    public boolean checkNotMatch(@NotNull YamlConfiguration before,
                                 @NotNull YamlConfiguration after,
                                 @NotNull String @NotNull... configPaths) {
        BiFunction<YamlConfiguration, String, String> config = (yaml, path) -> yaml.getString(path, "").trim();
        Function<String, Boolean> check = (path) -> config.apply(before, path).equals(config.apply(after, path));

        return Stream.of(configPaths).map(check).anyMatch(equals -> !equals);
    }

    public boolean checkNotificationConfig(@NotNull YamlConfiguration before,
                                           @NotNull YamlConfiguration after) {
        return checkNotMatch(before, after,
            ConfigPaths.NOTIFICATION_CHANNEL
        );
    }

    public boolean checkDatabaseConfig(@NotNull YamlConfiguration before,
                                           @NotNull YamlConfiguration after) {
        return checkNotMatch(before, after,
            ConfigPaths.DATABASE_URL,
            ConfigPaths.DATABASE_NAME,
            ConfigPaths.DATABASE_USERNAME,
            ConfigPaths.DATABASE_PASSWORD,
            ConfigPaths.DATABASE_WEBHOOK_TABLE
        );
    }

    public boolean checkWebhookConfig(@NotNull YamlConfiguration before,
                                      @NotNull YamlConfiguration after) {
        return checkNotMatch(before, after,
            ConfigPaths.WEBHOOK_GUILD_ID,
            ConfigPaths.WEBHOOK_CHANNEL_ID,
            ConfigPaths.WEBHOOK_ID
        );
    }

    public boolean checkTags(@NotNull YamlConfiguration before,
                             @NotNull YamlConfiguration after) {
        return checkNotMatch(before, after,
            ConfigPaths.TAG_ON_GOING,
            ConfigPaths.TAG_FINISHED,
            ConfigPaths.TAG_REJECTED,
            ConfigPaths.TAG_APPROVED,
            ConfigPaths.TAG_ARCHIVED,
            ConfigPaths.TAG_ABANDONED,
            ConfigPaths.EMBED_COLOR_ON_GOING,
            ConfigPaths.EMBED_COLOR_FINISHED,
            ConfigPaths.EMBED_COLOR_REJECTED,
            ConfigPaths.EMBED_COLOR_APPROVED,
            ConfigPaths.EMBED_COLOR_ARCHIVED,
            ConfigPaths.EMBED_COLOR_ABANDONED
        );
    }

    public boolean checkWebhookProvider(@NotNull YamlConfiguration config,
                                        @NotNull WebhookProvider provider) {
        long channelID = provider.getChannelID();
        long guildID = provider.getGuildID();
        long webhookID = provider.getWebhookID();

        String guildConfig = config.getString(ConfigPaths.WEBHOOK_GUILD_ID, "");
        String channelConfig = config.getString(ConfigPaths.WEBHOOK_CHANNEL_ID, "");
        String webhookConfig = config.getString(ConfigPaths.WEBHOOK_ID, "");

        return Stream.of(
            guildConfig.trim().equals(Long.toUnsignedString(guildID)),
            channelConfig.trim().equals(Long.toUnsignedString(channelID)),
            webhookConfig.trim().equals(Long.toUnsignedString(webhookID))
        ).allMatch(equals -> equals);
    }
}
