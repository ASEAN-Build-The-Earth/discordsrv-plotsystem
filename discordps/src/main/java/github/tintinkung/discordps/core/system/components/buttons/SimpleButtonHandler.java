package github.tintinkung.discordps.core.system.components.buttons;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;

@FunctionalInterface
public interface SimpleButtonHandler extends PluginButtonHandler {
    void onInteracted(PluginButton button, ButtonClickEvent event);
}
