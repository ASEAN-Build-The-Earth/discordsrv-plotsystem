package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.HistoryMessage;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.InfoComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.ErrorMessage.PLOT_UPDATE_SQL_EXCEPTION;

/**
 * Plot-System forum thread manager,
 * provide 3 major structure for creating/modifying Plot-System thread.
 *
 * <p>{@linkplain  PlotSystemWebhook#newThreadForPlotID(PlotSystemThread, PlotCreateData, boolean, boolean) #newThreadForPlotID}
 * Implementation: </p>
 * <blockquote>{@snippet :
 * public void newThreadForPlotID(PlotCreateData data, PlotSystemThread thread) {
 *     // 1st: Retrieve plot data from thread provider
 *     PlotData plotData = thread.getProvider().apply(data);
 *
 *     // 2nd: Get the display name from thread applier
 *     String threadName = thread.getApplier().apply(plotData);
 *
 *     // 3rd: Optional modification to info component if provided
 *     InfoComponent infoComponent = new InfoComponent(0, plotData);
 *     thread.getModifier().accept(plotData, infoComponent);
 * }}</blockquote>
 *
 * @see #setProvider(PlotDataProvider) Provide plot data
 * @see #setApplier(ThreadNameApplier) Apply thread name
 * @see #setModifier(PlotInfoModifier) Modify plot layout
 */
public class PlotSystemThread {

    /**
     * Thread name formatter for the giving plot data,
     * require plot ID and owner name to format.
     *
     * <p>Example formatted: {@code Plot#17 @bob (bobTheBuilder)}</p>
     */
    public static final BiFunction<Integer, String, String> THREAD_NAME = (plotID, ownerName) -> DiscordPS.getMessagesLang()
            .get(PlotInformation.THREAD_NAME)
            .replace(Format.OWNER, ownerName)
            .replace(Format.PLOT_ID, String.valueOf(plotID));

    private static final Function<Integer, ThreadNameApplier> DEFAULT_THREAD_NAME = plotID -> owner -> THREAD_NAME.apply(plotID, owner.formatOwnerName());

    /**
     * Formatter for initial history message to newly create plot, require owner name to format.
     *
     * <p>Example formatted: {@code @bob created the plot 3 minutes ago}</p>
     */
    public static final Function<String, String> INITIAL_HISTORY = ownerName -> DiscordPS.getMessagesLang()
            .get(HistoryMessage.INITIAL_CREATION)
            .replace(Format.OWNER, ownerName)
            .replace(Format.TIMESTAMP, String.valueOf(Instant.now().getEpochSecond()));

    private static final PlotInfoModifier DEFAULT_THREAD_CREATED_STATUS = (owner, info) -> info.addHistory(INITIAL_HISTORY.apply(owner.getOwnerMentionOrName()));

    /**
     * The plot data provider (usually {@link PlotData#PlotData(PlotCreateData)})
     * which can be modified before passing the instance.
     */
    private @Nullable PlotDataProvider provider;

    /** Applier for thread name, invoked with the plot's owner info. */
    private @Nullable ThreadNameApplier applier;

    /** Optional modifier for info data after it is initialized */
    private @Nullable PlotInfoModifier modifier;

    /** The nullable thread ID if the plot already have thread data registered before. */
    private @Nullable Long threadID;

    /** The unique ID of the tracking plot on this thread. */
    private final int plotID;

    /**
     * Construct a new thread information from plot ID.
     *
     * @param plotID The plot ID tracked by this thread.
     */
    public PlotSystemThread(int plotID) {
        this.plotID = plotID;
    }

    /**
     * Construct a thread information from existing thread.
     *
     * @param plotID The plot ID tracked by this thread.
     * @param threadID The existing thread ID of this instance.
     */
    public PlotSystemThread(int plotID, long threadID) {
        this.plotID = plotID;
        this.threadID = threadID;
    }

    /**
     * Get the plot ID tracked by this thread.
     *
     * @return The unique plot ID in integer.
     */
    public int getPlotID() {
        return this.plotID;
    }

    /**
     * Get the thread's snowflake ID if existed.
     *
     * @return If thread exist (created by the constructor {@link PlotSystemThread#PlotSystemThread(int, long)})
     *         then a non-empty optional is returned with the thread ID.
     */
    public @NotNull Optional<@NotNull Long> getOptID() {
        return Optional.ofNullable(this.threadID);
    }

    /**
     * Set a new {@link PlotData} provider for this thread.
     *
     * @param provider New plot data provider
     * @see PlotDataProvider
     */
    public void setProvider(@NotNull PlotDataProvider provider) {
        this.provider = provider;
    }

    /**
     * Set a new {@linkplain #DEFAULT_THREAD_NAME thread name} to apply.
     *
     * @param applier New thread name applier
     * @see ThreadNameApplier
     */
    public void setApplier(@NotNull ThreadNameApplier applier) {
        this.applier = applier;
    }

    /**
     * Set a layout modifier for plot's initial thread layout.
     *
     * @param modifier New plot's {@link InfoComponent} modifier
     * @see PlotInfoModifier
     */
    public void setModifier(@NotNull PlotInfoModifier modifier) {
        this.modifier = modifier;
    }

    /**
     * Get the {@link PlotData} provider for this thread.
     *
     * @return A defined provider (default to {@link PlotData#PlotData(PlotCreateData)})
     */
    public @NotNull PlotDataProvider getProvider() {
        return this.provider == null? PlotData::new : this.provider;
    }

    /**
     * Get layout modifier for plot's initial thread layout.
     *
     * @return A defined modifier (default to {@linkplain #INITIAL_HISTORY}
     *         which applies initial plot-create history to the layout)
     */
    public @NotNull PlotInfoModifier getModifier() {
        return this.modifier == null? DEFAULT_THREAD_CREATED_STATUS : this.modifier;
    }

    /**
     * Get the applier for this thread's name.
     *
     * @return A defined applier (default to {@linkplain #THREAD_NAME})
     */
    public @NotNull ThreadNameApplier getApplier() {
        return this.applier == null?  DEFAULT_THREAD_NAME.apply(this.plotID) : this.applier;
    }

    /**
     * Modifier for {@link InfoComponent} before building
     */
    @FunctionalInterface
    public interface PlotInfoModifier extends BiConsumer<MemberOwnable, InfoComponent> {
        /**
         * Accept pending modifications to info component.
         *
         * @param owner The owner which owns this plot info
         * @param info The info component to be modified
         */
        @Override
        void accept(MemberOwnable owner, InfoComponent info);
    }

    /**
     * Applier for plot's thread name.
     */
    @FunctionalInterface
    public interface ThreadNameApplier extends Function<MemberOwnable, String> {
        /**
         * Resolve for thread name with owner
         *
         * @param owner The owner data of this thread/plot
         * @return Formatted thread name applied with owner data
         */
        @Override
        String apply(MemberOwnable owner);
    }

    /**
     * Provider for all {@link PlotData} usage.
     */
    @FunctionalInterface
    public interface PlotDataProvider extends Function<PlotCreateData, PlotData> {
        /**
         * Apply the provider for plot data.
         *
         * @param plotData Plot information to be applied to the provider that provides the instance
         * @return A new plot data instance constructed from the provider
         */
        @Override
        PlotData apply(PlotCreateData plotData);
    }

    /**
     * Record entry representing a plot update action
     *
     * @param plotID The plot ID this action will revolve with
     * @param entry The webhook entry reference
     * @param messageID The plot entry ID as its status message ID
     * @param threadID The plot's thread ID
     */
    public record UpdateAction(int plotID,
                               @NotNull WebhookEntry entry,
                               @NotNull String messageID,
                               @NotNull String threadID) {

        /**
         * Create a new update action from the given event.
         *
         * @param event The plot event occurred
         * @return A new {@link UpdateAction} from the event's plot ID,
         *         null if sql exception occurred trying to query for entries
         *         and if the plot entry does not exist to return as event.
         * @param <T> The event type (extends {@link PlotEvent})
         */
        public static <T extends PlotEvent> @Nullable UpdateAction fromEvent(@NotNull T event) {
            WebhookEntry entry;
            try {
                List<WebhookEntry> entries = WebhookEntry.getByPlotID(event.getPlotID());
                if(entries.isEmpty()) throw new SQLException("Entry for plot ID: " + event.getPlotID() + " Does not exist.");
                entry = entries.getFirst();
            }
            catch (SQLException ex) {
                DiscordPS.error("Failed to fetch webhook entry for plot ID: " + event.getPlotID(), ex);
                DiscordPS.warning("Skipping plot update event " + event.getClass().getSimpleName());
                Notification.sendErrorEmbed(PLOT_UPDATE_SQL_EXCEPTION,
                    ex.toString(),
                    String.valueOf(event.getPlotID()),
                    event.getClass().getSimpleName()
                );
                return null;
            }

            String threadID = Long.toUnsignedString(entry.threadID());
            String messageID = Long.toUnsignedString(entry.messageID());

            return new UpdateAction(event.getPlotID(), entry, messageID, threadID);
        }
    }
}
