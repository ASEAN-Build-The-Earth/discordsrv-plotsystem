package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.ApiSubscribe;
import github.tintinkung.discordps.api.events.PlotAbandonedEvent;
import github.tintinkung.discordps.api.events.PlotCreateEvent;
import github.tintinkung.discordps.api.events.PlotSubmitEvent;
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
    PlotSystemWebhook webhook;

    public PlotSystemListener(PlotSystemWebhook webhook) {
        this.webhook = webhook;
    }

    public void onPlotAbandoned(@NotNull PlotAbandonedEvent event) {

    }

    @ApiSubscribe
    public void onPlotSubmitted(@NotNull PlotSubmitEvent event) {
        DiscordPS.info("New plot has just submitted (Plot ID: " + event.getPlotID() + ")");

        Runnable task = () -> webhook.submitPlot(event.getPlotID());

        // 30 seconds (0.5min * 60000ms / 50tick = 1200)
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, 600);
    }


    @ApiSubscribe
    public void onPlotCreated(@NotNull PlotCreateEvent event) {
        DiscordPS.info("New plot has just created (Plot ID: " + event.getPlotID() + ")");

        Runnable task = () -> {
            // Check if plot already been created by the system
            try {
                if(WebhookEntry.getByPlotID(event.getPlotID()) != null) {
                    DiscordPS.info("Trying to create new plot entry "
                            + "but the plot has already been added to the webhook database "
                            + "(Plot ID: " + event.getPlotID() + ")");
                    return;
                }
            }
            catch (SQLException ex) {
                DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            }

            this.webhook.addNewPlot(event.getPlotID());
        };

        // 1 minutes (1min * 60000ms / 50tick = 1200)
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), task, 1200);
    }
}
