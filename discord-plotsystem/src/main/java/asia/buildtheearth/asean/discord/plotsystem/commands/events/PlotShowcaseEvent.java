package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotShowcase;

public interface PlotShowcaseEvent {

    void onShowcaseConfirm(InteractionHook hook, OnPlotShowcase payload);
}
