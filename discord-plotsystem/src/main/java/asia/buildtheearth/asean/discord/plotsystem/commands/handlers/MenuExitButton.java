package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import org.jetbrains.annotations.NotNull;

/**
 * Function the same as {@link MenuButton} but exit the slash command after interacted.
 */
class MenuExitButton extends MenuButton {

    @Override
    public void onInteracted(@NotNull PluginButton button, @NotNull ButtonClickEvent event) {
        super.onInteracted(button, event);

        DiscordPS.getPlugin().exitSlashCommand(button.getIDLong());
    }
}
