package github.tintinkung.discordps.core.listeners;

import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.ApiSubscribe;
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

    @ApiSubscribe
    public void onPlotSubmitted(@NotNull PlotSubmitEvent event) {
        DiscordPS.info("New plot has just submitted (Plot ID: " + event.getPlotID() + ")");

        webhook.submitPlot(event.getPlotID());
    }


    @ApiSubscribe
    public void onPlotCreated(@NotNull PlotCreateEvent event) {
        DiscordPS.info("New plot has just created (Plot ID: " + event.getPlotID() + ")");

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

        webhook.addNewPlot(event.getPlotID());
    }
}
