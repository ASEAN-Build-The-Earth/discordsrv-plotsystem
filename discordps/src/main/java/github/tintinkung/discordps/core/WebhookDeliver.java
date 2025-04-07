package github.tintinkung.discordps.core;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.tintinkung.discordps.DiscordPS;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.utils.BuilderUser;
import github.tintinkung.discordps.core.utils.CoordinatesUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.Color;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public class WebhookDeliver {

    public WebhookDeliver() {


    }

    public static void fetchSubmittedPlots() {
        try {
            List<PlotEntry> plots = PlotEntry.fetchSubmittedPlots();

            // test plot
            PlotEntry plot = plots.getFirst();

            String[] mcLocation = plot.mcCoordinates().split(",");

            double xCords = Double.parseDouble(mcLocation[0].trim());
            double zCords = Double.parseDouble(mcLocation[2].trim());


            double[] geoCords = CoordinatesUtil.convertToGeo(xCords, zCords);



            String geoCoordinates = CoordinatesUtil.formatGeoCoordinatesNumeric(geoCords);

            MessageEmbed embed = new EmbedBuilder()
                    .setDescription(geoCoordinates)
                    .setColor(Color.YELLOW)
                    .setFooter(plot.status().toString())
                    .build();

            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(plot.ownerUUID()));
            Member ownerDiscord = BuilderUser.getAsDiscordMember(owner);

            try {
                MessageReference sentMessage = WebhookManager.newThreadFromWebhook(
                "Plot #" + plot.plotID(),
                    BuilderUser.getAsAvatarURL(owner),
                "## Plot #" + plot.plotID() + " by " + ownerDiscord.getAsMention(),
                    embed
                ).complete().orElseThrow();

                DiscordPS.info("Sent webhook of message id: " + sentMessage.getMessageId());
            } catch (NoSuchElementException ex) {
                DiscordPS.error("Failed to send webhook: " + ex);
            };

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
