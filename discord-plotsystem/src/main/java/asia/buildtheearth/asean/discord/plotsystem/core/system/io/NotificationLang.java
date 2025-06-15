package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

import org.jetbrains.annotations.NotNull;


public interface NotificationLang extends SystemLang {

    /**
     * Get the accent color of this notification
     *
     * @return The {@link java.awt.Color} object
     */
    @NotNull
    java.awt.Color getAccentColor();

    /**
     * System notification message can be enabled or disabled by the config file.
     *
     * @return The configuration path, the default value is {@link #getKey()}
     */
    @NotNull
    default String getConfig() {
        return getKey();
    }
}
