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
}
