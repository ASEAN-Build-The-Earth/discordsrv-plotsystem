package github.tintinkung.discordps.core.api;

public class PlotSubmitEvent extends ApiEvent {
    private final int plotID;

    public PlotSubmitEvent(int plotID) {
        this.plotID = plotID;
    }

    public int getPlotID() {
        return plotID;
    }
}
