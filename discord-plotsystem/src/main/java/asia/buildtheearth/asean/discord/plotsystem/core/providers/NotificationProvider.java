package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import asia.buildtheearth.asean.discord.plotsystem.ConfigPaths;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.Config;
import static asia.buildtheearth.asean.discord.plotsystem.Debug.Warning.NOTIFICATION_CHANNEL_NOT_SET;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.LABEL_ERROR;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.SYSTEM_ERROR;

public abstract class NotificationProvider {

    private record Notification<T extends MessageChannel>(T channel) {}

    protected record Metadata(Optional<String> content,
                              Config plugin,
                              Config errors,
                              String errorTitle,
                              String errorLabel) {}

    private static Notification<? extends MessageChannel> notification;

    protected static final Metadata METADATA;

    private static long getAndValidateChannelID(String channelID) throws IllegalArgumentException {
        Checks.notNull(channelID, "Plot-System notification channel");

        if(StringUtils.isBlank(channelID))
            throw new IllegalArgumentException("Plot-System notifications is not set.");

        Checks.isSnowflake(channelID, "Plot-System notification channel");

        return Long.parseUnsignedLong(channelID);
    }

    private static @Nullable String getNotificationContent() {
        String content = DiscordPS.getPlugin().getConfig().getString(ConfigPaths.NOTIFICATION_CONTENT, null);

        if(content != null && StringUtils.isBlank(content)) return null;
        else return content;
    }

    protected static @NotNull Config parseConfig(String key, @NotNull Config defaultValue) {
        String config = DiscordPS.getPlugin().getConfig().getString(key, defaultValue.name());

        if(config.equalsIgnoreCase(Config.WITH_CONTENT.name())) return Config.WITH_CONTENT;
        else if(config.equalsIgnoreCase((Config.ENABLED.name()))) return Config.ENABLED;
        else if(config.equalsIgnoreCase((Config.DISABLED.name()))) return Config.DISABLED;
        else return defaultValue;
    }

    static {
        String channelID = DiscordPS.getPlugin().getConfig().getString(ConfigPaths.NOTIFICATION_CHANNEL);

        METADATA = new Metadata(
            Optional.ofNullable(getNotificationContent()),
            parseConfig(ConfigPaths.NOTIFICATION_PLUGIN, Config.ENABLED),
            parseConfig(ConfigPaths.NOTIFICATION_ERRORS, Config.ENABLED),
            DiscordPS.getSystemLang().get(SYSTEM_ERROR),
            DiscordPS.getSystemLang().get(LABEL_ERROR)
        );

        try {
            // Parse and verify channel ID from config
            long channelIDLong = getAndValidateChannelID(channelID);

            // Set the notification channel
            notification = new Notification<>(DiscordPS.getPlugin().getJDA().getTextChannelById(channelIDLong));
        }
        catch (NumberFormatException ex) {
            DiscordPS.warning(
                    NOTIFICATION_CHANNEL_NOT_SET,
                    "Notification channel is set but "
                            + "failed to parse in configuration for "
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
