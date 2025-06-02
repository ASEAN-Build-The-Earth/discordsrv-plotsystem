package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotShowcase;

/**
 * Slash command events for the command {@code /plot showcase}
 *
 * @see #onShowcaseConfirm(InteractionHook, OnPlotShowcase)
 */
public interface PlotShowcaseEvent {

    /**
     * The only interaction when user clicked confirm on the showcase information.
     *
     * @param hook The event's hook
     * @param payload The {@link OnPlotShowcase} interaction payload of this event.
     */
    void onShowcaseConfirm(InteractionHook hook, OnPlotShowcase payload);
}
