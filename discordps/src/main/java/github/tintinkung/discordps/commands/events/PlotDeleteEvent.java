package github.tintinkung.discordps.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;

public interface PlotDeleteEvent {

    void onDeleteConfirm(InteractionHook hook, OnPlotDelete interaction);

}
