package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.ApiSubscribe;
import github.tintinkung.discordps.api.events.*;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.PlotSystemWebhook;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static github.tintinkung.discordps.Debug.Warning.RUNTIME_SQL_EXCEPTION;

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
    public void onPlotFeedback(@NotNull PlotFeedbackEvent event) {
        Runnable task = () -> webhook.onFeedbackSet(event);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotApproved(@NotNull PlotApprovedEvent event) {
        Runnable task = () -> webhook.updatePlot(event, ThreadStatus.approved);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotRejected(@NotNull PlotRejectedEvent event) {
        Runnable task = () -> webhook.updatePlot(event, ThreadStatus.rejected);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotAbandoned(@NotNull PlotAbandonedEvent event) {
        Runnable task = () -> webhook.updatePlot(event, ThreadStatus.abandoned);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }

    @ApiSubscribe
    public void onPlotSubmitted(@NotNull PlotSubmitEvent event) {
        Runnable task = () -> webhook.updatePlot(event, ThreadStatus.finished);
        DiscordPS.info("Got event: " + event.getClass().getSimpleName());
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }


    @ApiSubscribe
    public void onPlotCreated(@NotNull PlotCreateEvent event) {
        Runnable task = () -> this.webhook.addNewPlot(event.getPlotID());

        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, DELAYED_TASK);
    }
}
