package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;

import github.tintinkung.discordps.core.providers.NotificationProvider;
import org.jetbrains.annotations.NotNull;

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
     * Send message to the notification channel.
     * @param message The message as String of content
     */
    public static void sendMessage(CharSequence message) {
        getOpt().ifPresent((channel -> channel.sendMessage(message).queue()));
    }
}
