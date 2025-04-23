package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.ReceivedMessage;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.dependencies.okhttp3.HttpUrl;
import github.scarsz.discordsrv.util.HttpUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.providers.WebhookProviderImpl;
import github.tintinkung.discordps.utils.AvatarUtil;
import github.tintinkung.discordps.utils.BuilderUser;
import github.tintinkung.discordps.utils.CoordinatesUtil;
import github.tintinkung.discordps.utils.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PlotSystemWebhook {
    private final ForumWebhookImpl webhook;

    public PlotSystemWebhook(ForumWebhookImpl webhook) {
        this.webhook = webhook;
    }

    public WebhookProviderImpl getProvider() {
        return this.webhook.getProvider();
    }

    public void addNewPlot(int plotID) {
        PlotEntry plot = PlotEntry.getByID(plotID);

        // Prepare plot data
        if(plot == null) return;
        PlotData plotData = new PlotData(plot);
        EmbedBuilder embed = plotData.prepareEmbed();

        // Set Timestamp
        try {
            Date timestamp = DateFormat.getDateTimeInstance().parse(TimeUtil.timeStamp());
            embed.setTimestamp(timestamp.toInstant());
        }
        catch (ParseException ex) {
            DiscordPS.warning("Failed to parse timestamp (" + ex.getMessage() + ")");
        }

        // Set embed author
        plotData.getOwnerDiscord().ifPresentOrElse(
            (owner) -> embed.setAuthor(owner.getEffectiveName(), null, owner.getEffectiveAvatarUrl()),
            () -> embed.setAuthor(plotData.getOwner().getName())
        );

        // Figure out builder avatar
        String avatarFormat = "png";
        File avatarFile = prepareAvatarFile(plot.ownerUUID(), avatarFormat);
        if(downloadAvatarToFile(plot.ownerUUID(), avatarFile, avatarFormat))
            embed.setThumbnail("attachment://" + avatarFile.getName());

        // Create Webhook Data
        String threadName = "Plot #" + plot.plotID();

        WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                .setThreadName(threadName)
                .setEmbeds(Collections.singletonList(embed.build()))
                .suppressNotifications()
                .suppressMentions()
                .build();

        if(avatarFile.exists())
            data.addFile(avatarFile);

        final Consumer<Optional<MessageReference>> onSuccess = (optMessage) -> {
            MessageReference sentMessage = optMessage.orElseThrow();
            onNewThreadCreated(sentMessage, plotData);
            insertEntryToDatabase(new WebhookEntry(
                    sentMessage.getMessageIdLong(),
                    plotID,
                    plotData.getStatus(),
                    plot.ownerUUID()
            ));
        };

        final Consumer<? super Throwable> onFailure = (failed) -> {
            throw new RuntimeException(failed);
        };

        try {
            webhook.newThreadFromWebhook(data, plotData.getStatusTags(), true).queue(onSuccess, onFailure);
        } catch (RuntimeException ex) {
            DiscordPS.error("Failed to send webhook: " + ex);

            Notification.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(":red_circle: Discord Plot-System Error")
                .setDescription("Runtime exception creating new plot thread, "
                        + "The plot ID #`" + plotID + "` "
                        + "will may or may not be tracked by the system depending on the error.")
                .addField("Error", ex.toString(), false)
                .setColor(Color.RED)
                .build()
            );
        };
    }

    public void submitPlot(int plotID) {
        WebhookEntry entry;
        try {
            entry = WebhookEntry.getByPlotID(plotID);
            if(entry == null) throw new SQLException("Entry for plot ID: " + plotID + " Does not exist.");
        }
        catch (SQLException ex) {
            DiscordPS.error("Failed to fetch webhook entry for plot ID: " + plotID, ex);
            return;
        }

        String threadID = Long.toUnsignedString(entry.threadID());
        AvailableTags submitTag = AvailableTags.FINISHED;
        long tagID = submitTag.getTag().getIDLong();

        webhook.editThreadChannelTags(threadID, Set.of(tagID), true);

        webhook.getWebhookMessage(threadID, threadID, true).queue((optMessage) -> {
            optMessage.ifPresent((message) -> {
                List<MessageEmbed> embeds = message.getEmbeds();
                MessageEmbed initialMessage = embeds.getFirst();

                EmbedBuilder updated = PlotData.fromEmbed(initialMessage);

                Path mediaPath = DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/" + entry.ownerUUID());
                if(initialMessage.getThumbnail() != null) {
                    try {
                        File avatarFile = FileUtil.findImageFileByPrefix("avatar-image", mediaPath.toFile());

                        if (avatarFile != null && avatarFile.exists())
                            updated.setThumbnail("attachment://" + avatarFile.getName());
                        else updated.setThumbnail(initialMessage.getThumbnail().getUrl());

                    } catch (IOException ex) {
                        DiscordPS.error("Failed to set webhook avatar thumbnail", ex);
                    }
                }

                updated.addField(PlotData.makeStatusField(submitTag.toStatus()));
                updated.setColor(submitTag.getColor());

                WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                        .setEmbeds(Collections.singletonList(updated.build()))
                        .build();

                webhook.editInitialThreadMessage(threadID, data, true).queue((success) -> {
                    success.ifPresent((msg) -> {
                        try {
                            WebhookEntry.updateEntryStatus(entry.threadID(), submitTag.toStatus());
                        } catch (SQLException e) {
                            Notification.sendMessageEmbeds(new EmbedBuilder()
                                    .setTitle(":red_circle: Discord Plot-System Error")
                                    .setDescription("Failed update webhook entry in the database, "
                                            + "The plot ID #`" + entry.plotID() + "` "
                                            + "may not be tracked correctly by the system.")
                                    .setColor(Color.RED)
                                    .build()
                            );
                        }
                    });
                });
            });
        });


    }

    public void insertEntryToDatabase(WebhookEntry entry) {
        try {
            WebhookEntry.insertNewEntry(entry);
            DiscordPS.debug("Added plot to webhook database (Plot ID: " + entry.plotID() + ")");
        }
        catch (SQLException ex) {
            DiscordPS.error("Failed to insert new webhook entry to database: " + entry);

            Notification.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(":red_circle: Discord Plot-System Error")
                .setDescription("Failed to insert new webhook entry to database, "
                        + "The plot ID #`" + entry.plotID() + "` "
                        + "will appear on thread but cannot be edited by the system.")
                .setColor(Color.RED)
                .build()
            );
        }
    }

    public void onNewThreadCreated(@NotNull MessageReference sentMessage, @NotNull PlotData plotData) {
        Button plotLinkButton = Button.link(" https://www.google.com/maps/place/" + plotData.getGeoCoordinates(), "Google Map");

        ActionRow actionRow = (plotData.isOwnerHasDiscord())? ActionRow.of(
            Button.primary(
                Constants.NEW_PLOT_HELP_BUTTON.apply(
                    sentMessage.getMessageIdLong(),
                    plotData.getOwnerDiscord().orElseThrow().getIdLong(),
                    plotData.getPlot().plotID()),
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
    }

    public @NotNull File prepareAvatarFile(String playerUUID, String format) {
        // DataFolder/media/UUID/avatar-image.jpg
        Path mediaPath = DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/" + playerUUID);

        // Make player's directory if not exist
        if(mediaPath.toFile().mkdirs()) DiscordPS.debug("Created player media cache for UUID: " + mediaPath);


        return mediaPath.resolve("avatar-image." + format).toFile();
    }

    public boolean downloadAvatarToFile(String playerUUID, @NotNull File avatarFile, String format) {
        // Try download player's minecraft avatar
        try {
            // Download if not exist
            if(avatarFile.createNewFile()) {
                URL avatarURL = AvatarUtil.getAvatarUrl(playerUUID, 128, format);
                FileUtil.downloadFile(avatarURL, avatarFile);
            }
            return true;
        }
        catch (HttpRequest.HttpRequestException ex) {
            DiscordPS.error("Failed to download URL for player avatar image: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            DiscordPS.error("IO Exception occurred trying to read player media folder at: "
                    + avatarFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
        }
        return false;
    }

    @Deprecated
    public void fetchLatestPlot() {
        List<PlotEntry> plots;

        plots = PlotEntry.fetchSubmittedPlots();

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
