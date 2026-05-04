package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
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
     * Initialize Webhook provider with a given configuration,
     * may output {@link Debug.Error#WEBHOOK_MISSING_CONFIGURATION} on failures.
     *
     * @param config YAML Configuration file of this webhook
     * @throws IllegalArgumentException If the given configuration is invalid for webhook ID and its channel ID
     */
    public AbstractWebhookProvider(@NotNull YamlConfiguration config) throws IllegalArgumentException {
        this.config = config;

        String guildConfig = prepareConfig(ConfigPaths.WEBHOOK_GUILD_ID);
        String channelConfig = prepareConfig(ConfigPaths.WEBHOOK_CHANNEL_ID);
        String webhookConfig = prepareConfig(ConfigPaths.WEBHOOK_ID);

        this.guildID = parse(guildConfig);
        this.channelID = parse(channelConfig);
        this.webhookID = parse(webhookConfig);
    }

    private @Nullable String prepareConfig(String configPath) {
        String configValue = this.config.getString(configPath, "");

        if(StringUtils.isBlank(configValue)) {
            DiscordPS.error(Debug.Error.WEBHOOK_MISSING_CONFIGURATION,
            "Webhook configuration for '" + configPath + "' is not set.");
            return null;
        }

        return configValue;
    }

    /**
     * Parse and validate for webhook snowflake config value.
     *
     * @param value The yaml config path to get
     * @return The parsed config path as discord snowflake long
     * @throws IllegalArgumentException If the given config path is not valid
     */
    private long parse(String value) throws IllegalArgumentException {
        try {
            Checks.isSnowflake(value);
            return Long.parseUnsignedLong(value);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "Cannot parse webhook configuration for '"
                + value
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