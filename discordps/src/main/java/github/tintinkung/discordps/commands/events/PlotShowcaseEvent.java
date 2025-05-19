package github.tintinkung.discordps.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.tintinkung.discordps.commands.interactions.OnPlotShowcase;

public interface PlotShowcaseEvent {

    void onShowcaseConfirm(InteractionHook hook, OnPlotShowcase payload);
}
