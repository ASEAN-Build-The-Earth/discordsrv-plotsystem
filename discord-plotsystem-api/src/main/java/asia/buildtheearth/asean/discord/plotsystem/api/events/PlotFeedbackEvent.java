package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Set a feedback message to a reviewed plot.
 *
 * <p>Note: Use the feedback string {@code No Feedback} to skip this event.</p>
 */
public final class PlotFeedbackEvent extends PlotReviewEvent{

    private final String feedback;

    /**
     * Construct the feedback-event by a given message.
     *
     * @param plotID The plot ID in integer
     * @param feedback The given feedback message of this plot
     */
    public PlotFeedbackEvent(int plotID, String feedback) {
        super(plotID);
        this.feedback = feedback;
    }

    /**
     * Get the feedback message of this event
     *
     * @return The message as a string
     */
    public String getFeedback() {
        return feedback;
    }
}
