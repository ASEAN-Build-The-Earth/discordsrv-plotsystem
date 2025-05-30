package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotDelete;

public interface PlotDeleteEvent {

    void onDeleteConfirm(InteractionHook hook, OnPlotDelete interaction);

}
