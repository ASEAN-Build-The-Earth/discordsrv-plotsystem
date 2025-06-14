package asia.buildtheearth.asean.discord.plotsystem.api.test;

import asia.buildtheearth.asean.discord.plotsystem.api.events.NotificationType;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotNotificationEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

public abstract class PlotNotificationTest extends PlotEventTest {
    private final @NotNull NotificationType expected;
    private final boolean cancelled;

    public PlotNotificationTest(NotificationType type, boolean cancelled) {
        super(PlotNotificationEvent.class, event -> event.onPlotNotification(type, cancelled));
        this.expected = type;
        this.cancelled = cancelled;
    }

    @Override
    public <T extends PlotEvent>
    void retrieveEvent(@NotNull T retrieved) {
        testNotification(retrieved, this.expected, this.cancelled);
    }
}
