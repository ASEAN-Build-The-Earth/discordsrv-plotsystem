package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.dependencies.okhttp3.HttpUrl;
import github.scarsz.discordsrv.util.HttpUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.providers.WebhookProviderImpl;
import github.tintinkung.discordps.utils.AvatarUtil;
import github.tintinkung.discordps.utils.BuilderUser;
import github.tintinkung.discordps.utils.CoordinatesUtil;
import github.tintinkung.discordps.utils.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class PlotSystemWebhook {
    private final ForumWebhookImpl webhook;

    public PlotSystemWebhook(ForumWebhookImpl webhook) {
        this.webhook = webhook;
    }

    public WebhookProviderImpl getProvider() {
        return this.webhook.getProvider();
    }

    public void fetchLatestPlot() {
        List<PlotEntry> plots;

        try { plots = PlotEntry.fetchSubmittedPlots(); }
        catch (SQLException e) {
            DiscordPS.error("Failed to fetch plot data from the database!", e);
            return;
        }

        // test plot
        PlotEntry plot = plots.getFirst();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(plot.ownerUUID()));
        Member ownerDiscord = BuilderUser.getAsDiscordMember(owner);
        boolean hasDiscord = ownerDiscord != null;

        String[] mcLocation = plot.mcCoordinates().split(",");

        double xCords = Double.parseDouble(mcLocation[0].trim());
        double zCords = Double.parseDouble(mcLocation[2].trim());
        double[] geoCords = CoordinatesUtil.convertToGeo(xCords, zCords);
        String geoCoordinates = CoordinatesUtil.formatGeoCoordinatesNumeric(geoCords);
        String displayCords = CoordinatesUtil.formatGeoCoordinatesNSEW(geoCords);

        ThreadStatus status = ThreadStatus.toPlotStatus(plot.status());
        Set<Long> statusTags = new HashSet<>();
        if(status != null) {
            String tagID = status.toTag().getTag().getID();
            Checks.isSnowflake(tagID, "Forum Tag ID");
            statusTags.add(Long.parseUnsignedLong(tagID));
        }

//      .setImage("https://cdn.jsdelivr.net/gh/BuildTheEarth/assets@main/images/emojis/hug_builder_lg.png")

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Plot #" + plot.plotID())
                .setDescription("A plot in " + plot.countryName() +
                        ", " + plot.cityName()
                        + " by " + (hasDiscord? ownerDiscord.getAsMention() : owner.getName())
                        + " at " + displayCords)
                .addField("City", plot.cityName(), true)
                .addField("Country", plot.countryName(), true)
                .addField("Owner", (hasDiscord? ownerDiscord.getAsMention() : owner.getName()), true)
                .addField("Status", plot.status().toString(), true)
                .setColor(status.toTag().getColor())
                .setFooter(plot.status().toString() + " - " + TimeUtil.date());


//        int queryStart = avatarURL.indexOf(63);
//        embedBuilder.setThumbnail(avatarURL.substring(0, queryStart));

        if(hasDiscord)
            embedBuilder.setAuthor(ownerDiscord.getEffectiveName(), null, ownerDiscord.getEffectiveAvatarUrl());
        else embedBuilder.setAuthor(owner.getName());


        String threadName = "Plot #" + plot.plotID();


        // Figure out builder avatar
        String avatarFormat = "jpg";

        // DataFolder/media/UUID/avatar-image.jpg
        Path mediaPath = DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/" + plot.ownerUUID());

        // Make player's directory if not exist
        if(mediaPath.toFile().mkdirs()) {
            DiscordPS.debug("Created player media cache for UUID: " + mediaPath);
        }

        File avatarFile = mediaPath.resolve("avatar-image." + avatarFormat).toFile();

        // TODO: Plot screenshot for imageFile
        File imageFile = null;

        try {
            imageFile = FileUtil.findImageFileByPrefix(Constants.WEBHOOK_AVATAR_FILE);
        } catch (IOException ex) {
            DiscordPS.warning("Failed to attach image file");
        }

        if(avatarFile.exists()) {
            embedBuilder.setThumbnail("attachment://" + avatarFile.getName());
        }

        if(imageFile != null && imageFile.exists()) {
            embedBuilder.setImage("attachment://" + imageFile.getName());
        }

        WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                .setThreadName(threadName)
                .setEmbeds(Collections.singletonList(embedBuilder.build()))
                .suppressNotifications()
                .suppressMentions()
                .build();

        // Try download player's minecraft avatar
        try {
            // Download if not exist
            if(avatarFile.createNewFile()) {
                URL avatarURL = AvatarUtil.getAvatarUrl(plot.ownerUUID(), 128, avatarFormat);
                FileUtil.downloadFile(avatarURL, avatarFile);
            }
        }
        catch (HttpRequest.HttpRequestException ex) {
            DiscordPS.error("Failed to download URL for player avatar image: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            DiscordPS.error("IO Exception occurred trying to read player media folder at: "
                + avatarFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
        }

        if(imageFile != null && imageFile.exists()) {
            data.addFile(imageFile);
        }

        if(avatarFile.exists()) {
            data.addFile(avatarFile);
        }

        try {
            webhook.newThreadFromWebhook(data, statusTags, true)
                .queue((optMessage) -> {

                    MessageReference sentMessage = optMessage.orElseThrow();

                    Button plotLinkButton = Button.link(" https://www.google.com/maps/place/" + geoCoordinates, "Google Map");

                    ActionRow actionRow = (hasDiscord)? ActionRow.of(
                        Button.primary(
                            Constants.NEW_PLOT_HELP_BUTTON.apply(
                                sentMessage.getMessageIdLong(),
                                ownerDiscord.getIdLong(),
                                plot.plotID()),
                            "Help"),
                        plotLinkButton
                    ) : ActionRow.of(plotLinkButton);

                    WebhookDataBuilder.WebhookData interactionData = new WebhookDataBuilder()
                            .setComponents(Collections.singletonList(actionRow))
                            .build();

                    webhook.editInitialThreadMessage(
                            sentMessage.getMessageId(),
                            interactionData,
                            true)
                            .queueAfter(1L, TimeUnit.SECONDS);

                    DiscordPS.debug("Sent webhook of message id: " + sentMessage.getMessageId());

                },
                (failed) -> {
                    throw new RuntimeException(failed);
                });
        } catch (RuntimeException ex) {
            DiscordPS.error("Failed to send webhook: " + ex);
        };
    }


}
