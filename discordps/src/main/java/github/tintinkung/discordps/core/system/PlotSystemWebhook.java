package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.IMentionable;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.util.TimeUtil;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.events.PlotEvent;
import github.tintinkung.discordps.api.events.PlotFeedbackEvent;
import github.tintinkung.discordps.api.events.PlotSubmitEvent;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.providers.WebhookProviderImpl;
import github.tintinkung.discordps.core.system.layout.InfoComponent;
import github.tintinkung.discordps.core.system.layout.StatusComponent;
import github.tintinkung.discordps.core.system.embeds.StatusEmbed;
import github.tintinkung.discordps.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static github.tintinkung.discordps.Debug.Warning.RUNTIME_SQL_EXCEPTION;

public final class PlotSystemWebhook {
    private final ForumWebhookImpl webhook;

    public PlotSystemWebhook(ForumWebhookImpl webhook) {
        this.webhook = webhook;
    }

    public WebhookProviderImpl getProvider() {
        return this.webhook.getProvider();
    }

    public void addNewPlot(int plotID) {
        // Check if plot already been created by the system
        try {
            if(!WebhookEntry.getByPlotID(plotID).isEmpty()) {
                DiscordPS.info("Trying to create new plot entry "
                        + "but the plot has already been added to the webhook database "
                        + "(Plot ID: " + plotID + ")");
                return;
            }
        }
        catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.warning("Cannot verify plot create event because a runtime SQL exception occurred!");
        }

        PlotEntry plot = PlotEntry.getByID(plotID);

        if(plot == null) {
            DiscordPS.warning("Failed to fetch plot data from plot-system database!");
            Notification.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(":red_circle: Discord Plot-System Error")
                .setDescription("Runtime exception creating new plot thread, "
                        + "The plot ID #`" + plotID + "` will not be tracked by the system.")
                .setColor(Color.RED)
                .build()
            );
            return;
        }

        // Prepare plot data
        PlotData plotData = new PlotData(plot);
        InfoComponent infoComponent = new InfoComponent(0, plotData);
        StatusComponent statusComponent = new StatusComponent(1, plotData);

        // Initial history, more will be added dynamically via event type
        infoComponent.addHistory(plotData.getOwnerMentionOrName() + " created the plot <t:" + Instant.now().getEpochSecond() + ":R>");

        // Create Webhook Data
        String threadName = "Plot #" + plot.plotID();

        WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                .setThreadName(threadName)
                .setComponentsV2(List.of(infoComponent.build(), statusComponent.build()))
                .forceComponentV2()
                .build();

        // Attach files
        plotData.getAvatarFile().ifPresent(data::addFile);
        if(!plotData.getImageFiles().isEmpty()) plotData.getImageFiles().forEach(data::addFile);

        final Consumer<? super Throwable> onFailure = (failed) -> {
            throw new RuntimeException(failed);
        };

        final Consumer<Optional<MessageReference>> onSuccess = opt -> {
            MessageReference initialMsg = opt.orElseThrow();

            Button helpButton = Button.primary(
                    Constants.NEW_PLOT_HELP_BUTTON.apply(
                            initialMsg.getMessageIdLong(),
                            plotData.getOwnerDiscord().orElseThrow().getIdLong(),
                            plotData.getPlot().plotID()),
                    "Help"
            );
            Button docsButton = Button.link(
                    "https://asean.buildtheearth.asia/intro/getting-started/building-first-build/plot-system",
                    "Documentation"
            );

            // Add interactive help & docs button (only docs if the system failed to resolve user's discord account)
            ActionRow interactions = (plotData.isOwnerHasDiscord())? ActionRow.of(helpButton, docsButton) : ActionRow.of(docsButton);

            WebhookDataBuilder.WebhookData status = new WebhookDataBuilder()
                .setEmbeds(Collections.singletonList(new StatusEmbed(plotData.getStatus()).build()))
                .setComponents(Collections.singletonList(interactions))
                .build();

            // Send status interaction embed
            // This is where the user can interact with etc. "Help" button
            webhook.sendMessageInThread(initialMsg.getMessageId(), status, false, true)
                .queue(optMsg -> {
                    MessageReference message = optMsg.orElseThrow();
                    WebhookEntry.insertNewEntry(message.getMessageIdLong(),
                            initialMsg.getMessageIdLong(),
                            plotID,
                            plotData.getStatus(),
                            plot.ownerUUID()
                    );
                });

            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":hammer_pick: New Plot Created at <#" + initialMsg.getMessageId() + ">")
                    .setDescription("By " + plotData.getOwnerMentionOrName() + " (Plot ID: " + plotData.getPlot().plotID() + ")")
                    .setColor(Color.GREEN)
                    .build());
        };

        try {
            webhook.newThreadFromWebhook(data, plotData.getStatusTags(), true, true).queue(onSuccess, onFailure);
        } catch (RuntimeException ex) {
            DiscordPS.error("Failed to send webhook: " + ex);

            Notification.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(":red_circle: Discord Plot-System Error")
                .setDescription("Runtime exception creating new plot thread, "
                        + "The plot ID #`" + plotID + "` "
                        + "will may or may not be tracked by the system depending on the error.")
                .addField("Error", "```" + ex.toString() + "```", false)
                .setColor(Color.RED)
                .build()
            );
        };
    }

    public void onFeedbackSet(@NotNull PlotFeedbackEvent event) {
        WebhookEntry entry;
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(event.getPlotID());
            if(entries.isEmpty()) throw new SQLException("Entry for plot ID: " + event.getPlotID() + " Does not exist.");
            // TODO: a thread may have multiple plot entry
            entry = entries.getFirst();
        }
        catch (SQLException ex) {
            DiscordPS.error("Failed to fetch webhook entry for plot ID: " + event.getPlotID(), ex);
            DiscordPS.warning("Skipping plot update event " + event.getClass().getSimpleName());
            return;
        }
        String threadID = Long.toUnsignedString(entry.threadID());
        String messageID = Long.toUnsignedString(entry.messageID());

        DiscordPS.debug("Feedback Event: " + event.getFeedback());

        // Update feedback data entry
        try {
            WebhookEntry.updateEntryFeedback(entry.messageID(), event.getFeedback());
        }
        catch (SQLException ex) {
            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Failed update webhook feedback in the database, "
                            + "The plot ID #`" + entry.plotID() + "` "
                            + "feedback button will not be functional, the user will not be able to view it.")
                    .addField("Error", "```" + ex.toString() + "```", false)
                    .setColor(Color.RED)
                    .build()
            );
        }

        // Attach feedback button to the plot's message
        webhook.getWebhookMessage(threadID, messageID, true).queue(opt -> opt.ifPresent(message -> {

            if(message.getActionRows().isEmpty()) return;

            // Clone our component button and add a new, feedback button
            ActionRow componentRow = message.getActionRows().getFirst();
            List<Button> buttons = new ArrayList<>();

            componentRow.getButtons().forEach((button) -> {
                // Add all sent button back
                if(button == null) return;
                else buttons.add(button);

                // Parse for data in help button
                try {
                    ComponentUtil.PluginButton component = new ComponentUtil.PluginButton(button);

                    // Clone setting from a previously attached help button
                    // since this button's existence also confirms user owner in its ID
                    if(component.getType().equals(Constants.HELP_BUTTON)) {
                        buttons.add(Button.success(Constants.NEW_FEEDBACK_BUTTON.apply(
                            message.getIdLong(),
                            component.getUserIDLong(),
                            event.getPlotID()),
                            "View Feedback")
                        );

                        // Reply and ping user that their plot got reviewed
                        message.replyEmbeds(new EmbedBuilder()
                                .setTitle(":yellow_circle: Your plot has been reviewed!")
                                .setDescription("click the **View Feedback** button above to view the your feedback.")
                                .setColor(AvailableTags.FINISHED.getColor())
                                .setFooter("Updated")
                                .setTimestamp(Instant.now())
                                .build()
                        )
                        .content("<@" + component.getUserID() + ">")
                        .allowedMentions(Collections.singletonList(Message.MentionType.USER))
                        .queue();
                    }
                }
                catch (IllegalArgumentException ignored) { }
            });

            ActionRow updatedRow = ActionRow.of(buttons);

            WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                    .setComponents(Collections.singletonList(updatedRow))
                    .build();

            webhook.editThreadMessage(threadID, messageID, data, true).queue();
        }));
    }

    public <T extends PlotEvent> void updatePlot(@NotNull T event, @NotNull ThreadStatus status) {
        WebhookEntry entry;
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(event.getPlotID());
            if(entries.isEmpty()) throw new SQLException("Entry for plot ID: " + event.getPlotID() + " Does not exist.");
            entry = entries.getFirst();
        }
        catch (SQLException ex) {
            DiscordPS.error("Failed to fetch webhook entry for plot ID: " + event.getPlotID());
            DiscordPS.warning("Skipping plot update event " + event.getClass().getSimpleName());
            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Runtime exception **updating** new plot thread, "
                            + "The plot ID #`" + event.getPlotID() + "` "
                            + "will may or may not be tracked by the system depending on the error.")
                    .addField("Error", "```" + ex.toString() + "```", false)
                    .setColor(Color.RED)
                    .build()
            );
            return;
        }

        String threadID = Long.toUnsignedString(entry.threadID());
        String messageID = Long.toUnsignedString(entry.messageID());

        if(event instanceof PlotSubmitEvent) {
            Notification.sendMessageEmbeds("<@501366655624937472> <@480350715735441409> <@728196906395631637> <@939467710247604224> <@481786697860775937>",
                new EmbedBuilder()
                    .setTitle(":bell: Plot #" + entry.plotID() + " Has just submitted <t:" + Instant.now().getEpochSecond() + ":R>")
                    .setDescription("Tracker: <#" + threadID + ">")
                    .setColor(Color.CYAN)
                    .build()
            );
        }

        AvailableTags tag = status.toTag();
        long tagID = tag.getTag().getIDLong();

        // Edit channel tags
        webhook.editThreadChannelTags(threadID, Set.of(tagID), true).queue();

        // Edit status embed
        webhook.getWebhookMessage(threadID, messageID, true).queue(opt -> opt.ifPresent(message -> {
            // Status embed must be replaced on update
            StatusEmbed statusEmbed = new StatusEmbed(tag.toStatus());

            WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                    .setEmbeds(Collections.singletonList(statusEmbed.build()))
                    .build();

            webhook.editThreadMessage(threadID, messageID, data, true).queue(success -> success.ifPresent((msg) -> {
                try {
                    WebhookEntry.updateThreadStatus(entry.threadID(), tag.toStatus());
                } catch (SQLException ex) {
                    Notification.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle(":red_circle: Discord Plot-System Error")
                            .setDescription("Failed update webhook entry in the database, "
                                    + "The plot ID #`" + entry.plotID() + "` "
                                    + "may not be tracked correctly by the system.")
                            .addField("Error", "```" + ex.toString() + "```", false)
                            .setColor(Color.RED)
                            .build()
                    );
                }
            }));
        }));

        // Edit components
        webhook.getInitialLayout(threadID, true).queue(opt -> opt.ifPresent(component -> {

            WebhookDataBuilder.WebhookData updatedData = new WebhookDataBuilder()
                .forceComponentV2()
                .setComponentsV2(component.getLayout().stream().map(layout -> {
                    switch (layout) {
                        case InfoComponent infoComponent:
                            infoComponent.addHistory(event);
                            infoComponent.setAccentColor(tag.getColor());
                            break;
                        case StatusComponent statusComponent:
                            statusComponent.setAccentColor(tag.getColor());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + layout);
                    }

                    return layout.build();
                }).toList())
                .build();

            webhook.editInitialThreadMessage(threadID, updatedData, true).queue();
        }));
    }

    @Deprecated
    public void attachInteractionButtons(@NotNull MessageReference sentMessage, @NotNull PlotData plotData) {
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

        Notification.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(":hammer_pick: New Plot Created at <#" + sentMessage.getMessageId() + ">")
                .setDescription("By " + plotData.getOwnerMentionOrName() + " (Plot ID: " + plotData.getPlot().plotID() + ")")
                .setColor(Color.GREEN)
                .build());
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
                URL avatarURL = AvatarUtil.getAvatarUrl(plot.ownerUUID(), 16, avatarFormat);
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
