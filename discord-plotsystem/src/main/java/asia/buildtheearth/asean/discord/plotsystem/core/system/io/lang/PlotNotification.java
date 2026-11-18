package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.api.events.NotificationType;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import org.jetbrains.annotations.NotNull;

public enum PlotNotification implements MessageLang {
    ON_CREATED("on-plot-created"),
    ON_SUBMITTED("on-plot-submitted"),
    ON_REVIEWED("on-plot-reviewed"),
    ON_APPROVED("on-plot-approved"),
    ON_REJECTED("on-plot-rejected"),
    ON_UNDO_REVIEW("on-undo-review"),
    ON_UNDO_SUBMIT("on-undo-submit"),
    ON_SHOWCASED("on-plot-showcased"),
    ON_ABANDONED("on-plot-abandoned"),
    ON_INACTIVITY("on-plot-inactivity");

    private final String path;

    PlotNotification(String path) {
        this.path = path;
    }

    @Override
    public @NotNull String getKey() {
        return "plot-notification." + this.path;
    }

    public String getPath() {
        return this.path;
    }

    @NotNull
    public static PlotNotification from(@NotNull NotificationType type) {
        return valueOf(type.name());
    }

    @NotNull
    public static PlotNotification from(@NotNull AvailableComponent.NotificationComponent type) {
        return valueOf(type.name());
    }

    @NotNull
    public AvailableComponent.NotificationComponent toComponent() {
        return valueOf(AvailableComponent.NotificationComponent.class, this.name());
    }
}
