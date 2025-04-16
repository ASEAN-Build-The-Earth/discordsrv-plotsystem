package github.tintinkung.discordps.core.providers;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.ConfigPaths;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public abstract class WebhookProvider implements WebhookProviderImpl {
    protected final YamlConfiguration config;

    protected final long guildID;

    protected final long channelID;


    protected final long webhookID;

    /**
     * Initialize Webhook provider with a given configuration
     * @param config Yaml Configuration file of this webhook
     * @throws IllegalArgumentException If the given configuration is invalid for webhook ID and its channel ID
     */
    public WebhookProvider(@NotNull YamlConfiguration config) throws IllegalArgumentException {
        this.config = config;

        this.guildID = getAndValidateConfig(ConfigPaths.WEBHOOK_GUILD_ID);
        this.channelID = getAndValidateConfig(ConfigPaths.WEBHOOK_CHANNEL_ID);
        this.webhookID = getAndValidateConfig(ConfigPaths.WEBHOOK_ID);
    }

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

    protected Webhook retrieveWebhook() {
        return getWebhookReference().resolve().complete();
    }

    public final long getWebhookID() {
        return webhookID;
    }

    public final long getChannelID() {
        return channelID;
    }

    @Contract(" -> new")
    public abstract @NotNull Webhook.WebhookReference getWebhookReference();


    public abstract void validateWebhook();

    public abstract Webhook getWebhook();

    public abstract github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl getJDA();
}
