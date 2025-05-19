package github.tintinkung.discordps.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Button that as plot selection menu as an action row in the message,
 * On interacted; disable the selection menu then disable the button as well.
 */
public class MenuButton implements SimpleButtonHandler {
    @Override
    public void onInteracted(@NotNull PluginButton button, @NotNull ButtonClickEvent event) {
        SelectionMenu menu = null;

        // Find the selection menu
        for(ActionRow row : event.getMessage().getActionRows()) {
            if(row.getComponents().isEmpty()) continue;
            if(row.getComponents().getFirst() instanceof SelectionMenu) {
                menu = (SelectionMenu) row.getComponents().getFirst();
                break;
            }
        }

        // Disable both the menu and button
        if(menu != null)
            event.editComponents(ActionRow.of(menu.asDisabled()), ActionRow.of(button.get().asDisabled())).queue();
        else event.editComponents(ActionRow.of(button.get().asDisabled())).queue();
    }
}
