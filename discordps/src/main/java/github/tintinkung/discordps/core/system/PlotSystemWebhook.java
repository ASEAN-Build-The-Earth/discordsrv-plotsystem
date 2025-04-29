package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.ReceivedMessage;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.util.TimeUtil;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.events.*;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.providers.WebhookProviderImpl;
import github.tintinkung.discordps.core.system.components.ComponentV2;
import github.tintinkung.discordps.core.system.layout.InfoComponent;
import github.tintinkung.discordps.core.system.layout.StatusComponent;
import github.tintinkung.discordps.core.system.embeds.StatusEmbed;
import github.tintinkung.discordps.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static github.tintinkung.discordps.Debug.Warning.RUNTIME_SQL_EXCEPTION;

public final class PlotSystemWebhook {

    public static class UpdateAction {
        final int plotID;
        final @NotNull String messageID;
        final @NotNull String threadID;
        final @NotNull WebhookEntry entry;

        public UpdateAction(int plotID, @NotNull WebhookEntry entry, @NotNull String messageID, @NotNull String threadID) {
            this.plotID = plotID;
            this.entry = entry;
            this.messageID = messageID;
            this.threadID = threadID;
        }
    }

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
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(plotID);
            if(!entries.isEmpty()) {
                DiscordPS.info("Trying to create new plot entry "
                        + "but the plot has already been added to the webhook database "
                        + "(Plot ID: " + plotID + ")");

                this.addNewExistingPlot(entries.getFirst());

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
            // Push new plot entry as the initial message ID as it is the same as new thread ID
            MessageReference initialMsg = opt.orElseThrow();
            this.registerNewPlot(plotData, initialMsg.getMessageIdLong(), plotID);
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

    //TODO: make this more flexible
    public void createNewPlotThread(
            String threadName,
            PlotData plotData,
            Collection<? extends ComponentV2> componentsV2,
            Consumer<Optional<MessageReference>> onSuccess,
            Consumer<? super Throwable> onFailure) {

        WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                .setThreadName(threadName)
                .setComponentsV2(componentsV2)
                .forceComponentV2()
                .build();

        // Attach files
        plotData.getAvatarFile().ifPresent(data::addFile);
        if(!plotData.getImageFiles().isEmpty()) plotData.getImageFiles().forEach(data::addFile);

        try {
            this.webhook.newThreadFromWebhook(data, plotData.getStatusTags(), true, true).queue(onSuccess, onFailure);
        } catch (RuntimeException ex) {
            DiscordPS.error("Failed to send webhook: " + ex);

            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Runtime exception creating new plot thread, "
                            + "The plot ID #`" + plotData.getPlot().plotID() + "` "
                            + "will may or may not be tracked by the system depending on the error.")
                    .addField("Error", "```" + ex.toString() + "```", false)
                    .setColor(Color.RED)
                    .build()
            );
        };
    }


    public void addNewExistingPlot(@NotNull WebhookEntry entry) {

        // TODO: check reclaim reason
        ThreadStatus status = entry.status();

        PlotEntry plot = PlotEntry.getByID(entry.plotID());

        if(plot == null) {
            DiscordPS.warning("Failed to fetch plot data from plot-system database!");
            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Runtime exception creating new plot thread, "
                            + "The plot ID #`" + entry.plotID() + "` will not be tracked by the system.")
                    .setColor(Color.RED)
                    .build()
            );
            return;
        }

        // Prepare plot data
        PlotData plotData = new PlotData(plot);

        AvailableTags tag = plotData.getStatus().toTag();
        long tagID = tag.getTag().getIDLong();
        String threadID = Long.toUnsignedString(entry.threadID());

        // Edit channel tags
        webhook.editThreadChannelTags(threadID, Set.of(tagID), true).queue();

        // Edit components
        webhook.getInitialLayout(threadID, true).queue(opt -> opt.ifPresent(component -> {

            List<ComponentV2> updated = new ArrayList<>();
            int latestPosition = 1;

            for(LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>> layout : component.getLayout()) {
                switch (layout) {
                    case InfoComponent infoComponent:
                        infoComponent.addHistory("<t:" + Instant.now().getEpochSecond() + ":D> • Plot is reclaimed by " + plotData.getOwnerMentionOrName());
                        infoComponent.setAccentColor(tag.getColor());
                        break;
                    case StatusComponent statusComponent:
                        // TODO: embed re-claimed reason into old status message
                        statusComponent.setAccentColor(tag.getColor());
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + layout);
                }
                latestPosition += layout.getLayoutPosition();
                updated.add(layout.build());
            }

            // Add new claim owner to the status
            updated.add(new StatusComponent(latestPosition + 1, plotData).build());

            WebhookDataBuilder.WebhookData updatedData = new WebhookDataBuilder()
                    .forceComponentV2()
                    .setComponentsV2(updated)
                    .build();

            webhook.editInitialThreadMessage(threadID, updatedData, true).queue();
        }));

        // Prepare status data
        this.registerNewPlot(plotData, entry.threadID(), entry.plotID());
    }

    public void registerNewPlot(PlotData plotData, long threadIDLong, int plotID) {
        String threadID = Long.toUnsignedString(threadIDLong);

        // Prepare status data
        Button helpButton = Button.primary(
                Constants.NEW_PLOT_HELP_BUTTON.apply(
                        threadIDLong,
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

        WebhookDataBuilder.WebhookData statusData = new WebhookDataBuilder()
                .setEmbeds(Collections.singletonList(new StatusEmbed(plotData.getStatus()).build()))
                .setComponents(Collections.singletonList(interactions))
                .build();

        // Send status interaction embed
        // This is where the user can interact with etc. "Help" button
        webhook.sendMessageInThread(threadID, statusData, false, true)
                .queue(optMsg -> {
                    MessageReference message = optMsg.orElseThrow();
                    WebhookEntry.insertNewEntry(message.getMessageIdLong(),
                            threadIDLong,
                            plotID,
                            plotData.getStatus(),
                            plotData.getPlot().ownerUUID()
                    );
                });

        Notification.sendMessageEmbeds(new EmbedBuilder()
                .setTitle(":hammer_pick: New Plot Created at <#" + threadID + ">")
                .setDescription("By " + plotData.getOwnerMentionOrName() + " (Plot ID: " + plotData.getPlot().plotID() + ")")
                .setColor(Color.GREEN)
                .build());
    }

    public <T extends PlotReviewEvent> void onPlotReview(@NotNull T event) {
        switch (event) {
            case PlotApprovedEvent approved: {
                UpdateAction action = this.updatePlot(approved, ThreadStatus.approved, (message, user) -> {
                    // Reply and ping user that their plot got reviewed
                    message.replyEmbeds(new EmbedBuilder()
                            .setTitle(":green_circle: Your plot has been **approved**!")
                            .setDescription("We will notify your review feedback by the button below.")
                            .setColor(AvailableTags.APPROVED.getColor())
                            .setFooter("Updated")
                            .setTimestamp(Instant.now())
                            .build()
                        ).content(user != null? "<@" + user + ">" : null)
                        .allowedMentions(Collections.singletonList(Message.MentionType.USER))
                        .queue();
                });
                this.attachFeedbackButton(ButtonStyle.SUCCESS, "No Feedback Yet", action);
                return;
            }
            case PlotRejectedEvent rejected: {
                UpdateAction action = this.updatePlot(rejected, ThreadStatus.approved, (message, user) -> {
                    // Reply and ping user that their plot got reviewed
                    message.replyEmbeds(new EmbedBuilder()
                            .setTitle(":red_circle: Your plot has been **rejected**!")
                            .setDescription("We will notify the rejected reason by the button below, " +
                                    "you can re-submit your plot again to get an approval.")
                            .setColor(AvailableTags.REJECTED.getColor())
                            .setFooter("Updated")
                            .setTimestamp(Instant.now())
                            .build()
                        ).content(user != null? "<@" + user + ">" : null)
                        .allowedMentions(Collections.singletonList(Message.MentionType.USER))
                        .queue();
                });
                this.attachFeedbackButton(ButtonStyle.DANGER, "No Reason Yet", action);
                return;
            }
            case PlotFeedbackEvent feedback: {
                this.setFeedback(feedback.getFeedback(), createAction(event));
                return;
            }
            default: throw new IllegalStateException("Illegal PlotReviewEvent: " + event);
        }
    }

    public void setFeedback(String feedback, @Nullable UpdateAction action) {
        if(action == null) return;

        // TODO: plot-system hard-coded this string, maybe find a better way for this
        if(Objects.equals(feedback, "No Feedback")) return;

        // Attach feedback button to the plot's message
        webhook.getWebhookMessage(action.threadID, action.messageID, true).queue(opt -> opt.ifPresent(message -> {

            if(message.getActionRows().isEmpty()) return;

            // Clone our component button and add a new, feedback button
            ActionRow componentRow = message.getActionRows().getFirst();
            List<Button> buttons = new ArrayList<>();

            componentRow.getButtons().forEach((button) -> {
                // Add all sent button back
                if(button == null) return;

                // Parse for data in help button
                try {
                    ComponentUtil.PluginButton component = new ComponentUtil.PluginButton(button);

                    // If feedback button is already attached
                    // update it to functional button
                    // (the initial feedback button is set as "No Feedback"
                    if (component.getType().equals(Constants.FEEDBACK_BUTTON)) {

                        String label = button.getLabel();

                        buttons.add(switch (button.getStyle()) {
                            case DANGER -> button.withLabel(label = "Show Reason").asEnabled();
                            case SUCCESS -> button.withLabel(label = "View Feedback").asEnabled();
                            default -> button.asDisabled();
                        });

                        // Reply and ping user that their plot got reviewed
                        message.replyEmbeds(new EmbedBuilder()
                                .setTitle(":yellow_circle: Your plot has been reviewed!")
                                .setDescription("click the **" + label + "** button above to view the your feedback.")
                                .setColor(AvailableTags.FINISHED.getColor())
                                .setFooter("Updated")
                                .setTimestamp(Instant.now())
                                .build()
                            ).content("<@" + component.getUserID() + ">")
                            .allowedMentions(Collections.singletonList(Message.MentionType.USER))
                            .queue();
                        return;
                    }
                }
                catch (IllegalArgumentException ignored) { }

                buttons.add(button);
            });

            ActionRow updatedRow = ActionRow.of(buttons);

            WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                    .setComponents(Collections.singletonList(updatedRow))
                    .build();

            webhook.editThreadMessage(action.threadID, action.messageID, data, true).queue();
        }));

        // Update feedback data entry
        try {
            WebhookEntry.updateEntryFeedback(action.entry.messageID(), feedback);
        }
        catch (SQLException ex) {
            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Failed update webhook feedback in the database, "
                            + "The plot ID #`" + action.entry.plotID() + "` "
                            + "feedback button will not be functional, the user will not be able to view it.")
                    .addField("Error", "```" + ex.toString() + "```", false)
                    .setColor(Color.RED)
                    .build()
            );
        }
    }

    public void attachFeedbackButton(ButtonStyle style, String label, @Nullable UpdateAction action) {
        if(action == null) return;

        // Attach feedback button to the plot's message
        webhook.getWebhookMessage(action.threadID, action.messageID, true).queue(opt -> opt.ifPresent(message -> {

            if(message.getActionRows().isEmpty()) return;

            // Clone our component button and add a new, feedback button
            ActionRow componentRow = message.getActionRows().getFirst();
            List<Button> buttons = new ArrayList<>();

            componentRow.getButtons().forEach((button) -> {
                // Add all sent button back
                if(button == null) return;

                // Parse for data in help button
                try {
                    ComponentUtil.PluginButton component = new ComponentUtil.PluginButton(button);

                    switch (component.getType()) {
                        // If feedback button is already attached
                        case Constants.FEEDBACK_BUTTON: return;
                        // Feedback button is not yet attached:
                        // Clone setting from a previously attached help button
                        // since this button's existence also confirms user owner in its ID
                        case Constants.HELP_BUTTON: buttons.add(
                            Button.of(style, Constants.NEW_FEEDBACK_BUTTON.apply(
                                message.getIdLong(),
                                component.getUserIDLong(),
                                action.plotID),
                            label).asDisabled()
                        );
                    }
                }
                catch (IllegalArgumentException ignored) { }

                buttons.add(button);
            });

            ActionRow updatedRow = ActionRow.of(buttons);

            WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                    .setComponents(Collections.singletonList(updatedRow))
                    .build();

            webhook.editThreadMessage(action.threadID, action.messageID, data, true).queue();
        }));
    }

    private <T extends PlotEvent> @Nullable UpdateAction createAction(@NotNull T event) {
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
            return null;
        }

        String threadID = Long.toUnsignedString(entry.threadID());
        String messageID = Long.toUnsignedString(entry.messageID());

        return new UpdateAction(event.getPlotID(), entry, messageID, threadID);
    }

    public <T extends PlotEvent> UpdateAction updatePlot(@NotNull T event, @NotNull ThreadStatus status, BiConsumer<ReceivedMessage, String> onSuccess) {
        UpdateAction action = createAction(event);

        if(action == null) return null;

        if(event instanceof PlotSubmitEvent) {
            Notification.sendMessageEmbeds("<@501366655624937472> <@480350715735441409> <@728196906395631637> <@939467710247604224> <@481786697860775937>",
                new EmbedBuilder()
                    .setTitle(":bell: Plot #" + action.plotID + " Has just submitted <t:" + Instant.now().getEpochSecond() + ":R>")
                    .setDescription("Tracker: <#" + action.threadID + ">")
                    .setColor(Color.CYAN)
                    .build()
            );
        }

        String messageID = action.messageID;
        String threadID = action.threadID;
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

            webhook.editThreadMessage(threadID, messageID, data, true).queue(success -> success.ifPresent((ignored) -> {
                try {
                    WebhookEntry.updateThreadStatus(action.entry.threadID(), tag.toStatus());
                } catch (SQLException ex) {
                    Notification.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle(":red_circle: Discord Plot-System Error")
                            .setDescription("Failed update webhook entry in the database, "
                                    + "The plot ID #`" + action.entry.plotID() + "` "
                                    + "may not be tracked correctly by the system.")
                            .addField("Error", "```" + ex.toString() + "```", false)
                            .setColor(Color.RED)
                            .build()
                    );
                }
            }));

            // Check for user that own this entry
            if(message.getActionRows().isEmpty()) return;

            // Clone our component button and add a new, feedback button
            ActionRow componentRow = message.getActionRows().getFirst();
            String userID = null;

            for(Button button : componentRow.getButtons()) {
                // Add all sent button back
                if(button == null) return;

                // Parse for data in help button
                try {
                    ComponentUtil.PluginButton component = new ComponentUtil.PluginButton(button);
                    if(component.getType().equals(Constants.HELP_BUTTON))
                        userID = component.getUserID();
                }
                catch (IllegalArgumentException ignored) { }
            }

            onSuccess.accept(message, userID);
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
                            if(event instanceof PlotAbandonedEvent) {
                                statusComponent.setStatusMessage("""
                                    ## Abandoned by {owner}\
                        
                                    this plot is *abandoned*.\
                        
                                    Anyone can re-claim this plot to continue the progress."""
                                );
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + layout);
                    }

                    return layout.build();
                }).toList())
                .build();

            webhook.editInitialThreadMessage(threadID, updatedData, true).queue();
        }));

        return action;
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
