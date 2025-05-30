package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import org.jetbrains.annotations.NotNull;

public interface PlotArchiveEvent {

    void onImagesAttached(Message message, OnPlotArchive interaction);

    void onImagesProvided(InteractionHook hook, OnPlotArchive interaction);

    void onConfirmArchive(InteractionHook hook, @NotNull OnPlotArchive interaction);

    void onShowcaseConfirmed(InteractionHook hook, OnPlotArchive interaction);
}
