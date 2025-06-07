package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotFetch;
import org.jetbrains.annotations.NotNull;

/**
 * Slash command events for the command {@code /plotctl fetch}
 *
 * @see #onConfirmOverride(InteractionHook, OnPlotFetch)
 * @see #onCreateRegister(InteractionHook, OnPlotFetch)
 * @see #onCreateUntracked(InteractionHook, OnPlotFetch)
 * @see #onConfirmFetch(InteractionHook, OnPlotFetch)
 */
public interface PlotFetchEvent {

    /**
     * Interacted when user confirm the prompt to override plot data of an existing thread.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onConfirmOverride(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction);

    /**
     * Interacted when user confirm to fetch the plot with their slash command inputs.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onConfirmFetch(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction);

    /**
     * If the fetch process is creating a new plot thread,
     * invoked when user decided to create and register the new thread.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onCreateRegister(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction);

    /**
     * If the fetch process is creating a new plot thread,
     * invoked when user decided to create the new thread as un-tracked.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onCreateUntracked(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction);
}
