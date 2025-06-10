package asia.buildtheearth.asean.discord.plotsystem.api.test;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemImpl;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockDiscordPlotSystemPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class PlotCreateTest extends PlotEventTest {

    public PlotCreateTest(Consumer<DiscordPlotSystemImpl.PlotEventAction> action) {
        super(PlotCreateEvent.class, action);
    }

    @Override
    public <T extends PlotEvent> void retrieveEvent(@NotNull T retrieved) {
        testCreateEvent(retrieved, DiscordPlotSystemAPI.getDataProvider().getData(MockDiscordPlotSystemPlugin.MOCK_PLOT_ID));
    }
}
