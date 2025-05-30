package asia.buildtheearth.asean.discord.plotsystem.api.events;

public final class PlotFeedbackEvent extends PlotReviewEvent{

    private final String feedback;

    public PlotFeedbackEvent(int plotID, String feedback) {
        super(plotID);
        this.feedback = feedback;
    }

    public String getFeedback() {
        return feedback;
    }
}
