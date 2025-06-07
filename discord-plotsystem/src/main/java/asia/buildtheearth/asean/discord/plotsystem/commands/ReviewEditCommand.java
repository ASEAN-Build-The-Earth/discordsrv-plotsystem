package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.components.api.TextDisplay;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnReview;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.MemberOwnable;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import asia.buildtheearth.asean.discord.plotsystem.core.system.buttons.PlotFeedback;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.ReviewComponent;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import github.scarsz.discordsrv.util.SchedulerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.GREEN;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewEditCommand.*;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewCommand.DESC_COMMAND_EDIT;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewCommand.DESC_PLOT_ID;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.CommandMessage.PLOT_REVIEW_SEND;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.CommandMessage.PLOT_REVIEW_EDIT;

sealed class ReviewEditCommand extends AbstractReviewCommand permits ReviewSendCommand {

    public ReviewEditCommand(@NotNull String name, @NotNull String plotID) {
        super(name);

        this.setDescription(getLangManager().get(DESC_COMMAND_EDIT));

        this.addOption(OptionType.INTEGER, plotID, getLangManager().get(DESC_PLOT_ID), true);
    }

    @Override
    protected void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnReview interaction) {
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(interaction.getPlotID());

            // Existing entries detected
            if(!entries.isEmpty()) {
                // Plot already exist
                EntryMenuBuilder menu = new EntryMenuBuilder(EDIT_SELECTION_MENU.apply(interaction), interaction::setFetchOptions);

                entries.forEach(entry -> this.formatEntryOption(entry, menu::registerMenuOption));

                hook.sendMessageEmbeds(getEmbed(Constants.ORANGE, EMBED_REVIEW_INFO))
                    .addActionRow(menu.build())
                    .addActionRows(this.getSelectionRow(interaction))
                    .setEphemeral(true)
                    .queue();
            } // Entry does not exist, nothing to delete
            else this.queueEmbed(hook, getEmbed(Constants.ORANGE, EMBED_NOTHING_TO_EDIT));
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_GET_ERROR, ex.toString()));
        }
    }

    @Override
    public void onSelectionConfirm(@NotNull InteractionHook hook, @NotNull OnReview interaction) {
        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        String selected = interaction.getFetchOptions().getFirst().getValue();

        DiscordPS.info("Editing review by slash command event (entry ID: " + selected + ")");

        try {
            Checks.isSnowflake(selected);

            WebhookEntry entry = WebhookEntry.getByMessageID(Long.parseUnsignedLong(selected));

            if(entry == null) throw new SQLException("Entry does not exist for selected id: " + selected);

            interaction.setSelectedEntry(entry);

            // Discord owner does not exist
            if(entry.ownerID() == null) {
                ActionRow row = ActionRow.of(
                    Button.PUBLIC_CONTINUE.get(interaction),
                    Button.PUBLIC_CANCEL.get(interaction)
                );
                this.queueEmbed(hook, row, getEmbed(Constants.RED, EMBED_OWNER_NO_DISCORD));
                return;
            }

            if(hasExistingMedia(entry.plotID(), selected)) {
                ActionRow row = ActionRow.of(
                    Button.PREV_IMG_CLEAR.get(interaction),
                    Button.PREV_IMG_ADD.get(interaction)
                );
                this.queueEmbed(hook, row, getEmbed(Constants.ORANGE, EMBED_PREV_MEDIA_FOUND));
                return;
            }

            sendInstruction(hook, interaction);
        }
        catch (SQLException | IllegalArgumentException ex) {
            this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_GET_ERROR, ex.toString()));
        }
    }

    @Override
    public void onNewMessageConfirm(@NotNull Message message, @NotNull OnReview interaction) {
        WebhookEntry entry = interaction.getSelectedEntry();
        MessageChannel channel = message.getChannel();

        if(entry == null) {
            channel.sendMessageEmbeds(errorEmbed(MESSAGE_VALIDATION_ERROR)).queue();
            return;
        }

        // Get the previous review data
        MultipartBody previousData = null;

        try {
            WebhookDataBuilder.WebhookData reviewData = PlotFeedback.getFeedback(
                "## " + getLang(MESSAGE_PREVIOUS_REVIEW), entry.messageID()
            );
            previousData = reviewData.prepareRequestBody();
        }
        catch (SQLException ex) {
            channel.sendMessageEmbeds(errorEmbed(MESSAGE_SQL_GET_ERROR)).queue();
        }

        // Parse the new content and send it as preview
        String feedbackRaw = message.getContentRaw();

        WebhookDataBuilder.WebhookData reviewData = parseReviewContent(
                message,
                entry.messageID(),
                entry.plotID(),
                entry.status().toTag().getColor(),
                interaction::setAttachments
        );

        // Send the preview as raw rest-action to bypass outdated checks
        Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(channel.getId());
        MultipartBody requestBody = reviewData.prepareRequestBody();
        RestAction<Object> newReview = new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, requestBody);
        RestAction<Object> previousReview = new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, previousData);
        RestAction<?> previewAction = (previousData != null)? RestAction.allOf(previousReview, newReview) : newReview;

        this.sendReviewConfirmation(previewAction, channel, interaction)
            .thenAccept(success -> success.setNewReviewMessage(feedbackRaw));
    }

    @Override
    public void sendInstruction(@NotNull InteractionHook hook, @NotNull OnReview interaction) {

        WebhookEntry entryData = interaction.getSelectedEntry();

        if(entryData == null) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        MemberOwnable owner = new MemberOwnable(entryData.ownerUUID());
        String threadID = Long.toUnsignedString(entryData.threadID());
        LanguageFile.EmbedLang lang = getEmbed(EMBED_EDIT_INSTRUCTION);
        String title = lang.title().replace(Format.OWNER, owner.getOwnerMentionOrName()).replace(Format.THREAD_ID, threadID);
        String description = lang.description();
        ActionRow interactions = this.getSubmissionRow(interaction);
        MessageEmbed embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(Constants.BLUE)
                .build();

        hook.sendMessageEmbeds(embed).addActionRows(interactions).queue();
    }

    @Override
    public void clearPreviousReview(@NotNull InteractionHook hook, @NotNull OnReview interaction) {
        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        File mediaFolder = PlotData.prepareMediaFolder(interaction.getPlotID());
        String selected = interaction.getFetchOptions().getFirst().getValue();

        // Clear media folder for saving new attachment data
        if(mediaFolder.exists()) {
            Predicate<File> filter = file -> file.isFile() && FileUtil.getFilenameFromFile(file).endsWith(selected);

            Optional.ofNullable(mediaFolder.listFiles())
                    .ifPresent(files -> Arrays.stream(files).filter(filter)
                    .forEach(this.deleteMedia(hook)));
        }

        this.sendInstruction(hook, interaction);
    }

    @Override
    public void onReviewConfirm(@NotNull InteractionHook hook, @NotNull OnReview interaction) {
        if(interaction.getSelectedEntry() == null
        || interaction.getNewReviewMessage() == null
        || StringUtils.isBlank(interaction.getNewReviewMessage())) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        try {
            WebhookEntry.updateEntryFeedback(interaction.getSelectedEntry().messageID(), interaction.getNewReviewMessage());

            String threadID = Long.toUnsignedString(interaction.getSelectedEntry().threadID());

            Notification.notify(
                PLOT_REVIEW_EDIT,
                String.valueOf(interaction.getSelectedEntry().plotID()),
                hook.getInteraction().getUser().getId(),
                threadID
            );

            // Discord account is not linked
            if(interaction.getSelectedEntry().ownerID() == null) {
                interaction.getOptAttachments().ifPresentOrElse(attachments -> {
                    String eventID = Long.toUnsignedString(interaction.eventID);
                    this.saveAttachmentsToFile(hook, interaction, attachments, (uploaded, files) ->
                        this.sendReviewInThread(hook, interaction, eventID, uploaded, files), eventID);
                }, () -> this.sendReviewInThread(hook, interaction, null, null, null));

                return;
            }

            EmbedBuilder embed = this.getLangManager()
                .getEmbedBuilder(EMBED_REVIEW_EDIT_SUCCESS,
                    description -> description.replace(Format.THREAD_ID, threadID))
                .setColor(GREEN);

            interaction.getOptAttachments().ifPresentOrElse(attachments -> {
                String eventID = Long.toUnsignedString(interaction.eventID);
                String feedbackID = Long.toUnsignedString(interaction.getSelectedEntry().messageID());
                this.saveAttachmentsToFile(hook, interaction, attachments, (uploaded, files) -> this.queueEmbed(hook,
                        embed.addField(getLang(MESSAGE_SAVED_ATTACHMENTS), uploaded, false)
                    .build()
                ), eventID, feedbackID);
            }, () -> this.queueEmbed(hook, embed.build()));
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_SQL_SAVE_ERROR));
        }
    }

    @Override
    protected @NotNull ActionRow getSelectionRow(@NotNull OnReview payload) {
        return ActionRow.of(
                Button.SELECTION_CONFIRM.get(payload),
                Button.SELECTION_CANCEL.get(payload)
        );
    }

    @Override
    protected @NotNull ActionRow getSubmissionRow(@NotNull OnReview payload) {
        return ActionRow.of(
                Button.MESSAGE_CONFIRM.get(payload),
                Button.MESSAGE_CANCEL.get(payload)
        );
    }

    @Override
    protected @NotNull ActionRow getConfirmationRow(@NotNull OnReview payload) {
        return ActionRow.of(
                Button.EDIT_CONFIRM.get(payload),
                Button.EDIT_EDIT.get(payload),
                Button.EDIT_CANCEL.get(payload)
        );
    }

    @NotNull
    protected CompletableFuture<OnReview> sendReviewConfirmation(@NotNull RestAction<?> action,
                                                                 MessageChannel channel,
                                                                 OnReview interaction) {
        return action.submit().handle((ok, error) -> {
            if(error != null) {
                channel.sendMessageEmbeds(errorEmbed(MESSAGE_PREVIEW_ERROR, error.toString())).queue();
                DiscordPS.debug(error.toString());
            }

            ActionRow interactions = this.getConfirmationRow(interaction);

            String confirmation = this.getLang(MESSAGE_REVIEW_CONFIRMATION);

            channel
                .sendMessage(confirmation)
                .setActionRows(interactions)
                .queue();

            return interaction;
        });
    }



    protected void saveAttachmentsToFile(@NotNull InteractionHook hook,
                                         @NotNull OnReview interaction,
                                         @NotNull List<Message.Attachment> attachments,
                                         @NotNull BiConsumer<String, List<File>> onSuccess,
                                         @NotNull String... fileSuffix) {
        if(interaction.getSelectedEntry() == null) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        List<CompletableFuture<File>> download = new ArrayList<>();
        File folder = PlotData.prepareMediaFolder(interaction.getSelectedEntry().plotID());

        boolean canSave = false;
        if(!folder.exists()) {
            if(folder.mkdirs()) {
                canSave = true;
                this.queueEmbed(hook, getEmbed(GREEN, EMBED_MEDIA_FOLDER_CREATED));
            }
            else this.queueEmbed(hook, errorEmbed(getEmbed(EMBED_MEDIA_FOLDER_FAILED), folder.getAbsolutePath()));
        }
        else canSave = true;

        if(!canSave) return;

        for (int i = 0; i < attachments.size(); i++) {
            Message.Attachment file = attachments.get(i);
            if(file.getContentType() == null) continue;
            if(file.getContentType().startsWith("image/")) {
                StringBuilder filename = new StringBuilder();
                filename.append(Constants.PLOT_REVIEW_IMAGE_FILE);
                filename.append('-').append(i);
                for(String suffix : fileSuffix)
                    filename.append('-').append(suffix);
                download.add(file.downloadToFile(
                        folder.toPath().resolve(filename.toString() + '.' + file.getFileExtension()).toFile()
                ));
            }
        }

        Runnable onDownloaded = () -> {
            StringBuilder uploaded = new StringBuilder();
            List<File> tempFiles = new ArrayList<>(download.size());

            // Check downloaded integrity
            for (CompletableFuture<File> downloadedFile : download) {
                try {
                    File file = downloadedFile.get();
                    tempFiles.add(file);
                    uploaded.append("Saved `").append(file.getName()).append("`\n");

                } catch (InterruptedException | IllegalArgumentException | ExecutionException ex) {
                    DiscordPS.error("Failed to download file to resource", ex);
                    DiscordPS.error("Failed to download file to: " + folder.getAbsolutePath());
                    uploaded.append("Failed `").append(ex.getClass().getSimpleName()).append("`\n");
                }
            }

            onSuccess.accept(uploaded.toString(), tempFiles);
        };

        CompletableFuture.allOf(download.toArray(new CompletableFuture[0])).thenRun(onDownloaded);
    }

    protected void sendReviewInThread(@NotNull InteractionHook hook,
                                      @NotNull OnReview interaction,
                                      @Nullable String feedbackID,
                                      @Nullable String uploadedFile,
                                      @Nullable List<File> tempFiles) {
        if(interaction.getSelectedEntry() == null
                || interaction.getNewReviewMessage() == null
                || StringUtils.isBlank(interaction.getNewReviewMessage())) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        String threadID = Long.toUnsignedString(interaction.getSelectedEntry().threadID());

        CompletableFuture<?> feedbackAction = DiscordPS.getPlugin().getWebhook().sendFeedback(
                interaction.getPlotID(),
                interaction.getSelectedEntry(),
                interaction.getNewReviewMessage(),
                threadID,
                feedbackID
        );

        feedbackAction.whenComplete((ok, error) -> {
            if(error != null) this.queueEmbed(hook, errorEmbed(MESSAGE_FEEDBACK_SEND_ERROR, error.toString()));
            else {
                EmbedBuilder embed = this.getLangManager()
                    .getEmbedBuilder(
                        EMBED_REVIEW_SEND_SUCCESS,
                        description -> description.replace(Format.THREAD_ID, threadID)
                    ).setColor(GREEN);

                if(uploadedFile != null)
                    embed.addField(getLang(MESSAGE_SAVED_ATTACHMENTS), uploadedFile, false);

                this.queueEmbed(hook, embed.build());

                Notification.notify(
                    PLOT_REVIEW_SEND,
                    String.valueOf(interaction.getSelectedEntry().plotID()),
                    hook.getInteraction().getUser().getId(),
                    Long.toUnsignedString(interaction.getSelectedEntry().threadID())
                );
            }

            if(tempFiles != null) SchedulerUtil.runTaskLater(
                DiscordPS.getPlugin(),
                () -> tempFiles.forEach(this::deleteMedia),
                1200 // 1-minute delay before clearing temp file
            );
        });
    }

    @NotNull
    protected WebhookDataBuilder.WebhookData
    parseReviewContent(@NotNull Message message,
                       long messageID,
                       int plotID,
                       Color color,
                       Consumer<List<Message.Attachment>> attachmentReceiver) {

        List<Message.Attachment> attachments = message.getAttachments();
        String feedbackRaw = message.getContentRaw();
        String entryID = Long.toUnsignedString(messageID);

        Optional<List<File>> reviewMedia = ReviewComponent.getOptMedia(plotID, entryID);

        if(!attachments.isEmpty()) attachmentReceiver.accept(attachments);

        ReviewComponent component = new ReviewComponent(feedbackRaw, attachments, color, reviewMedia.orElse(null));

        TextDisplay title = new TextDisplay("## " + getLang(MESSAGE_NEW_REVIEW_PREVIEW));

        WebhookDataBuilder.WebhookData reviewData = new WebhookDataBuilder()
                .setComponentsV2(List.of(title, component.build()))
                .forceComponentV2()
                .suppressNotifications()
                .suppressMentions()
                .build();

        reviewMedia.ifPresent(files -> files.forEach(reviewData::addFile));

        return reviewData;
    }

    public boolean hasExistingMedia(int plotID, String entryID) {
        File folder = PlotData.prepareMediaFolder(plotID);

        if(!folder.exists()) return false;

        try {
            for(File file : FileUtil.findImagesFileByPrefix(Constants.PLOT_REVIEW_IMAGE_FILE, folder)) {
                if(FileUtil.getFilenameFromFile(file).endsWith(entryID))
                    return true;
            }
        }
        catch (IOException ex) {
            Notification.sendErrorEmbed("IOException trying to verify review media", ex.toString());
        }

        return false;
    }
}
