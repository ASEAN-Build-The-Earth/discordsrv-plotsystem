package github.tintinkung.discordps.core.providers;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.ConfigPaths;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implementations for {@link WebhookProvider}
 */
public abstract class AbstractWebhookProvider implements WebhookProvider {
    protected final YamlConfiguration config;

    protected final long guildID;

    protected final long channelID;

    protected final long webhookID;

    /**
     * Initialize Webhook provider with a given configuration
     * @param config Yaml Configuration file of this webhook
     * @throws IllegalArgumentException If the given configuration is invalid for webhook ID and its channel ID
     */
    public AbstractWebhookProvider(@NotNull YamlConfiguration config) throws IllegalArgumentException {
        this.config = config;

        this.guildID = getAndValidateConfig(ConfigPaths.WEBHOOK_GUILD_ID);
        this.channelID = getAndValidateConfig(ConfigPaths.WEBHOOK_CHANNEL_ID);
        this.webhookID = getAndValidateConfig(ConfigPaths.WEBHOOK_ID);
    }

    /**
     * Parse and validate for webhook snowflake config value.
     *
     * @param configPath The yaml config path to get
     * @return The parsed config path as discord snowflake long
     * @throws IllegalArgumentException If the given config path is not valid
     */
    private long getAndValidateConfig(String configPath) throws IllegalArgumentException {
        String configValue = this.config.getString(configPath);

        Checks.notNull(configValue, "Webhook configuration for " + configPath);

        if(StringUtils.isBlank(configValue))
            throw new IllegalArgumentException("Webhook configuration for " + configPath + " is not set.");

        Checks.isSnowflake(configValue, "Webhook configuration for " + configPath);

        try { return Long.parseUnsignedLong(configValue); }
        catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Cannot parse webhook configuration for "
                + configPath + ": " + configValue
                + " (" + ex.getMessage() + ")"
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
        return webhookID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getChannelID() {
        return channelID;
    }

    /**
     * {@inheritDoc}
     */
    @Contract(" -> new")
    public abstract @NotNull Webhook.WebhookReference getWebhookReference();

    /**
     * {@inheritDoc}
     *
     * @return
     */
    public abstract CompletableFuture<Void> validateWebhook(WebhookProvider... others);

    /**
     * {@inheritDoc}
     */
    public abstract Webhook getWebhook();

    /**
     * {@inheritDoc}
     */
    public abstract github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl getJDA();
}