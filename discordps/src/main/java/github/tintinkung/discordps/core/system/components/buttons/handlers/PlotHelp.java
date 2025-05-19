package github.tintinkung.discordps.core.system.components.buttons.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.tintinkung.discordps.core.system.AvailableTag;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.PluginButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public class PlotHelp implements PluginButtonHandler, SimpleButtonHandler {
    @Override
    public void onInteracted(PluginButton button, @NotNull ButtonClickEvent event) {
        MessageEmbed helpEmbed = new EmbedBuilder()
                .setTitle("Help")
                .setDescription("Please message our support bot <@1361310076308033616> " +
                        "for questions if you need help about this plot. (or just ping a staff here)")
                .addField("Building",
                        "Click the documentation link above to take a look " +
                                "at a detailed guide on how to build your plot!", false)
                .setColor(AvailableTag.APPROVED.getColor())
                .build();
        event.deferReply(true).addEmbeds(helpEmbed).queue();
    }

    @Override
    public void onInteractedBadOwner(@NotNull ButtonClickEvent event) {
        MessageEmbed notOwnerEmbed = new EmbedBuilder()
                .setTitle("You don't own this plot")
                .setDescription("Go build a plot!")
                .setColor(Color.RED)
                .build();
        event.deferReply(true).addEmbeds(notOwnerEmbed).queue();
    }
}
