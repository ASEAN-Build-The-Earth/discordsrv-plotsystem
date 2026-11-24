package asia.buildtheearth.asean.discord.plotsystem.test;

import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordCommandListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordEventListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.NotificationProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.test.mock.MockDiscordSRV;
import asia.buildtheearth.asean.discord.plotsystem.test.mock.MockPluginServer;
import com.alpsbte.plotsystem.api.event.PlotCreatedEvent;
import com.alpsbte.plotsystem.api.event.PlotFeedbackUpdatedEvent;
import com.alpsbte.plotsystem.api.event.PlotReviewUndoneEvent;
import com.alpsbte.plotsystem.api.event.PlotSubmissionUndoneEvent;
import com.alpsbte.plotsystem.api.event.PlotSubmittedEvent;
import com.alpsbte.plotsystem.api.event.PlotRejectedEvent;
import com.alpsbte.plotsystem.api.model.PlotDifficulty;
import com.alpsbte.plotsystem.api.model.PlotSnapshot;
import com.alpsbte.plotsystem.api.model.PlotStatus;
import com.alpsbte.plotsystem.api.model.PlotType;
import com.alpsbte.plotsystem.api.model.ReviewSnapshot;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.ServerMock;

import java.time.LocalDate;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

@DisplayName("Plugin")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class PluginTest implements MockPluginServer {

    protected static ServerMock server;
    protected static DiscordPS plugin;
    protected static github.scarsz.discordsrv.DiscordSRV discordSRV;

    @Override
    public void onServerStarted(ServerMock server, DiscordSRV discordSRV, DiscordPS plugin) {
        PluginTest.server = server;
        PluginTest.discordSRV = discordSRV;
        PluginTest.plugin = plugin;
    }

    @DisplayName("Initialization")
    @Nested @Order(1)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class Initialization {
        @Test @Order(1)
        @DisplayName("Plugin and server is not null")
        public void expectedSetup(TestInfo info) {
            Assertions.assertAll(info.getDisplayName(),
                () -> Assertions.assertNotNull(server),
                () -> Assertions.assertNotNull(plugin)
            );
        }

        @Test @Order(2)
        @DisplayName("Plugin's instance references returns equal")
        public void instanceIsCorrect(TestInfo info) {
            Assertions.assertAll(info.getDisplayName(),
                () -> Assertions.assertEquals(plugin, DiscordPS.getPlugin()),
                () -> Assertions.assertThrows(IllegalArgumentException.class, DiscordSRV::getPlugin),
                () -> Assertions.assertEquals(discordSRV, MockDiscordSRV.getPlugin())
            );
        }

        @Test @Order(3)
        @DisplayName("Expect plugin enabled but not ready yet")
        public void expectNotReady(TestInfo info) {
            Assertions.assertAll(info.getDisplayName(),
                () -> Assertions.assertTrue(server.getPluginManager().isPluginEnabled(plugin)),
                () -> Assertions.assertTrue(server.getPluginManager().isPluginEnabled(discordSRV)),
                () -> Assertions.assertFalse(plugin.isReady()),
                () -> Assertions.assertFalse(plugin.discordIsReady()),
                () -> Assertions.assertNull(plugin.getJDA()),
                () -> Assertions.assertNotNull(plugin.getListenerHook())
            );
        }

        @Test @Order(4)
        @DisplayName("SQLite Database Connection initialized")
        public void databaseWillNotInitialize() {
            Assertions.assertFalse(DiscordPS.getDebugger().hasError(Debug.Error.DATABASE_NOT_INITIALIZED));
        }

        @Test @Order(5)
        @DisplayName("Call DiscordReadyEvent then the plugin will be ready")
        public void onReady() {
            // Assuming that event is successfully called
            DiscordReadyEvent event = Assertions.assertDoesNotThrow(() ->
                MockDiscordSRV.api.callEvent(new DiscordReadyEvent())
            );
            // Then the plugin should be ready
            Assumptions.assumingThat(event != null, () -> Assertions.assertAll(
                "Plugin Should be ready",
                () -> Assertions.assertTrue(plugin.getListenerHook().hasSubscribed()),
                () -> Assertions.assertTrue(plugin.isReady()),
                () -> Assertions.assertTrue(plugin.discordIsReady())
            ));
        }

        @Test @Order(6)
        @DisplayName("No error occurred during DiscordReadyEvent")
        public void expectNoFatalError() {
            Assertions.assertFalse(DiscordPS.getDebugger().hasAnyError(), "Expected no thrown error from the Debugger");

            // IF a warning were to happen, we expect it to be notification provider's static initializer
            // JUnit may initialize NotificationProvider earlier e.g. by earlier test suite
            // The warning will be resolved by next test order
            Assumptions.assumingThat(DiscordPS.getDebugger().hasAnyWarning(), () -> {
                Assertions.assertTrue(DiscordPS.getDebugger().hasWarning(Debug.Warning.NOTIFICATION_CHANNEL_NOT_SET));
                Assertions.assertEquals(1, DiscordPS.getDebugger().allThrownWarnings().size());
            });
        }

        @Test @Order(7)
        @DisplayName("Warnings thrown (if-any) is expected")
        public void noUnexpectedWarnings() {
            Assertions.assertFalse(DiscordPS.getDebugger().hasAnyError(), "Expected no thrown error from the Debugger");
            Assumptions.assumingThat(DiscordPS.getDebugger().hasAnyWarning(), () -> {
                Set<Debug.Warning> expected = Set.of(
                    // IF a warning were to happen, we expect it to be notification provider's static initializer
                    // JUnit may initialize NotificationProvider earlier e.g. by earlier test suite
                    // The warning will be resolved by next test order
                    Debug.Warning.NOTIFICATION_CHANNEL_NOT_SET
                    // Add expected warning here if project grows
                );

                for(Debug.Warning warning : expected) {
                    Assertions.assertTrue(DiscordPS.getDebugger().hasWarning(warning),
                        warning + " is expected to thrown but not found.");
                }

                Assertions.assertEquals(expected.size(), DiscordPS.getDebugger().allThrownWarnings().size(),
                "There are one or more unexpected error in all thrown warning lists: "
                    + DiscordPS.getDebugger().allThrownWarnings().toString()
                );
            });
        }

        @Test @Order(8)
        @DisplayName("Notification system should be functional")
        public void notificationExist() {
            // Manually assign notification if not present
            // (can happen if junit initialized NotificationProvider before JDA is ready)
            Assumptions.assumingThat(() -> Notification.getOpt().isEmpty(), NotificationProvider::assignNotification);

            // And then, notification should exist
            Assertions.assertTrue(() -> Notification.getOpt().isPresent(),
                "Expected notification channel to be mocked to a non-null value."
            );
            Assertions.assertFalse(DiscordPS.getDebugger().hasWarning(Debug.Warning.NOTIFICATION_CHANNEL_NOT_SET),
                "Expected the no warning signature 'NOTIFICATION_CHANNEL_NOT_SET' but the signature was thrown"
            );
        }

        @Test @Order(9)
        @DisplayName("ThreadStatus tags is generated")
        public void initAvailableTag() {
            DataArray mockTags = DataArray.empty();

            for (int i = 0; i < ThreadStatus.VALUES.length; i++)
                mockTags.add(MockPluginServer.generateAvailableTag(DataObject::empty, i));

            Assertions.assertAll("Tags Generation",
                () -> Assertions.assertDoesNotThrow(() -> AvailableTag.initCache(mockTags)),
                () -> Assertions.assertDoesNotThrow(() -> AvailableTag.resolveAllTag(plugin.getConfig())),
                () -> Assertions.assertDoesNotThrow(AvailableTag::applyAllTag)
            );
        }

        @Test @Order(10)
        @DisplayName("Finally, resolve ALL warnings (if exists) with no fatal error")
        public void checkoutNoErrorOrWarnings() {
            Assertions.assertFalse(DiscordPS.getDebugger().hasAnyError(), "Expected no thrown error from the Debugger");
            Assertions.assertFalse(DiscordPS.getDebugger().hasAnyWarning(), "Expected no thrown warning from the Debugger");
        }
    }

    @DisplayName("Test Plot-System #1")
    @Nested @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class TestPlotSystem1 extends EventSystemTest {

        @Test @Order(1)
        @DisplayName("Create plot")
        public void createPlot() {
            plotOwner = UUID.randomUUID();
            fire(new PlotCreatedEvent(snapshot(PlotStatus.UNFINISHED)));

            // Expected plot to be created with ongoing status
            this.expectStatus(ThreadStatus.on_going);
        }

        @Test @Order(2)
        @DisplayName("Submit Plot")
        public void submitPlot() {
            fire(new PlotSubmittedEvent(snapshot(PlotStatus.UNREVIEWED)));
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(3)
        @DisplayName("Reject Plot")
        public void rejectPlot() {
            fire(new PlotRejectedEvent(review(PlotStatus.COMPLETED, null)));
            this.expectStatus(ThreadStatus.rejected);
        }

        @Test @Order(4)
        @DisplayName("Undo Review Plot")
        public void undoReviewPlot() {
            fire(new PlotReviewUndoneEvent(review(PlotStatus.UNREVIEWED, null)));
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(5)
        @DisplayName("Approve Plot")
        public void approvePlot() {
            fire(new com.alpsbte.plotsystem.api.event.PlotReviewedEvent(review(PlotStatus.COMPLETED, null)));
            this.expectStatus(ThreadStatus.approved);
        }

        @Test @Order(6)
        @DisplayName("Feedback Plot")
        public void feedbackPlot() {
            String feedback = UUID.randomUUID().toString();
            fire(new PlotFeedbackUpdatedEvent(review(PlotStatus.COMPLETED, feedback)));
            this.expectStatus(ThreadStatus.approved, feedback);
        }

    }

    @DisplayName("Test Plot-System #2")
    @Nested @Order(3)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class TestPlotSystem2 extends EventSystemTest {

        @Test @Order(1)
        @DisplayName("Create plot")
        public void createPlot() {
            plotOwner = UUID.randomUUID();
            fire(new PlotCreatedEvent(snapshot(PlotStatus.UNFINISHED)));

            // Expected plot to be created with ongoing status
            this.expectStatus(ThreadStatus.on_going);
        }

        @Test @Order(2)
        @DisplayName("Submit Plot")
        public void submitPlot() {
            fire(new PlotSubmittedEvent(snapshot(PlotStatus.UNREVIEWED)));
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(3)
        @DisplayName("Undo Submit Plot")
        public void undoSubmitPlot() {
            fire(new PlotSubmissionUndoneEvent(snapshot(PlotStatus.UNFINISHED)));
            this.expectStatus(ThreadStatus.on_going);
        }

        @Test @Order(4)
        @DisplayName("Re-Submit Plot")
        public void reSubmitPlot() {
            fire(new PlotSubmittedEvent(snapshot(PlotStatus.UNREVIEWED)));
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(5)
        @DisplayName("Reject Plot")
        public void rejectPlot() {
            fire(new PlotRejectedEvent(review(PlotStatus.COMPLETED, null)));
            this.expectStatus(ThreadStatus.rejected);
        }

        @Test @Order(6)
        @DisplayName("Feedback Plot")
        public void feedbackPlot() {
            String feedback = UUID.randomUUID().toString();
            fire(new PlotFeedbackUpdatedEvent(review(PlotStatus.COMPLETED, feedback)));
            this.expectStatus(ThreadStatus.rejected, feedback);
        }

        @Order(6)
        @DisplayName("Abandon Plot")
        @EnumSource(com.alpsbte.plotsystem.api.model.PlotAbandonReason.class)
        @ParameterizedTest
        public void abandonPlot(com.alpsbte.plotsystem.api.model.PlotAbandonReason reason) {
            fire(new com.alpsbte.plotsystem.api.event.PlotAbandonedEvent(snapshot(PlotStatus.UNCLAIMED), reason));
            this.expectStatus(ThreadStatus.abandoned);
        }

        @Test @Order(7)
        @DisplayName("Re-claim plot")
        public void reclaimPlot() {
            // Assuming the current plot is abandoned
            UUID newOwner = UUID.randomUUID();
            this.expectStatus(ThreadStatus.abandoned, newOwner, Assertions::assertNotEquals);

            // Re-Send a create event to the plot with new owner
            plotOwner = newOwner;
            fire(new PlotCreatedEvent(snapshot(PlotStatus.UNFINISHED)));

            // Expected plot to be on going with new owner
            this.expectStatus(ThreadStatus.on_going, newOwner, Assertions::assertEquals);
        }

        @Test @Order(8)
        @DisplayName("Archive Plot")
        public void archivePlot() {
            fire(new com.alpsbte.plotsystem.api.event.PlotArchivedEvent(
                snapshot(PlotStatus.UNCLAIMED),
                com.alpsbte.plotsystem.api.model.PlotAbandonReason.SYSTEM,
                plotOwner
            ));
            this.expectStatus(ThreadStatus.archived);
        }
    }

    abstract static class EventSystemTest {
        protected static Integer plotID;
        protected static UUID plotOwner;
        protected static final DiscordSRVListener hook;
        protected static final PlotSystemListener systemListener;
        protected static final DiscordEventListener jdaListener;
        protected static final DiscordCommandListener commandListener;

        static {
            hook = Assertions.assertDoesNotThrow(plugin::getListenerHook);
            systemListener = Assertions.assertDoesNotThrow(plugin::getPlotSystemListener);
            jdaListener = Assertions.assertDoesNotThrow(hook::getEventListener);
            commandListener = Assertions.assertDoesNotThrow(hook::getPluginSlashCommand);
        }

        @BeforeAll
        public static void randomPlotID() {
            plotID = new Random().nextInt(0, 999);
            plotOwner = UUID.randomUUID();
        }

        @AfterAll
        public static void resetPlotID() {
            plotID = null;
            plotOwner = null;
        }

        protected static PlotSnapshot snapshot(PlotStatus status) {
            return PlotSnapshot.builder()
                .id(plotID)
                .owner(plotOwner)
                .status(status)
                .type(PlotType.FOCUS_MODE)
                .difficulty(PlotDifficulty.EASY)
                .cityProjectId("MOCK_CITY")
                .countryCode("MOCK_COUNTRY")
                .lastActivity(LocalDate.now())
                .build();
        }

        protected static ReviewSnapshot review(PlotStatus status, String feedback) {
            return new ReviewSnapshot(plotID, snapshot(status), UUID.randomUUID(), 1, 1, feedback);
        }

        protected static void fire(org.bukkit.event.Event event) {
            Assertions.assertDoesNotThrow(() -> server.getPluginManager().callEvent(event));
        }

        protected void expectStatus(ThreadStatus expected, UUID owner, BiConsumer<Object, Object> comparison) {
            WebhookEntry.ifPlotExisted(plotID).ifPresentOrElse(created -> {
                Assertions.assertSame(created.status(), expected);
                comparison.accept(created.ownerUUID(), owner.toString());
            }, () -> Assertions.fail("Expected plot does not exist in the database"));
        }

        protected void expectStatus(ThreadStatus expected) {
            WebhookEntry.ifPlotExisted(plotID).ifPresentOrElse(created -> {
                Assertions.assertSame(created.status(), expected);
            }, () -> Assertions.fail("Expected plot does not exist in the database"));
        }

        protected void expectStatus(ThreadStatus expected, String feedback) {
            WebhookEntry.ifPlotExisted(plotID).ifPresentOrElse(created -> {
                Assertions.assertSame(created.status(), expected);
                Assertions.assertNotNull(created.feedback());
                Assertions.assertEquals(created.feedback(), feedback);
            }, () -> Assertions.fail("Expected plot does not exist in the database"));
        }
    }

    @AfterAll
    @Override
    public void onServerStop() {
        server.getLogger().info("[TEST] ==========[ PLUGIN TEST END ]============");
        MockPluginServer.super.onServerStop();
    }
}
