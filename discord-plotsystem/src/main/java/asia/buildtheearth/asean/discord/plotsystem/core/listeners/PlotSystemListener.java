package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.system.MemberOwnable;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.events.PlotSnapshotData;
import asia.buildtheearth.asean.discord.plotsystem.core.system.events.PlotSystemEvents;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotMessage;
import com.alpsbte.plotsystem.api.event.PlotAbandonedEvent;
import com.alpsbte.plotsystem.api.event.PlotArchivedEvent;
import com.alpsbte.plotsystem.api.event.PlotCreatedEvent;
import com.alpsbte.plotsystem.api.event.PlotFeedbackUpdatedEvent;
import com.alpsbte.plotsystem.api.event.PlotRejectedEvent;
import com.alpsbte.plotsystem.api.event.PlotReviewUndoneEvent;
import com.alpsbte.plotsystem.api.event.PlotReviewedEvent;
import com.alpsbte.plotsystem.api.event.PlotSubmissionUndoneEvent;
import com.alpsbte.plotsystem.api.event.PlotSubmittedEvent;
import com.alpsbte.plotsystem.api.model.PlotSnapshot;
import com.alpsbte.plotsystem.api.model.ReviewSnapshot;
import github.scarsz.discordsrv.util.SchedulerUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Translates Plot-System's public lifecycle events into the plugin's webhook actions.
 */
@SuppressWarnings("unused")
public class PlotSystemListener implements Listener {
    /** Allow Plot-System three seconds to finish its update before updating the Discord thread. */
    protected static final long DELAYED_TASK = 60;

    private final Deque<Consumer<PlotSystemWebhook>> pendingEvents = new ArrayDeque<>();
    private PlotSystemWebhook webhook;

    public PlotSystemListener() {
    }

    public PlotSystemListener(@NotNull PlotSystemWebhook webhook) {
        this.webhook = webhook;
    }

    public synchronized void setWebhook(@NotNull PlotSystemWebhook webhook) {
        this.webhook = webhook;
        while(!pendingEvents.isEmpty()) {
            Consumer<PlotSystemWebhook> queuedEvent = pendingEvents.removeFirst();
            queueTask(() -> queuedEvent.accept(webhook));
        }
    }

    protected void queueTask(@NotNull Runnable task) {
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @EventHandler
    public void onPlotCreated(@NotNull PlotCreatedEvent event) {
        PlotSnapshotData data = PlotSnapshotData.from(event.getPlot());
        if(data.ownerUUID() == null) {
            DiscordPS.error("Plot System published a plot-created event without an owner (plot " + data.plotID() + ")");
            return;
        }
        queue(new PlotSystemEvents.PlotCreateEvent(event.getPlot().id(), data), webhook ->
            webhook.createAndRegisterNewPlot(event.getPlot().id(), data));
    }

    @EventHandler
    public void onPlotFeedbackUpdated(@NotNull PlotFeedbackUpdatedEvent event) {
        Integer plotID = reviewPlotId(event.getReview(), "plot-feedback-updated");
        if(plotID == null) return;
        PlotSystemEvents.PlotFeedbackEvent translated = new PlotSystemEvents.PlotFeedbackEvent(
            plotID, event.getReview().feedback()
        );
        queue(translated, webhook -> webhook.onPlotReview(translated));
    }

    @EventHandler
    public void onPlotReviewed(@NotNull PlotReviewedEvent event) {
        Integer plotID = reviewPlotId(event.getReview(), "plot-reviewed");
        if(plotID == null) return;
        PlotSystemEvents.PlotApprovedEvent translated = new PlotSystemEvents.PlotApprovedEvent(plotID);
        queue(translated, webhook -> webhook.onPlotReview(translated));
    }

    @EventHandler
    public void onPlotRejected(@NotNull PlotRejectedEvent event) {
        Integer plotID = reviewPlotId(event.getReview(), "plot-rejected");
        if(plotID == null) return;
        PlotSystemEvents.PlotRejectedEvent translated = new PlotSystemEvents.PlotRejectedEvent(plotID);
        queue(translated, webhook -> webhook.onPlotReview(translated));
    }

    @EventHandler
    public void onPlotAbandoned(@NotNull PlotAbandonedEvent event) {
        PlotSystemEvents.PlotAbandonedEvent translated = new PlotSystemEvents.PlotAbandonedEvent(
            event.getPlot().id(), event.getReason()
        );
        queue(translated, webhook -> webhook.onPlotAbandon(translated));
    }

    @EventHandler
    public void onPlotArchived(@NotNull PlotArchivedEvent event) {
        PlotSystemEvents.PlotArchiveEvent translated = new PlotSystemEvents.PlotArchiveEvent(
            event.getPlot().id(), resolveArchiveOwner(event)
        );
        queue(translated, webhook -> webhook.updatePlot(translated, ThreadStatus.archived, plot ->
            Notification.notify(
                PlotMessage.PLOT_ARCHIVE,
                plot.threadID(),
                String.valueOf(translated.getPlotID())
            )
        ));
    }

    @EventHandler
    public void onPlotSubmitted(@NotNull PlotSubmittedEvent event) {
        PlotSystemEvents.PlotSubmitEvent translated = new PlotSystemEvents.PlotSubmitEvent(event.getPlot().id());
        queue(translated, webhook -> webhook.onPlotSubmit(translated));
    }

    @EventHandler
    public void onPlotSubmissionUndone(@NotNull PlotSubmissionUndoneEvent event) {
        PlotSystemEvents.PlotUndoSubmitEvent translated = new PlotSystemEvents.PlotUndoSubmitEvent(event.getPlot().id());
        queue(translated, webhook -> webhook.onPlotUndo(translated));
    }

    @EventHandler
    public void onPlotReviewUndone(@NotNull PlotReviewUndoneEvent event) {
        Integer plotID = reviewPlotId(event.getReview(), "plot-review-undone");
        if(plotID == null) return;
        PlotSystemEvents.PlotUndoReviewEvent translated = new PlotSystemEvents.PlotUndoReviewEvent(plotID);
        queue(translated, webhook -> webhook.onPlotUndo(translated));
    }

    private synchronized void queue(PlotSystemEvents.PlotEvent event, Consumer<PlotSystemWebhook> action) {
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        PlotSystemWebhook target = webhook;
        if(target == null) pendingEvents.addLast(action);
        else this.queueTask(() -> action.accept(target));
    }

    private static @Nullable Integer reviewPlotId(@Nullable ReviewSnapshot review, @NotNull String eventName) {
        if(review == null) {
            DiscordPS.error("Plot System published a " + eventName + " event without a review payload");
            return null;
        }
        PlotSnapshot plot = review.plot();
        if(plot == null) {
            DiscordPS.error("Plot System published a " + eventName + " event without an attached plot");
            return null;
        }
        return plot.id();
    }

    private static @NotNull String resolveArchiveOwner(@NotNull PlotArchivedEvent event) {
        UUID actor = event.getActor();
        if(actor != null) {
            return new MemberOwnable(actor.toString()).getOwnerMentionOrName();
        }
        UUID owner = event.getPlot().owner();
        if(owner != null) {
            return new MemberOwnable(owner.toString()).getOwnerMentionOrName();
        }
        return "Plot-System";
    }
}
