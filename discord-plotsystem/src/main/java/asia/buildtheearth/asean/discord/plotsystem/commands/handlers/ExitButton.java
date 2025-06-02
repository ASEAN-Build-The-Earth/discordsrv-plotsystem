package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;
import org.jetbrains.annotations.NotNull;

/**
 * A Button that when clicked, exited the plugin slash command
 */
class ExitButton implements SimpleButtonHandler {
    @Override
    public void onInteracted(@NotNull PluginButton button, @NotNull ButtonClickEvent event) {
        event.editComponents(ActionRow.of(button.get().asDisabled())).queue();

        DiscordPS.getPlugin().exitSlashCommand(button.getIDLong());
    }
}
