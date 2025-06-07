package asia.buildtheearth.asean.discord.plotsystem.api.events;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;

/**
 * <p>The first event in the plot lifecycle and is used
 * to initialize database record for further plot-related events.</p>
 *
 * @see PlotCreateData
 * @see asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateProvider
 */
public final class PlotCreateEvent extends PlotEvent {

    private final PlotCreateData data;

    /**
     * Constructs a new PlotCreateEvent by converting raw data using the registered provider.
     *
     * @param plotID The ID of the plot being created.
     * @param rawData The raw data to convert into {@link PlotCreateData}.
     * @throws IllegalArgumentException If the provider is not registered or conversion fails.
     */
    public PlotCreateEvent(int plotID, Object rawData) throws IllegalArgumentException {
        super(plotID);
        this.data = DiscordPlotSystemAPI
            .getDataProvider()
            .getData(rawData);
    }

    /**
     * Constructs a new PlotCreateEvent using pre-prepared {@link PlotCreateData}.
     *
     * @param plotID The ID of the plot being created.
     * @param data The fully constructed {@link PlotCreateData}.
     */
    public PlotCreateEvent(int plotID, PlotCreateData data) {
        super(plotID);
        this.data = data;
    }

    /**
     * Constructs a new PlotCreateEvent using the plot ID as the raw data source.
     * <p>This assumes the provider supports parsing an integer ID as raw data.</p>
     *
     * @param plotID The ID of the plot being created.
     * @throws IllegalArgumentException If the provider fails to convert the ID to plot data.
     */
    public PlotCreateEvent(int plotID) throws IllegalArgumentException {
        this(plotID, plotID);
    }

    /**
     * Returns the full plot creation data.
     *
     * @return The {@link PlotCreateData} for this event.
     */
    public PlotCreateData getData() {
        return data;
    }
}
