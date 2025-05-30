package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import github.scarsz.discordsrv.util.SchedulerUtil;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.ApiSubscribe;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotFeedbackEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotApprovedEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotRejectedEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotAbandonedEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotSubmitEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotUndoSubmitEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotUndoReviewEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemWebhook;

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
