package asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.InteractionEvent;

@FunctionalInterface
public interface InteractiveButtonHandler extends PluginButtonHandler {

    void onInteracted(PluginButton button, ButtonClickEvent event, InteractionEvent interactions);
}