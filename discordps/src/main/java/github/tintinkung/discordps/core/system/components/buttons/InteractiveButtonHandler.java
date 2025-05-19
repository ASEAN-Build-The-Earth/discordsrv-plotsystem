package github.tintinkung.discordps.core.system.components.buttons;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.tintinkung.discordps.commands.interactions.InteractionEvent;

@FunctionalInterface
public interface InteractiveButtonHandler extends PluginButtonHandler {

    void onInteracted(PluginButton button, ButtonClickEvent event, InteractionEvent interactions);
}