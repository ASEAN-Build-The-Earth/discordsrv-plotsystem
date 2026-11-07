package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import asia.buildtheearth.asean.discord.plotsystem.ConfigPaths;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementations for {@link WebhookProvider}
 */
public abstract class AbstractWebhookProvider implements WebhookProvider {
    protected final YamlConfiguration config;

    protected final long guildID;

    protected final long channelID;

    protected final long webhookID;

    /**
     * Initialize Webhook provider with a given configuration.
     *
     * <p>Callers decide how to surface failures (fatal for the main webhook,
     * warning for optional showcase). This constructor only validates and throws.</p>
     *
     * @param config YAML Configuration file of this webhook
     * @throws IllegalArgumentException If the given configuration is missing or invalid for webhook identity fields
     */
    public AbstractWebhookProvider(@NotNull YamlConfiguration config) throws IllegalArgumentException {
        this.config = config;

        String guildConfig = config.getString(ConfigPaths.WEBHOOK_GUILD_ID, "");
        String channelConfig = config.getString(ConfigPaths.WEBHOOK_CHANNEL_ID, "");
        String webhookConfig = config.getString(ConfigPaths.WEBHOOK_ID, "");

        StringBuilder missing = new StringBuilder();
        appendIfBlank(missing, ConfigPaths.WEBHOOK_GUILD_ID, guildConfig);
        appendIfBlank(missing, ConfigPaths.WEBHOOK_CHANNEL_ID, channelConfig);
        appendIfBlank(missing, ConfigPaths.WEBHOOK_ID, webhookConfig);
        if(!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "Webhook configuration for '" + missing + "' is not set.");
        }

        this.guildID = parse(guildConfig, ConfigPaths.WEBHOOK_GUILD_ID);
        this.channelID = parse(channelConfig, ConfigPaths.WEBHOOK_CHANNEL_ID);
        this.webhookID = parse(webhookConfig, ConfigPaths.WEBHOOK_ID);
    }

    /**
     * Whether the given YAML contains non-blank values for the required webhook identity paths.
     *
     * @param config webhook or showcase YAML
     * @return {@code true} if guild, channel, and webhook IDs are all present
     */
    public static boolean hasRequiredWebhookSettings(@NotNull YamlConfiguration config) {
        return !StringUtils.isBlank(config.getString(ConfigPaths.WEBHOOK_GUILD_ID, ""))
            && !StringUtils.isBlank(config.getString(ConfigPaths.WEBHOOK_CHANNEL_ID, ""))
            && !StringUtils.isBlank(config.getString(ConfigPaths.WEBHOOK_ID, ""));
    }

    private static void appendIfBlank(@NotNull StringBuilder missing, @NotNull String path, @Nullable String value) {
        if(!StringUtils.isBlank(value)) return;
        if(!missing.isEmpty()) missing.append(", ");
        missing.append(path);
    }

    /**
     * Parse and validate for webhook snowflake config value.
     *
     * @param value The yaml config value to parse
     * @param configPath The yaml path (for error messages)
     * @return The parsed config path as discord snowflake long
     * @throws IllegalArgumentException If the given config path is not valid
     */
    private long parse(String value, String configPath) throws IllegalArgumentException {
        try {
            Checks.isSnowflake(value);
            return Long.parseUnsignedLong(value);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Cannot parse webhook configuration for '"
                + configPath
                + "' (" + ex.getMessage() + ")"
            );
        }
    }

    /**
     * Retrieve for webhook entity be resolving {@link #getWebhookReference()}
     *
     * @return The webhook entity resolved with {@link RestAction#complete()}
     */
    protected Webhook retrieveWebhook() {
        return getWebhookReference().resolve().complete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getWebhookID() {
        return this.webhookID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getChannelID() {
        return this.channelID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getGuildID() {
        return this.guildID;
    }

    /**
     * {@inheritDoc}
     */
    @Contract(" -> new")
    public abstract @NotNull Webhook.WebhookReference getWebhookReference();

    /**
     * {@inheritDoc}
     */
    public abstract Webhook getWebhook();

    /**
     * {@inheritDoc}
     */
    public abstract github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl getJDA();
}