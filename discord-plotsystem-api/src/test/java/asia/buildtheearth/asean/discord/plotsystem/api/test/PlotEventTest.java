package asia.buildtheearth.asean.discord.plotsystem.api.test;

import asia.buildtheearth.asean.discord.plotsystem.api.*;
import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockDiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockDiscordPlotSystemPlugin;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockEventListener;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class PlotEventTest {
    protected final Class<? extends PlotEvent> type;
    protected final Consumer<DiscordPlotSystemImpl.PlotEventAction> action;

    public PlotEventTest(Class<? extends PlotEvent> type, Consumer<DiscordPlotSystemImpl.PlotEventAction> action) {
        this.type = type;
        this.action = action;
    }

    @Test
    @DisplayName("Event is called")
    public final void callEvent() {
        Assertions.assertNotNull(DiscordPlotSystemAPI.getDataProvider(),
                "API Data provider is Null"
        );

        DiscordPlotSystemImpl.getOpt(MockDiscordPlotSystemPlugin.MOCK_PLOT_ID).ifPresentOrElse(this.action,
                () -> Assertions.fail("API instance to call events is not present")
        );
    }

    @Test
    @DisplayName("Retrieved expected event")
    public final void retrieveEvent() {

        MockEventListener listener = MockDiscordPlotSystemAPI.getInstance().getEventListener();

        Assertions.assertTrue(listener.received(this.type), "Event is not received");

        this.retrieveEvent(listener.popEvent(this.type));
    }

    public <T extends PlotEvent> void retrieveEvent(@NotNull T event) {
        Assertions.assertEquals(event.getPlotID(), MockDiscordPlotSystemPlugin.MOCK_PLOT_ID, "Received unexpected plot ID");
    }

    public static <T extends PlotEvent> void testNotification(@NotNull T retrieved,
                                                              NotificationType expected,
                                                              boolean cancelled) {
        testPlotEvent(PlotNotificationEvent.class, retrieved, event -> {
            Assertions.assertNotNull(expected, "Expected type is null");
            Assertions.assertEquals(event.getPlotID(), MockDiscordPlotSystemPlugin.MOCK_PLOT_ID, "Received unexpected plot ID");
            Assertions.assertEquals(event.getType(), expected, "Received unexpected plot ID");
            Assertions.assertEquals(event.isCancelled(), cancelled, "Received unexpected plot ID");
        });
    }

    @SuppressWarnings("deprecation")
    public static <T extends PlotEvent> void testInactivityNotice(@NotNull T retrieved, LocalDate expected) {
        testPlotEvent(InactivityNoticeEvent.class, retrieved, event -> {
            Assertions.assertNotNull(expected, "Expected timestamp is null");
            Assertions.assertEquals(event.getPlotID(), MockDiscordPlotSystemPlugin.MOCK_PLOT_ID, "Received unexpected plot ID");
            Assertions.assertEquals(event.getType(), NotificationType.ON_INACTIVITY, "Received unexpected plot ID");
            Assertions.assertEquals(event.getTimestamp(), expected, "Received unexpected plot ID");
        });
    }

    public static <T extends PlotEvent> void testAbandonEvent(@NotNull T retrieved,
                                                              AbandonType expected) {
        testPlotEvent(PlotAbandonedEvent.class, retrieved, event -> {
            Assertions.assertNotNull(expected, "Expected type is null");
            Assertions.assertEquals(event.getPlotID(), MockDiscordPlotSystemPlugin.MOCK_PLOT_ID, "Received unexpected plot ID");
            Assertions.assertEquals(event.getType(), expected, "Received unexpected plot ID");
        });
    }

    public static <T extends PlotEvent> void testCreateEvent(@NotNull T retrieved,
                                                             PlotCreateData expected) {
        testPlotEvent(PlotCreateEvent.class, retrieved, event -> {
            Assertions.assertNotNull(expected, "Expected plot data is null");
            Assertions.assertNotNull(event, "Event is Null");
            Assertions.assertEquals(MockDiscordPlotSystemPlugin.MOCK_PLOT_ID, event.getPlotID(), "Retrieved unexpected Plot ID");
            Assertions.assertEquals(expected, event.getData(), "Retrieved unexpected plot data");
        });
    }

    public static <T extends PlotEvent>
    void testPlotEvent(@NotNull Class<T> expected,
                       @NotNull PlotEvent retrieved,
                       @NotNull Consumer<T> onExpected) {
        Assertions.assertNotNull(retrieved, "Expected event returned null");
        if (expected.isAssignableFrom(retrieved.getClass())) onExpected.accept(expected.cast(retrieved));
        else Assertions.fail("Received event if not of expected type " + expected.getSimpleName());
    }

    public static <T extends PlotEvent, V>
    void testScopedEvent(@NotNull Class<V> scope,
                         @NotNull Function<V, String> scopeFunction,
                         @NotNull T retrieved,
                         @NotNull String expected) {
        if (scope.isInstance(retrieved)) {
            Assertions.assertNotNull(expected, "Expected type is null");
            Assertions.assertEquals(scopeFunction.apply(scope.cast(retrieved)), expected, "Received unexpected owner of a scoped event");
        } else Assertions.fail(retrieved.getClass().getSimpleName()
                + " does not implement expected scope of "
                + scope.getSimpleName()
        );
    }
}
