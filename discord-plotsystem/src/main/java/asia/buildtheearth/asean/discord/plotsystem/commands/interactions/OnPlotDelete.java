package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class OnPlotDelete extends Interaction implements PlotFetchInteraction {
    /**
     * The plot ID to be deleted
     */
    public final long plotID;

    private @Nullable List<SelectOption> fetchOptions;

    /**
     * Create a plot-delete payload
     *
     * @param userID The interaction owner ID (snowflake)
     * @param eventID The initial event ID (snowflake)
     * @param plotID The plot ID of this delete process
     */
    public OnPlotDelete(long userID, long eventID, long plotID) {
        super(userID, eventID);
        this.plotID = plotID;
    }

    public int getPlotID() {
        return (int) plotID;
    }

    public void setFetchOptions(@NotNull List<SelectOption> fetchOptions) {
        this.fetchOptions = new ArrayList<>(fetchOptions);
    }

    public @Nullable List<SelectOption> getFetchOptions() {
        return fetchOptions;
    }
}
