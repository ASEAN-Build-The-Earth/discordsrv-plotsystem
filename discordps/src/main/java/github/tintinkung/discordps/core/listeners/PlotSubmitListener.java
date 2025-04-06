package github.tintinkung.discordps.core.listeners;

import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.ApiSubscribe;
import github.tintinkung.discordps.api.events.PlotSubmitEvent;

@SuppressWarnings("unused")
public class PlotSubmitListener {

    @ApiSubscribe
    public void onPlotSubmitted(PlotSubmitEvent event) {
        fetchNewSubmittedPlot(event.getPlotID());
    }

    public void fetchNewSubmittedPlot(int plotID) {
        DiscordPS.info("New plot has just submitted (Plot ID: " + plotID + ")");
    }
}
