package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;

import github.tintinkung.discordps.core.providers.NotificationProvider;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class Notification extends NotificationProvider {

    /**
     * Send message embed(s) to the notification channel.
     * @param embed The {@link MessageEmbed}
     * @param other Other embeds
     */
    public static void sendMessageEmbeds(@NotNull MessageEmbed embed, @NotNull MessageEmbed... other) {
        getOpt().ifPresent((channel -> channel.sendMessageEmbeds(embed, other).queue()));
    }

    /**
     * Send message content and embed(s) to the notification channel.
     * @param content Message string content.
     * @param embeds The {@link MessageEmbed}
     */
    public static void sendMessageEmbeds(String content, @NotNull MessageEmbed... embeds) {
        getOpt().ifPresent((channel -> channel.sendMessage(content).setEmbeds(embeds).queue()));
    }

    /**
     * Send message to the notification channel.
     * @param message The message as String of content
     */
    public static void sendMessage(CharSequence message) {
        getOpt().ifPresent((channel -> channel.sendMessage(message).queue()));
    }

    public static void sendErrorEmbed(String description) {
        sendMessageEmbeds(new EmbedBuilder()
            .setTitle(":red_circle: Discord Plot-System Error")
            .setDescription(description)
            .setColor(Color.RED)
            .build()
        );
    }

    public static void sendErrorEmbed(String description, String error) {
        sendMessageEmbeds(new EmbedBuilder()
            .setTitle(":red_circle: Discord Plot-System Error")
            .setDescription(description)
            .addField("Error", "```" + error + "```", false)
            .setColor(Color.RED)
            .build()
        );
    }
}
