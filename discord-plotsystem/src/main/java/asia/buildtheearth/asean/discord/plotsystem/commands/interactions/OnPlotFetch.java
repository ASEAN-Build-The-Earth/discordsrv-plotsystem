package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload for the command {@code /plotctl fetch}
 *
 * @see asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotFetchEvent
 */
public final class OnPlotFetch extends Interaction implements PlotFetchInteraction {
    /**
     * The plot ID to fetch on this interaction
     */
    public final long plotID;

    /**
     * Whether to override an already existing entry
     */
    public final boolean override;

    private final @Nullable ThreadStatus primaryStatus;
    private @Nullable List<SelectOption> fetchOptions;

    /**
     * Create a plot-delete payload
     *
     * @param userID The interaction owner ID (snowflake)
     * @param eventID The initial event ID (snowflake)
     * @param plotID The plot ID of this delete process
     * @param override Whether to override an already existing entry
     * @param status The primary status to fetch this plot ID
     */
    public OnPlotFetch(long userID, long eventID, long plotID, boolean override, @Nullable ThreadStatus status) {
        super(userID, eventID);
        this.plotID = plotID;
        this.override = override;
        this.primaryStatus = status;
    }

    /**
     * Get the chosen status to fetch this plot with
     *
     * @return The status as a {@link ThreadStatus}
     */
    public @Nullable ThreadStatus getPrimaryStatus() {
        return primaryStatus;
    }

    /**
     * {@inheritDoc}
     */
    public int getPlotID() {
        return (int) plotID;
    }

    /**
     * {@inheritDoc}
     */
    public void setFetchOptions(@NotNull List<SelectOption> fetchOptions) {
        this.fetchOptions = new ArrayList<>(fetchOptions);
    }

    /**
     * {@inheritDoc}
     */
    public @Nullable List<SelectOption> getFetchOptions() {
        return fetchOptions;
    }
}