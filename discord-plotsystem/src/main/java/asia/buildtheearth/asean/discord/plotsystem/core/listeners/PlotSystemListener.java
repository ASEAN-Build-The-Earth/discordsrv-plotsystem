package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification;
import github.scarsz.discordsrv.util.SchedulerUtil;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.ApiSubscribe;
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
     * Delay is set to 3 seconds: 0.05 minutes × 60,000 ms ÷ 50 ms per tick = 60 ticks.
     */
    protected static final long DELAYED_TASK = 60;

    protected final PlotSystemWebhook webhook;

    public PlotSystemListener(PlotSystemWebhook webhook) {
        this.webhook = webhook;
    }

    protected void queueTask(@NotNull Runnable task) {
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotCreated(@NotNull PlotCreateEvent event) {
        Runnable task = () -> this.webhook.createAndRegisterNewPlot(event.getPlotID(), event.getData());
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotFeedback(@NotNull PlotFeedbackEvent event) {
        Runnable task = () -> this.webhook.onPlotReview(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotApproved(@NotNull PlotApprovedEvent event) {
        Runnable task = () -> this.webhook.onPlotReview(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotRejected(@NotNull PlotRejectedEvent event) {
        Runnable task = () -> this.webhook.onPlotReview(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotAbandoned(@NotNull PlotAbandonedEvent event) {
        Runnable task = () -> this.webhook.onPlotAbandon(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotSubmitted(@NotNull PlotSubmitEvent event) {
        Runnable task = () -> this.webhook.onPlotSubmit(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotUndoSubmit(@NotNull PlotUndoSubmitEvent event) {
        Runnable task = () -> this.webhook.onPlotUndo(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotUndoReview(@NotNull PlotUndoReviewEvent event) {
        Runnable task = () -> this.webhook.onPlotUndo(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotInactivityNotice(@NotNull InactivityNoticeEvent event) {
        Runnable task = () -> this.webhook.onPlotInactivity(event);
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }

    @ApiSubscribe
    public void onPlotArchive(@NotNull PlotArchiveEvent event) {
        Runnable task = () -> this.webhook.updatePlot(event, ThreadStatus.archived, plot ->
            asia.buildtheearth.asean.discord.plotsystem.core.system.Notification.notify(
                Notification.PlotMessage.PLOT_ARCHIVE, plot.threadID(), String.valueOf(event.getPlotID())
            )
        );
        DiscordPS.debug("Got event: " + event.getClass().getSimpleName());
        this.queueTask(task);
    }
}
