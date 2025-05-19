package github.tintinkung.discordps.commands.interactions;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Plot interaction that has to fetch for existing entries as an option menu
 */
public interface PlotFetchInteraction extends PlotInteraction {

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
