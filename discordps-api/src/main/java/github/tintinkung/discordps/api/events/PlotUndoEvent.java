package github.tintinkung.discordps.api.events;

/**
 * All available undo event
 *
 * @see PlotUndoReviewEvent
 * @see PlotUndoReviewEvent
 */
public abstract sealed class PlotUndoEvent extends PlotEvent
    permits PlotUndoSubmitEvent, PlotUndoReviewEvent {

    public PlotUndoEvent(int plotID) {
        super(plotID);
    }
}
