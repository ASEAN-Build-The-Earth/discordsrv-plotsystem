package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotDelete;
import org.jetbrains.annotations.NotNull;

/**
 * Slash command events for the command {@code /plotctl delete}
 *
 * @see #onDeleteConfirm(InteractionHook, OnPlotDelete)
 */
public interface PlotDeleteEvent {

    /**
     * The only interaction when user clicked on the delete confirmation button
     * after delete entry is selected.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotDelete} interaction payload of this event.
     */
    void onDeleteConfirm(@NotNull InteractionHook hook, @NotNull OnPlotDelete interaction);
}
