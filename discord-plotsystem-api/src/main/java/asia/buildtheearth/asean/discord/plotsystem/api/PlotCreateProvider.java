package asia.buildtheearth.asean.discord.plotsystem.api;

/**
 * A provider interface for constructing {@link PlotCreateData} from raw input data.
 * <p>
 * This interface is used exclusively by {@link asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent}
 * to extract structured plot data from an external object.
 * </p>
 * 
 * @see #getData(Object)
 * @see #getData(int)
 *
 * @deprecated API is deprecated for official Plot-System's API module.
 *             DiscordPlotSystemAPI will no longer release starting by 1.3.0.
 */
@Deprecated(since = "1.2.5")
public interface PlotCreateProvider {

    /**
     * Converts the given raw input into a structured {@link PlotCreateData} instance.
     *
     * <p>The raw input can be any object type required by the provider.
     * It's up to the implementing class to handle parsing and validation.</p>
     *
     * @param rawData The raw input object defined and expected by the provider implementation.
     * @return A valid {@link PlotCreateData} object created from the input.
     */
    PlotCreateData getData(Object rawData);

    /**
     * Converts a unique plot ID to {@link PlotCreateData} instance.
     *
     * @param plotID The unique integer plot ID to retrieve data with.
     * @return A valid {@link PlotCreateData} object created from the input.
     */
    PlotCreateData getData(int plotID);
}
