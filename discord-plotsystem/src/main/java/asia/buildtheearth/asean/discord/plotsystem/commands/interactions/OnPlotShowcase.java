package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Payload for the command {@code /plot showcase}
 *
 * @see asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotShowcaseEvent
 */
public final class OnPlotShowcase extends Interaction implements PlotInteraction {
    /**
     * The plot ID to be deleted
     */
    public final long plotID;

    /**
     * Plot data to showcase this plot, retrieve on trigger of the command.
     */
    private @Nullable PlotData plotData;

    /**
     * Plot entry to showcase this plot, retrieve on trigger of the command.
     */
    private @Nullable WebhookEntry plotEntry;

    /**
     * Create a plot-delete payload
     *
     * @param userID The interaction owner ID (snowflake)
     * @param eventID The initial event ID (snowflake)
     * @param plotID The plot ID of this delete process
     */
    public OnPlotShowcase(long userID, long eventID, long plotID) {
        super(userID, eventID);
        this.plotID = plotID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPlotID() {
        return (int) plotID;
    }

    /**
     * Set the plot data of this showcase
     *
     * @param plotData Plot's external information
     * @param plotEntry Plot's webhook entry information
     */
    public void setPlotInfo(@NotNull PlotData plotData, @NotNull WebhookEntry plotEntry) {
        this.plotData = plotData;
        this.plotEntry = plotEntry;
    }

    /**
     * Get the plot data to showcase
     *
     * @return Nullable plot data, the payload must be updated
     *         with {@link #setPlotInfo(PlotData, WebhookEntry)} first before retrieving non-null value
     */
    public @Nullable PlotData getPlotData() {
        return plotData;
    }

    /**
     * Get the plot entry to showcase
     *
     * @return Nullable plot entry, the payload must be updated
     *         with {@link #setPlotInfo(PlotData, WebhookEntry)} first before retrieving non-null value
     */
    public @Nullable WebhookEntry getPlotEntry() {
        return plotEntry;
    }

}
