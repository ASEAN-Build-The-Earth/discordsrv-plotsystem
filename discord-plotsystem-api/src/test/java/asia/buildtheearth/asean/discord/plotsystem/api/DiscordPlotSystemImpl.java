package asia.buildtheearth.asean.discord.plotsystem.api;

import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.api.test.mock.MockDiscordPlotSystemPlugin;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Wrapper for all plot-system implementations.
 *
 * <p>This class is an example of how to integrate a java plugin to this API</p>
 *
 * <p>This singleton utility wraps interaction with the {@link asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystem}
 * and should only be initialized if the Discord plugin is available and enabled.</p>
 *
 * <p>Submitting a plot: </p>{@snippet :
 * getOpt(MockDiscordPlotSystemPlugin.MOCK_PLOT_ID).ifPresent(DiscordPlotSystemImpl.PlotEventAction::onPlotSubmit);
 * }<p>Always check for availability using {@link #getOpt(int)} before calling integration methods,
 * to avoid runtime errors if the plugin is not installed.</p>
 *
 * @see #init(Plugin)
 * @see #getOpt(int)
 */
public class DiscordPlotSystemImpl {

    private static @Nullable DiscordPlotSystemImpl instance;

    private final @NotNull DiscordPlotSystem api;

    private DiscordPlotSystemImpl(@NotNull DiscordPlotSystem api) {
        this.api = api;
    }

    /**
     * Prepare a plot event by plot ID.
     *
     * @param plotID The plot ID to prepare.
     * @return Optional of this instance that is mapped to a new {@link PlotEventAction}
     * @see PlotEventAction
     */
    public static Optional<PlotEventAction> getOpt(int plotID) {
        return Optional.ofNullable(instance).map(instance -> new PlotEventAction(plotID, instance));
    }


    /**
     * Initialize the API with physical instance.
     *
     * @param plugin The qualified plugin instance to initialize with.
     */
    public static void init(@NotNull org.bukkit.plugin.Plugin plugin) {
        if(plugin instanceof DiscordPlotSystem discordPlugin) {
            DiscordPlotSystemImpl.instance = new DiscordPlotSystemImpl(discordPlugin);
            DiscordPlotSystemAPI.registerProvider(new DiscordPlotSystemImpl.DiscordDataProvider());
        }
    }

    /**
     * Call an event using the API instance.
     *
     * @param event Event object to call ({@link PlotEvent} since we only test for plot-related in the test suite)
     * @param <E> Event type to call
     */
    public <E extends PlotEvent> void callEvent(E event) {
        this.api.callEvent(event);
    }

    /**
     * Event action to prepare a plot ID and decide what event to call.
     *
     * @see PlotEventAction#PlotEventAction(int, DiscordPlotSystemImpl)  PlotEventAction
     */
    public static final class PlotEventAction {

        private final int plotID;
        private final @NotNull DiscordPlotSystemImpl api;

        /**
         * Create a new event action using an API instance.
         *
         * @param plotID The plot ID of this action
         * @param api API instance to be used to call events
         */
        private PlotEventAction(int plotID, @NotNull DiscordPlotSystemImpl api) {
            this.plotID = plotID;
            this.api = api;
        }

        public void onPlotCreate() {
            this.api.callEvent(new PlotCreateEvent(this.plotID));
        }

        public void onPlotCreate(Object rawData) {
            this.api.callEvent(new PlotCreateEvent(this.plotID, rawData));
        }

        public void onPlotCreate(PlotCreateData data) {
            this.api.callEvent(new PlotCreateEvent(this.plotID, data));
        }

        public void onPlotSubmit() {
            this.api.callEvent(new PlotSubmitEvent(this.plotID));
        }

        public void onPlotAbandon(@NotNull AbandonType type) {
            this.api.callEvent(new PlotAbandonedEvent(this.plotID, type)
            );
        }

        public void onPlotAbandon() {
            this.api.callEvent(new PlotAbandonedEvent(this.plotID));
        }

        public void onPlotReject() {
            this.api.callEvent(new PlotRejectedEvent(this.plotID));
        }

        public void onPlotFeedback(String feedback) {
            this.api.callEvent(new PlotFeedbackEvent(this.plotID, feedback));
        }

        public void onPlotApprove() {
            this.api.callEvent(new PlotApprovedEvent(this.plotID));
        }

        public void onPlotUndoReview() {
            this.api.callEvent(new PlotUndoReviewEvent(this.plotID));
        }

        public void onPlotUndoSubmit() {
            this.api.callEvent(new PlotUndoSubmitEvent(this.plotID));
        }

        public void onPlotArchive(String owner) {
            this.api.callEvent(new PlotArchiveEvent(this.plotID, owner));
        }

        public void onPlotReclaim(String owner) {
            this.api.callEvent(new PlotReclaimEvent(this.plotID, owner));
        }

        public void onPlotInactivity(LocalDate abandonDate) {
            this.api.callEvent(new InactivityNoticeEvent(this.plotID, abandonDate));
        }

        public void onPlotNotification(NotificationType type, boolean cancelled) {
            PlotNotificationEvent event = new PlotNotificationEvent(this.plotID, type);
            if(cancelled) event.setCancelled();
            this.api.callEvent(event);
        }
    }

    /**
     * Data Provider for {@link DiscordPlotSystemAPI}
     * registered during the plugin onEnabled.
     *
     * @see DiscordPlotSystemAPI#registerProvider(PlotCreateProvider)
     */
    private static final class DiscordDataProvider implements PlotCreateProvider {

        @Override
        public PlotCreateData getData(Object rawData) {
            if(rawData instanceof PlotCreateData data) return data;
            else if(rawData instanceof Integer plotID) return MockDiscordPlotSystemPlugin.MOCK_PROVIDER.get(plotID);
            throw new IllegalArgumentException("Unexpected raw data object of type " + rawData.getClass().getSimpleName());
        }

        @Override
        public PlotCreateData getData(int plotID) {
            return MockDiscordPlotSystemPlugin.MOCK_PROVIDER.get(plotID);
        }
    }
}

