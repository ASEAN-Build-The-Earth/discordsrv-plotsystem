package asia.buildtheearth.asean.discord.plotsystem.api;

import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.api.test.*;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockDiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockDiscordPlotSystemPlugin;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockEventListener;
import asia.buildtheearth.asean.discord.plotsystem.api.test.utils.DisplayNameResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemImpl.PlotEventAction;

/**
 * Main test runner for the Discord Plot-System API.
 *
 * <p>This suite begins with 3 core test cases that validate general API functionality,
 * followed by 12 targeted test cases covering each {@link PlotEvent} subclass.</p>
 *
 * <p>All plot event tests extend {@link PlotEventTest}, which performs the following steps:</p>
 * <ol>
 *     <li>Dispatches the event to a mocked event listener</li>
 *     <li>Retrieves the triggered event and verifies its data integrity</li>
 * </ol>
 *
 * <p>Some specific plot event has its own test runner class that extends {@link PlotEventTest}
 * and overrides event-specific methods. These runner classes are located in the {@code test} sub-package.</p>
 *
 * @see PlotAbandonTest
 * @see PlotCreateTest
 * @see PlotEventTest
 * @see PlotNotificationTest
 * @see ScopedPlotEventTest
 */
@DisplayName("API")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class DiscordPlotSystemTest extends MockDiscordPlotSystemPlugin {

    private static final ApiManager api = new ApiManager();

    @BeforeAll
    static void beforeAll() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, DiscordPlotSystemAPI::getDataProvider,
                "Getting API Data provider before its initialized should throw exception but no error was thrown"
        );
    }

    @Override
    public <E extends ApiEvent> E callEvent(E event) {
        return api.callEvent(event);
    }

    @Override
    public boolean unsubscribe(Object listener) {
        return api.unsubscribe(listener);
    }

    @Override
    public void subscribe(Object listener) {
        if(listener instanceof MockEventListener mockListener)
            api.subscribe(this.eventListener = mockListener);
        else api.subscribe(listener);
    }

    @Override
    public void onEnable() {
        DiscordPlotSystemAPI.plugin = this;
        MockDiscordPlotSystemAPI.plugin = this;
    }

    @Override
    public void onLoad() {
        DiscordPlotSystemImpl.init(this);
    }

    @Test
    @Override
    @DisplayName("Enable plugin and initialize instance")
    public void enablePlugin() {
        super.enablePlugin();
        Assertions.assertNotNull(DiscordPlotSystemAPI.getInstance(), "API instance returned null after enabling plugin!");
        Assertions.assertNotNull(MockDiscordPlotSystemAPI.getInstance(), "Mock instance returned null after enabling plugin!");
    }

    @Test
    @Timeout(1)
    @DisplayName("API Manager is functional")
    void testAPI() {
        Assertions.assertNotNull(DiscordPlotSystemAPI.getInstance(), "Plugin instance is expected but its value is Null");
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> MockDiscordPlotSystemAPI.getInstance().subscribe(new EventListener() { }));

        CompletableFuture<ApiEvent> receiver = new CompletableFuture<>();

        EventListener listener = new EventListener() {
            @ApiSubscribe @SuppressWarnings("unused")
            public void testingMethod(ApiEvent event) {
                receiver.complete(event);
                throw new RuntimeException();
            }
        };

        MockDiscordPlotSystemAPI.getInstance().subscribe(listener);

        MockDiscordPlotSystemAPI.getInstance().callEvent(new ApiEvent() { });

        receiver.orTimeout(100, TimeUnit.MILLISECONDS).whenComplete((received, error) -> {
            if(error != null) Assertions.fail("Error occurred on the received event with: " + error);

           Assertions.assertNotNull(received, "Received event is null");
           Assertions.assertInstanceOf(ApiEvent.class, received);
        });

        Assertions.assertTrue(MockDiscordPlotSystemAPI.getInstance().unsubscribe(listener),
            "Test listener is expected to be unsubscribed but the return value return false"
        );

        Assertions.assertThrowsExactly(
            IllegalArgumentException.class,
            () -> MockDiscordPlotSystemAPI.getInstance().subscribe(new EventListener() { }),
            "Subscribing empty event listener should throw but no exception has thrown"
        );
    }

    @Test
    @DisplayName("Subscribe to API with mock listener")
    void subscribeAPI() {
        Assertions.assertNotNull(DiscordPlotSystemAPI.getInstance(), "Plugin instance is expected but its value is Null");

        MockDiscordPlotSystemAPI.getInstance().subscribe(new MockEventListener());

        Assertions.assertNotNull(MockDiscordPlotSystemAPI.getInstance().getEventListener(), "Listener not subscribed");
    }

    @Nested @DisplayName("Logging") @Order(1)
    class Logging {

        @Test @DisplayName("Error Thrown")
        void testThrowable() {
            Throwable testThrowable = new RuntimeException("Thrown Error");
            String testError = "Test Throwable";

            DiscordPlotSystemAPI.error(testError, testThrowable);
            Assertions.assertTrue(logger.popFirst().startsWith(testError));
            Assertions.assertTrue(logger.popFirst().startsWith(testThrowable.getClass().getName()));
            logger.clear();

            DiscordPlotSystemAPI.error(testThrowable);
            Assertions.assertTrue(logger.popFirst().startsWith(testThrowable.getClass().getName()));
            logger.clear();
        }

        @Nested @DisplayName("Test Messages")
        class Messages {
            private final List<String> testMessages = List.of("Info Message", "Warning Message", "Error Message");

            private void testMessage(Consumer<String> message) {
                testMessages.forEach(message);
                Assertions.assertTrue(logger.equals(testMessages));
                logger.clear();
            }

            @Test @DisplayName("Info Logged")
            void testInfo() { testMessage(DiscordPlotSystemAPI::info); }

            @Test @DisplayName("Warning Logged")
            void testWarning() { testMessage(DiscordPlotSystemAPI::warning); }

            @Test @DisplayName("Error Logged")
            void testError() { testMessage(DiscordPlotSystemAPI::error); }
        }
    }

    @Nested @DisplayName("Creating a plot") @Order(2)
    class PlotCreate {
        private final PlotCreateData expected = DiscordPlotSystemAPI.getDataProvider().getData(MOCK_PLOT_ID);
        private final EnumMap<PlotCreateData.PlotStatus, List<String>> statusMap = new EnumMap<>(PlotCreateData.PlotStatus.class);
        private final String[] contents = new String[]{
            MOCK_UUID, MOCK_CITY_ID, MOCK_COUNTRY, MOCK_STATUS.toString(),
            String.valueOf(MOCK_PLOT_ID), Arrays.toString(MOCK_COORDINATES)
        };

        PlotCreate() {
            this.statusMap.put(PlotCreateData.PlotStatus.FINISHED, List.of("finished", "completed", "unreviewed"));
            this.statusMap.put(PlotCreateData.PlotStatus.ON_GOING, List.of("on_going", "unfinished"));
            this.statusMap.put(PlotCreateData.PlotStatus.ABANDONED, List.of("abandoned", "unclaimed"));
            this.statusMap.put(PlotCreateData.PlotStatus.REJECTED, List.of("rejected"));
            this.statusMap.put(PlotCreateData.PlotStatus.APPROVED, List.of("approved"));
            this.statusMap.put(PlotCreateData.PlotStatus.ARCHIVED, List.of("archived"));
        }

        void checkEachStatus(Function<String, String> name) {
            this.statusMap.forEach((key, value) -> value.forEach(status -> {
                Assertions.assertEquals(PlotCreateData.prepareStatus(name.apply(status)), key);
                Assertions.assertEquals(PlotCreateData.prepareStatus(name.apply(status)).getName(), value.getFirst());
            }));
        }

        @Test @DisplayName("All status is valid")
        void prepareAvailableStatus() {
            this.checkEachStatus(Function.identity());
            this.checkEachStatus(name -> name.toUpperCase(Locale.ENGLISH));
        }

        @Test @DisplayName("Unknown status is thrown")
        void checkUnknownStatus() {
            Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> PlotCreateData.prepareStatus(""));
            Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> PlotCreateData.prepareStatus(null));
        }

        @Test @DisplayName("Mock plot create data is as expected")
        void checkCreateData() {
            PlotCreateData expected = DiscordPlotSystemAPI.getDataProvider().getData(MOCK_PLOT_ID);
            for(String content : contents) Assertions.assertTrue(expected.toString().contains(content));
            Assertions.assertEquals(expected.toString(), MOCK_PROVIDER.get(MOCK_PLOT_ID).toString());
            Assertions.assertEquals(expected, MOCK_PROVIDER.get(MOCK_PLOT_ID));
        }

        @Nested @DisplayName("by plot ID")
        class CreateByID extends PlotCreateTest {
            CreateByID() { super(PlotEventAction::onPlotCreate); }
        }

        @Nested @DisplayName("using data provider")
        class CreateByObject extends PlotCreateTest {
            CreateByObject() { super(event -> event.onPlotCreate((Object) expected)); }
        }

        @Nested @DisplayName("by a pre-defined object")
        class CreateByData extends PlotCreateTest {
            CreateByData() { super(event -> event.onPlotCreate(expected)); }
        }
    }

    @Nested @Order(3)
    @DisplayName("Notify:")
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    class PlotNotification {
        @ExtendWith(DisplayNameResolver.class)
        abstract static class Notification {
            protected final String type;
            Notification(String type) { this.type = type; }
        }

        @TestClassOrder(ClassOrderer.OrderAnnotation.class)
        abstract static class Test extends Notification {
            Test(String type) { super(type); }

            @Nested @DisplayName("-> Call Event") @Order(1)
            class TestNotify extends PlotNotificationTest { TestNotify() { super(Test.this.type, false); } }

            @Nested @DisplayName("-> Cancel Event") @Order(2)
            class TestCancelled extends PlotNotificationTest { TestCancelled() { super(Test.this.type, true); } }
        }

        @Nested @Order(0)
        @DisplayName("Inactivity notice with current time")
        class InactivityNotice extends PlotEventTest {
            static final LocalDate timestamp = LocalDate.now();

            InactivityNotice() {
                super(InactivityNoticeEvent.class, event -> event.onPlotInactivity(timestamp));
            }

            @Override public <T extends PlotEvent>
            void retrieveEvent(@NotNull T event) { testInactivityNotice(event, timestamp); }
        }

        @Nested @DisplayName("ON_CREATED")
        class Create extends Test { Create(String type) { super(type); } }

        @Nested @DisplayName("ON_SUBMITTED")
        class Submit extends Test { Submit(String type) { super(type); } }

        @Nested @DisplayName("ON_REVIEWED")
        class Review extends Test { Review(String type) { super(type); } }

        @Nested @DisplayName("ON_APPROVED")
        class Approve extends Test { Approve(String type) { super(type); } }

        @Nested @DisplayName("ON_REJECTED")
        class Reject extends Test { Reject(String type) { super(type); } }

        @Nested @DisplayName("ON_SHOWCASED")
        class Showcase extends Test { Showcase(String type) { super(type); } }

        @Nested @DisplayName("ON_UNDO_REVIEW")
        class UndoReview extends Test { UndoReview(String type) { super(type); } }

        @Nested @DisplayName("ON_UNDO_SUBMIT")
        class UndoSubmit extends Test { UndoSubmit(String type) { super(type); } }

        @Nested @DisplayName("ON_ABANDONED")
        class Abandon extends Test { Abandon(String type) { super(type); } }

        @Nested @DisplayName("ON_INACTIVITY")
        class Inactive extends Test { Inactive(String type) { super(type); } }
    }

    @Nested @Order(4)
    @DisplayName("Submitting a plot")
    class PlotSubmit extends PlotEventTest {
        PlotSubmit() { super(PlotSubmitEvent.class, PlotEventAction::onPlotSubmit); }
    }

    @Nested @Order(5)
    @DisplayName("Abandoning a plot for: ")
    @ExtendWith(DisplayNameResolver.class)
    class PlotAbandon {
        static final String NONE = "N/A", COMMANDS = "COMMANDS", MANUALLY = "MANUALLY", INACTIVE = "INACTIVE", SYSTEM = "SYSTEM";

        @Nested @DisplayName(NONE)
        class None extends PlotEventTest {
            None() { super(PlotAbandonedEvent.class, PlotEventAction::onPlotAbandon); }

            @Override public <T extends PlotEvent>
            void retrieveEvent(@NotNull T retrieved) { testAbandonEvent(retrieved, AbandonType.SYSTEM); }
        }

        @Nested @DisplayName(COMMANDS)
        class Commands extends PlotAbandonTest { Commands(String type) { super(type); } }

        @Nested @DisplayName(MANUALLY)
        class Manually extends PlotAbandonTest { Manually(String type) { super(type); } }

        @Nested @DisplayName(SYSTEM)
        class System extends PlotAbandonTest { System(String type) { super(type); } }

        @Nested @DisplayName(INACTIVE)
        class Inactive extends PlotAbandonTest { Inactive(String type) { super(type); } }
    }

    @Nested @Order(6)
    @DisplayName("Reclaiming abandoned plot")
    class PlotReclaim extends ScopedPlotEventTest {
        PlotReclaim() { super(PlotReclaimEvent.class, PlotEventAction::onPlotReclaim); }
    }

    @Nested @Order(7)
    @DisplayName("Approving a plot")
    class PlotApprove extends PlotEventTest {
        PlotApprove() { super(PlotApprovedEvent.class, PlotEventAction::onPlotApprove); }
    }

    @Nested @Order(8)
    @DisplayName("Rejecting a plot")
    class PlotReject extends PlotEventTest {
        PlotReject() { super(PlotRejectedEvent.class, PlotEventAction::onPlotReject); }
    }

    @Nested @Order(9)
    @DisplayName("Sending feedback to a plot")
    class PlotFeedback extends ScopedPlotEventTest {
        private static final String TEST_FEEDBACK = "This is a test feedback message.";
        PlotFeedback() { super(PlotFeedbackEvent.class, PlotEventAction::onPlotFeedback, TEST_FEEDBACK); }

        @Override
        protected <T extends PlotEvent> void test(@NotNull T retrieved) {
            testScopedEvent(PlotFeedbackEvent.class, PlotFeedbackEvent::getFeedback, retrieved, TEST_FEEDBACK);
        }
    }

    @Nested @Order(10)
    @DisplayName("Undoing review of a plot")
    class PlotUndoReview extends PlotEventTest {
        PlotUndoReview() { super(PlotUndoReviewEvent.class, PlotEventAction::onPlotUndoReview); }
    }

    @Nested @Order(11)
    @DisplayName("Undoing submission of a plot")
    class PlotUndoSubmit extends PlotEventTest {
        PlotUndoSubmit() { super(PlotUndoSubmitEvent.class, PlotEventAction::onPlotUndoSubmit); }
    }

    @Nested @Order(12)
    @DisplayName("Archiving completed plot")
    class PlotArchive extends ScopedPlotEventTest {
        PlotArchive() { super(PlotArchiveEvent.class, PlotEventAction::onPlotArchive); }
    }
}