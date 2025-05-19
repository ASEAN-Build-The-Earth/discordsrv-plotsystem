package github.tintinkung.discordps.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A Button that when clicked, exited the plugin slash command
 */
public class ExitButton implements SimpleButtonHandler {
    @Override
    public void onInteracted(@NotNull PluginButton button, @NotNull ButtonClickEvent event) {
        event.editComponents(ActionRow.of(button.get().asDisabled())).queue();

        DiscordPS.getPlugin().exitSlashCommand(button.getIDLong());
    }
}
