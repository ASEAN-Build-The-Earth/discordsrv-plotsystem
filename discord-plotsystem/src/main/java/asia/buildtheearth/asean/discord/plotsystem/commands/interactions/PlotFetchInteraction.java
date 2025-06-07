package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Plot interaction that has to fetch for existing entries as an option menu.
 *
 * @see OnPlotFetch
 * @see OnPlotDelete
 * @see OnReview
 */
public sealed interface PlotFetchInteraction
        extends PlotInteraction
        permits OnPlotFetch, OnPlotDelete, OnReview {

    /**
     * Set the selectable plot options to fetch the specific plot ID.
     *
     * @param fetchOptions The new fetch option to be selected
     */
    void setFetchOptions(@NotNull List<SelectOption> fetchOptions);

    /**
     * Get the selectable options of this interaction
     *
     * @return The current fetch options, Null if it has not been set yet
     */
    @Nullable
    List<SelectOption> getFetchOptions();
}
