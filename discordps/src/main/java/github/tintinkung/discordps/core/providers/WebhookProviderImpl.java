package github.tintinkung.discordps.core.providers;

public interface WebhookProviderImpl {

    long getWebhookID();

    long getChannelID();

    void validateWebhook();

    github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook.WebhookReference getWebhookReference();

    github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook getWebhook();

    github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl getJDA();
}
