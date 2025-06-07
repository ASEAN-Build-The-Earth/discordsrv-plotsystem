package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.api.events.NotificationType;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import org.jetbrains.annotations.NotNull;

public enum PlotNotification implements MessageLang {
    ON_CREATED("plot-notification.on-plot-created"),
    ON_SUBMITTED("plot-notification.on-plot-submitted"),
    ON_REVIEWED("plot-notification.on-plot-reviewed"),
    ON_APPROVED("plot-notification.on-plot-approved"),
    ON_REJECTED("plot-notification.on-plot-rejected"),
    ON_UNDO_REVIEW("plot-notification.on-undo-review"),
    ON_UNDO_SUBMIT("plot-notification.on-undo-submit"),
    ON_SHOWCASED("plot-notification.on-plot-showcased"),
    ON_ABANDONED("plot-notification.on-plot-abandoned"),
    ON_INACTIVITY("plot-notification.on-plot-inactivity");

    private final String path;

    PlotNotification(String path) {
        this.path = path;
    }

    @Override
    public @NotNull String getKey() {
        return this.path;
    }

    @NotNull
    public static PlotNotification from(@NotNull NotificationType type) {
        return valueOf(type.name());
    }
}
