package github.tintinkung.discordps.core.system.components.buttons.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.Notification;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.PluginButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.SQLException;

public class PlotFeedback implements PluginButtonHandler, SimpleButtonHandler {
    @Override
    public void onInteracted(@NotNull PluginButton button, ButtonClickEvent event) {
        Checks.notNull(button.getPayload(), "Feedback button payload");

        try {
            WebhookEntry entry = WebhookEntry.getByMessageID(button.getIDLong());

            if(entry == null || entry.feedback() == null)
                throw new SQLException("Trying to get feedback that does not exist in the database!");

            MessageEmbed feedbackEmbed = new EmbedBuilder()
                    .setTitle("Your Feedback")
                    .setDescription(entry.feedback())
                    .addField("Help",
                            "Please message our support bot <@1361310076308033616> " +
                                    "for questions if you need help about this plot. (or just ping a staff here)",
                            false)
                    .setColor(entry.status().toTag().getColor())
                    .build();

            event.deferReply(true).addEmbeds(feedbackEmbed).queue();


        } catch (SQLException ex) {
            MessageEmbed errorEmbed = new EmbedBuilder()
                    .setTitle("Error :(")
                    .setDescription("Sorry an error occurred trying to get your feedback data. " +
                            "Please message our support bot <@1361310076308033616> to ask for it.")
                    .setColor(Color.RED)
                    .build();
            event.deferReply(true).addEmbeds(errorEmbed).queue();


            String plot = button.getPayload();

            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Runtime exception **fetching** plot feedback, "
                            + "The owner of plot ID #`" + plot + "` "
                            + "cannot view their feedback message!")
                    .addField("Error", "```" + ex.toString() + "```", false)
                    .setColor(Color.RED)
                    .build()
            );
        }
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
