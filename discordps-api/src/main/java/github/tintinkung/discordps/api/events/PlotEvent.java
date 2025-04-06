package github.tintinkung.discordps.api.events;

public abstract class PlotEvent extends ApiEvent {

    private final int plotID;

    public PlotEvent(int plotID) {
        this.plotID = plotID;
    }

        public int getPlotID() {
        return plotID;
    }
}
