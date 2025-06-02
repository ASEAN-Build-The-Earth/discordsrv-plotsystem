package asia.buildtheearth.asean.discord.plotsystem.core.system.buttons;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInteraction;
import org.jetbrains.annotations.NotNull;

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
