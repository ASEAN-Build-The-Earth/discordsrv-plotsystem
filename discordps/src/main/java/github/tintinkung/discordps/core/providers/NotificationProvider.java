package github.tintinkung.discordps.core.providers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.ConfigPaths;
import github.tintinkung.discordps.DiscordPS;

import static github.tintinkung.discordps.Debug.Warning.NOTIFICATION_CHANNEL_NOT_SET;

public abstract class NotificationProvider {

    private record Notification<T extends MessageChannel>(T channel) {}

    private static Notification<? extends MessageChannel> notification;

    private static long getAndValidateChannelID(String channelID) throws IllegalArgumentException {
        Checks.notNull(channelID, "Plot-System notification channel");

        if(StringUtils.isBlank(channelID))
            throw new IllegalArgumentException("Plot-System notifications is not set.");

        Checks.isSnowflake(channelID, "Plot-System notification channel");

        return Long.parseUnsignedLong(channelID);
    }

    static {
        String channelID = DiscordPS.getPlugin().getConfig().getString(ConfigPaths.NOTIFICATION_CHANNEL);

        try {
            // Parse and verify channel ID from config
            long channelIDLong = getAndValidateChannelID(channelID);

            // Set the notification channel
            notification = new Notification<>(DiscordSRV.getPlugin().getJda().getTextChannelById(channelIDLong));
        }
        catch (NumberFormatException ex) {
            DiscordPS.warning(
                    NOTIFICATION_CHANNEL_NOT_SET,
                    "Notification channel is set but "
                            + "failed to parse webhook configuration for "
                            + channelID + " (" + ex.getMessage() + ")"
            );
        }
        catch (IllegalArgumentException ex) {
            DiscordPS.warning(NOTIFICATION_CHANNEL_NOT_SET, "Notification channel disabled (" + ex.getMessage() + ")");
        }
    }

    /**
     * Get the notification channel.
     * @return The message channel as an {@link java.util.Optional Optional} because it may be disabled.
     */
    public static java.util.Optional<? extends MessageChannel> getOpt() {
        return java.util.Optional.ofNullable(notification.channel());
    }
}
