package github.tintinkung.discordps.commands.interactions;

public interface PlotInteraction {

    /**
     * Get the plot ID of this interaction
     *
     * @return The plot ID in integer, use {@code this.plotID} for long value
     */
    int getPlotID();
}
