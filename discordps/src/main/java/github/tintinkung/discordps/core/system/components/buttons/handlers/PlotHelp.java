package github.tintinkung.discordps.core.system.components.buttons.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.system.AvailableTag;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.PluginButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;
import github.tintinkung.discordps.core.system.io.lang.PlotInformation;
import github.tintinkung.discordps.core.system.io.lang.PlotInteraction;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public class PlotHelp implements PluginButtonHandler, SimpleButtonHandler {
    @Override
    public void onInteracted(PluginButton button, @NotNull ButtonClickEvent event) {

        // TODO: expose this to build-team related data in config.yml
        String title = DiscordPS.getMessagesLang().get(PlotInformation.HELP_LABEL);
        String content = DiscordPS.getMessagesLang().get(PlotInformation.HELP_CONTENT);

        // TODO: Make the message send with ComponentV2 to be able to format with markdown
        MessageEmbed helpEmbed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(content)
                .setColor(Constants.GREEN)
                .build();
        event.deferReply(true).addEmbeds(helpEmbed).queue();
    }

    @Override
    public void onInteractedBadOwner(@NotNull ButtonClickEvent event) {
        MessageEmbed notOwnerEmbed = DiscordPS.getMessagesLang()
                .getEmbedBuilder(PlotInteraction.INTERACTED_BAD_OWNER)
                .setColor(Constants.RED)
                .build();
        event.deferReply(true).addEmbeds(notOwnerEmbed).queue();
    }
}
