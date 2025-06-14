package asia.buildtheearth.asean.discord.plotsystem.api.test;

import asia.buildtheearth.asean.discord.plotsystem.api.events.AbandonType;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotAbandonedEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

public abstract class PlotAbandonTest extends PlotEventTest {
    private final @NotNull AbandonType expected;

    public PlotAbandonTest(@NotNull AbandonType type) {
        super(PlotAbandonedEvent.class, event -> event.onPlotAbandon(type));
        this.expected = type;
    }

    @Override
    public <T extends PlotEvent>
    void retrieveEvent(@NotNull T retrieved) {
        testAbandonEvent(retrieved, this.expected);
    }
}
