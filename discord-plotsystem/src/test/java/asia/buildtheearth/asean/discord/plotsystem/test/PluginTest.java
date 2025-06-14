package asia.buildtheearth.asean.discord.plotsystem.test;

import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordCommandListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordEventListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.DiscordSRVListener;
import asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.test.mock.MockDiscordSRV;
import asia.buildtheearth.asean.discord.plotsystem.test.mock.MockPluginServer;
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
        @DisplayName("No error or warning occurred during DiscordReadyEvent")
        public void expectedError() {
            Assertions.assertFalse(DiscordPS.getDebugger().hasAnyError(), "Expected no thrown error from the Debugger");
            Assertions.assertFalse(DiscordPS.getDebugger().hasAnyWarning(), "Expected no thrown warning from the Debugger");
        }

        @Test @Order(7)
        @DisplayName("Notification system should be functional")
        public void notificationExist() {
            Assertions.assertFalse(DiscordPS.getDebugger().hasWarning(Debug.Warning.NOTIFICATION_CHANNEL_NOT_SET),
                "Expected the no warning signature 'NOTIFICATION_CHANNEL_NOT_SET' but the signature was thrown"
            );
            Assertions.assertTrue(Notification.getOpt().isPresent(),
                "Expected notification channel to be mocked to a non-null value."
            );
        }

        @Test @Order(8)
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
    }

    @DisplayName("Test Plot-System #1")
    @Nested @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class TestPlotSystem1 extends EventSystemTest {

        @Test @Order(1)
        @DisplayName("Create plot")
        public void createPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                .callEvent(new PlotCreateEvent(plotID,
                    new PlotCreateData(
                        plotID,
                        UUID.randomUUID().toString(),
                        PlotCreateData.PlotStatus.ON_GOING,
                        "MOCK_CITY",
                        "MOCK_COUNTRY",
                        new double[] {0, 0}
                    )
                )
            ));

            // Expected plot to be created with ongoing status
            this.expectStatus(ThreadStatus.on_going);
        }

        @Test @Order(2)
        @DisplayName("Submit Plot")
        public void submitPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotSubmitEvent(plotID))
            );
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(3)
        @DisplayName("Reject Plot")
        public void rejectPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotRejectedEvent(plotID))
            );
            this.expectStatus(ThreadStatus.rejected);
        }

        @Test @Order(4)
        @DisplayName("Undo Review Plot")
        public void undoReviewPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotUndoReviewEvent(plotID))
            );
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(5)
        @DisplayName("Approve Plot")
        public void approvePlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotApprovedEvent(plotID))
            );
            this.expectStatus(ThreadStatus.approved);
        }

        @Test @Order(6)
        @DisplayName("Feedback Plot")
        public void feedbackPlot() {
            String feedback = UUID.randomUUID().toString();
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotFeedbackEvent(plotID, feedback))
            );
            this.expectStatus(ThreadStatus.approved, feedback);
        }

        @Test @Order(7)
        @DisplayName("Archive Plot")
        public void archivePlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotArchiveEvent(plotID, "MockSystem"))
            );
            this.expectStatus(ThreadStatus.archived);
        }
    }

    @DisplayName("Test Plot-System #2")
    @Nested @Order(3)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class TestPlotSystem2 extends EventSystemTest {

        @Test @Order(1)
        @DisplayName("Create plot")
        public void createPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance().callEvent(
                new PlotCreateEvent(plotID,
                    new PlotCreateData(
                            plotID,
                            UUID.randomUUID().toString(),
                            PlotCreateData.PlotStatus.ON_GOING,
                            "MOCK_CITY",
                            "MOCK_COUNTRY",
                            new double[] {0, 0}
                    ))
                )
            );

            // Expected plot to be created with ongoing status
            this.expectStatus(ThreadStatus.on_going);
        }

        @Test @Order(2)
        @DisplayName("Submit Plot")
        public void submitPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotSubmitEvent(plotID))
            );
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(3)
        @DisplayName("Undo Submit Plot")
        public void undoSubmitPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotUndoSubmitEvent(plotID))
            );
            this.expectStatus(ThreadStatus.on_going);
        }

        @Test @Order(4)
        @DisplayName("Re-Submit Plot")
        public void reSubmitPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotSubmitEvent(plotID))
            );
            this.expectStatus(ThreadStatus.finished);
        }

        @Test @Order(5)
        @DisplayName("Reject Plot")
        public void rejectPlot() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotRejectedEvent(plotID))
            );
            this.expectStatus(ThreadStatus.rejected);
        }

        @Test @Order(6)
        @DisplayName("Feedback Plot")
        public void feedbackPlot() {
            String feedback = UUID.randomUUID().toString();
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotFeedbackEvent(plotID, feedback))
            );
            this.expectStatus(ThreadStatus.rejected, feedback);
        }

        @Test @Order(5)
        @DisplayName("Notify Inactivity Plot")
        public void inactivityNotice() {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new InactivityNoticeEvent(plotID, LocalDate.now()))
            );
        }

        @Order(6)
        @DisplayName("Abandon Plot")
        @EnumSource(AbandonType.class)
        @ParameterizedTest
        public void abandonPlot(AbandonType type) {
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance()
                    .callEvent(new PlotAbandonedEvent(plotID, type))
            );
            this.expectStatus(ThreadStatus.abandoned);
        }

        @Test @Order(7)
        @DisplayName("Re-claim plot")
        public void reclaimPlot() {
            // Assuming the current plot is abandoned
            UUID newOwner = UUID.randomUUID();
            this.expectStatus(ThreadStatus.abandoned, newOwner, Assertions::assertNotEquals);

            // Re-Send a create event to the plot with new owner
            Assertions.assertDoesNotThrow(() -> DiscordPlotSystemAPI.getInstance().callEvent(
                new PlotCreateEvent(plotID,
                    new PlotCreateData(
                        plotID,
                        newOwner.toString(),
                        PlotCreateData.PlotStatus.ON_GOING,
                        "MOCK_CITY",
                        "MOCK_COUNTRY",
                        new double[] {0, 0}
                    ))
                )
            );

            // Expected plot to be on going with new owner
            this.expectStatus(ThreadStatus.on_going, newOwner, Assertions::assertEquals);
        }
    }

    abstract static class EventSystemTest {
        protected static Integer plotID;
        protected static final DiscordSRVListener hook;
        protected static final PlotSystemListener systemListener;
        protected static final DiscordEventListener jdaListener;
        protected static final DiscordCommandListener commandListener;

        static {
            hook = Assertions.assertDoesNotThrow(plugin::getListenerHook);
            systemListener = Assertions.assertDoesNotThrow(hook::getPlotSystemListener);
            jdaListener = Assertions.assertDoesNotThrow(hook::getEventListener);
            commandListener = Assertions.assertDoesNotThrow(hook::getPluginSlashCommand);
        }

        @BeforeAll
        public static void randomPlotID() {
            plotID = new Random().nextInt(0, 999);
        }

        @AfterAll
        public static void resetPlotID() {
            plotID = null;
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
