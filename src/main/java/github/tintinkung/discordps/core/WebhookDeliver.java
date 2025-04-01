package github.tintinkung.discordps.core;

import github.tintinkung.discordps.DiscordPS;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Webhook;
import github.scarsz.discordsrv.util.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.Date;
import java.util.UUID;

public class WebhookDeliver {

    public WebhookDeliver() {

    }

    public static void sendTestEmbed() {
        TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById("PLACEHOLDER_CONFIG");

        Message message = DiscordUtil.sendMessageBlocking(channel, "Plot ID: 1");


        Webhook webhook = WebhookUtil.createWebhook(channel, "test-webhook");



        MessageEmbed embed = new EmbedBuilder()
                .setDescription(new Date() + "\n" + "```" + "Test Message Ha Ha Ha" + "```")
                .setColor(Color.YELLOW)
                .setFooter("Some Footer he he")
                .build();

        OfflinePlayer test = Bukkit.getOfflinePlayer(UUID.fromString("PLACEHOLDER_PLAYER"));

        // channel.createWebhook("uhh-webhook").

        WebhookUtil.deliverMessage(channel, test, "test", "Test Message Webhook", embed);

        SchedulerUtil.runTaskLater(DiscordPS.getPlugin(), () -> {
            MessageEmbed editedEmbed = new EmbedBuilder()
                    .setDescription(new Date() + "\n" + "```" + "Edited Embed Ha Ha Ha" + "```")
                    .setColor(Color.BLUE)
                    .setFooter("Some Footer he he")
                    .build();
            DiscordPS.info("Editing webhook URL: " + webhook.getUrl());
            // WebhookUtil.editMessage(channel, message.getId(), "Edited Webhook", editedEmbed);

//            Request.Builder requestBuilder = (new Request.Builder()).url(webhookUrl).header("User-Agent", "DiscordSRV/" + DiscordSRV.getPlugin().getDescription().getVersion());
//            if (editMessageId == null) {
//                requestBuilder.post(bodyBuilder.build());
//            } else {
//                requestBuilder.patch(bodyBuilder.build());
//            }
//
//            OkHttpClient httpClient = DiscordSRV.getPlugin().getJda().getHttpClient();
//            Response response = httpClient.newCall(requestBuilder.build()).execute();


        }, 1000);

        // WebhookUtil.deliverMessage(channel, "test-webhook", );
    }
}
