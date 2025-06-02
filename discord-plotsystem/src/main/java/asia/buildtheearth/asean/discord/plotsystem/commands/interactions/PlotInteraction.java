package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

/**
 * Base interface for all plot related command interactions/payload.
 *
 * @see #getPlotID()
 */
public sealed interface PlotInteraction
        permits OnPlotArchive, OnPlotShowcase, PlotFetchInteraction {

    /**
     * Get the plot ID of this interaction
     *
     * @return The plot ID in integer, use {@code this.plotID} for long value
     */
    int getPlotID();
}
