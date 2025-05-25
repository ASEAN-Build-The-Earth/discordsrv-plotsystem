package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.ApiSubscribe;
import github.tintinkung.discordps.api.events.*;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.system.PlotSystemWebhook;
import org.jetbrains.annotations.NotNull;

/**
 * Main Listener for plot system
 */
@SuppressWarnings("unused")
public class PlotSystemListener {

    /**
     * A mandatory delay (in ticks) to make sure plot-system plugin
     * has enough time to resolve and update
     * plot data before this plugin reacts to the event.
     * <p>
     * Delay is set to 30 seconds: 0.5 minutes × 60,000 ms ÷ 50 ms per tick = 600 ticks.
     */
    private static final long DELAYED_TASK = 60;

    PlotSystemWebhook webhook;

    public PlotSystemListener(PlotSystemWebhook webhook) {
        this.webhook = webhook;
    }

    @ApiSubscribe
    public void onPlotCreated(@NotNull PlotCreateEvent event) {
        Runnable task = () -> this.webhook.createAndRegisterNewPlot(event.getPlotID());

        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotFeedback(@NotNull PlotFeedbackEvent event) {
        // Ignore no feedback
        if(event.getFeedback().equals("No Feedback")) return;

        Runnable task = () -> webhook.onPlotReview(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotApproved(@NotNull PlotApprovedEvent event) {
        Runnable task = () -> webhook.onPlotReview(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotRejected(@NotNull PlotRejectedEvent event) {
        Runnable task = () -> webhook.onPlotReview(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotAbandoned(@NotNull PlotAbandonedEvent event) {
        Runnable task = () -> webhook.onPlotAbandon(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotSubmitted(@NotNull PlotSubmitEvent event) {
        Runnable task = () -> webhook.onPlotSubmit(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotUndoSubmit(@NotNull PlotUndoSubmitEvent event) {
        Runnable task = () -> webhook.onPlotUndo(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotUndoReview(@NotNull PlotUndoReviewEvent event) {
        Runnable task = () -> webhook.onPlotUndo(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }
}
