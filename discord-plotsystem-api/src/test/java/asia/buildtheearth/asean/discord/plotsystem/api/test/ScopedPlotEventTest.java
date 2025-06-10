package asia.buildtheearth.asean.discord.plotsystem.api.test;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemImpl;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.ScopedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public abstract class ScopedPlotEventTest extends PlotEventTest {

    protected static final String MOCK_USER = "MockUser";

    public ScopedPlotEventTest(Class<? extends PlotEvent> type, BiConsumer<DiscordPlotSystemImpl.PlotEventAction, String> action) {
        super(type, event -> action.accept(event, MOCK_USER));
    }

    public ScopedPlotEventTest(Class<? extends PlotEvent> type, BiConsumer<DiscordPlotSystemImpl.PlotEventAction, String> action, String test) {
        super(type, event -> action.accept(event, test));
    }

    protected <T extends PlotEvent> void test(@NotNull T retrieved) {
        testScopedEvent(ScopedEvent.class, ScopedEvent::getOwner, retrieved, MOCK_USER);
    }

    @Override
    public final <T extends PlotEvent> void retrieveEvent(@NotNull T retrieved) {
        super.retrieveEvent(retrieved);
        this.test(retrieved);
    }
}
