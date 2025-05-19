package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.events.PlotArchiveEvent;
import github.tintinkung.discordps.commands.interactions.InteractionEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotArchive;
import github.tintinkung.discordps.commands.interactions.OnPlotShowcase;
import github.tintinkung.discordps.commands.providers.AbstractPlotArchiveCommand;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.*;
import github.tintinkung.discordps.core.system.layout.InfoComponent;
import github.tintinkung.discordps.core.system.layout.Layout;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static github.tintinkung.discordps.Constants.PLOT_IMAGE_FILE;

import static github.tintinkung.discordps.core.system.PlotSystemThread.THREAD_NAME;

class PlotArchiveCommand extends AbstractPlotArchiveCommand {

    public PlotArchiveCommand(@NotNull String name, @NotNull String plotID, @NotNull String override) {
        super(name, "Archive a plot");

        this.addOption(
            OptionType.INTEGER,
            plotID,
            "The plot ID in integer to be archived",
            true);

        this.addOption(
            OptionType.BOOLEAN,
            override,
            "If false will create new thread for this archival plot",
            true);
    }

    @Override
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotArchive payload) {
        String threadID;
        Optional<WebhookEntry> existingPlot = WebhookEntry.ifPlotExisted(payload.getPlotID());

        if(existingPlot.isPresent())
            threadID = Long.toUnsignedString(existingPlot.get().threadID());
        else { // Entry does not exist, nothing to deleted
            this.queueEmbed(hook, NOTHING_TO_ARCHIVE);
            return;
        }

        this.queueEmbed(hook, ARCHIVE_SETTING_EMBED.apply(threadID, payload.plotID));

        // Non-overriding archive will go straight to thread creation
        if(!payload.override) {
            this.queueImagesOption(hook, payload);
            return;
        }

        // Override is true, fetch the thread layout to verify its component data
        CompletableFuture<Optional<Layout>> fetchLayout = DiscordPS.getPlugin().getWebhook().getThreadLayout(threadID);

        fetchLayout.whenComplete((optLayout, error) -> {
            if(error != null) {
                this.queueEmbed(hook, ON_LAYOUT_FAILED.apply(error.toString()));
                return;
            }

            optLayout.ifPresentOrElse(layout -> layout.getLayout().forEach(component -> {
                if (component instanceof InfoComponent infoComponent) {

                    File folder = PlotData.prepareMediaFolder(payload.getPlotID());
                    Optional<List<File>> optFiles = Optional.empty();

                    if(!folder.exists()) {
                        if(folder.mkdirs()) this.queueEmbed(hook, MEDIA_FOLDER_CREATED);
                        else this.queueEmbed(hook, MEDIA_FOLDER_FAILED.apply(folder.getAbsolutePath()));
                    }
                    else {
                        List<File> files = PlotData.checkMediaFolder(folder);
                        if(!files.isEmpty()) optFiles = Optional.of(files);
                    }


                    // Plot exist without images
                    if (infoComponent.getAttachments().isEmpty()) {
                        payload.setArchiveEntry(existingPlot.get());
                        this.queueImagesOption(hook, payload);
                        return;
                    }
                    else {
                        // Check for cached image that is not attached to the layout component
                        optFiles.map(files -> {
                            List<String> pending = new ArrayList<>();
                            files.forEach(file -> {
                                if(!infoComponent.getAttachments().contains(file.getName()))
                                    pending.add(file.getName());
                            });
                            return pending;
                        }).ifPresent(pendingFiles -> {
                            if(!pendingFiles.isEmpty())
                                this.queueEmbed(hook, CACHED_FILE_PENDING.apply(String.join(", ", pendingFiles)));
                        });
                    }

                    // Plot exist with some images
                    ActionRow options = ActionRow.of(
                        ARCHIVE_NOW_BUTTON.apply(payload),
                        ATTACH_IMAGES_BUTTON.apply(payload),
                        PROVIDE_IMAGES_BUTTON.apply(payload),
                        ARCHIVE_DISMISS_BUTTON.apply(payload)
                    );

                    payload.setPreviousAttachment(infoComponent.getAttachments());
                    payload.setArchiveEntry(existingPlot.get());
                    this.queueImagesOption(hook, options, payload, IMAGES_EXIST_EMBED);
                }
            }), () -> this.queueEmbed(hook, ON_LAYOUT_FAILED.apply("Thread layout returned empty, possibly an internal error occurred.")));
        });
    }

    @Override
    public void onImagesProvided(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction) {
        this.queueEmbed(hook,
            ActionRow.of(
                ARCHIVE_CONFIRM_BUTTON.apply(interaction),
                ARCHIVE_CANCEL_BUTTON.apply(interaction)),
            ARCHIVE_CONFIRMATION_EMBED.apply(interaction.override));
    }

    @Override
    public void onImagesAttached(@NotNull Message message, @NotNull OnPlotArchive interaction) {
        List<Message.Attachment> attachment = message.getAttachments();
        MessageChannel channel = message.getChannel();
        List<CompletableFuture<File>> download = new ArrayList<>();
        Optional<Set<String>> attachments = interaction.getPreviousAttachment();
        File mediaFolder = PlotData.prepareMediaFolder(interaction.getPlotID());

        // Clear media folder for saving new attachment data
        if(mediaFolder.exists()) {
            Optional.ofNullable(mediaFolder.listFiles())
                    .ifPresent(files -> Arrays.stream(files).filter(File::isFile)
                    .forEach(CACHED_FILE_DELETER.apply(channel)));
        } else { // Exit if media folder has not been initialized
            channel.sendMessageEmbeds(ARCHIVE_FAILED_OPTION).queue();
            return;
        }

        Function<String, File> fileLocation = fileName -> mediaFolder.toPath().resolve(fileName).toFile();

        // Queue the message embed instantly as loading
        CompletableFuture<Message> queueMessage = channel.sendMessageEmbeds(UPLOADING_ATTACHMENT).submit();

        // Download each attachment to plot's media folder
        int suffixName = attachments.map(Set::size).orElse(0);
        for (int i = 0; i < attachment.size(); i++) {
            Message.Attachment file = attachment.get(i);

            final String fileName = PLOT_IMAGE_FILE + "-" + (suffixName + i) + "." + file.getFileExtension();
            if(attachments.map(existing -> existing.contains(fileName)).orElse(false)) {
                // Cant determined what's file name is previously set, append it with snowflake id
                File filePath = fileLocation.apply(fileName + "-" + message.getId());

                download.add(file.downloadToFile(filePath));
            }
            else download.add(file.downloadToFile(fileLocation.apply(fileName)));
        }

        // Edit the queued embed to confirm button after attachment is saved
        Runnable onDownloaded = () -> {
            StringBuilder uploaded = new StringBuilder();
            RuntimeException failed = null;

            // Check downloaded integrity
            for (CompletableFuture<File> downloadedFile : download) {
                try {
                    File file = downloadedFile.get();
                    uploaded.append("Saved ").append(file.getName()).append("\n");

                } catch (InterruptedException | IllegalArgumentException | ExecutionException ex) {
                    DiscordPS.error("cannot download file to resource", ex);
                    DiscordPS.error("please upload it into plugin data folder: " + fileLocation.apply(PLOT_IMAGE_FILE));

                    failed = new RuntimeException(ex.toString());
                }
            }

            if(failed != null) {
                String failedReason = failed.getMessage();
                queueMessage.thenAccept(sent -> ON_IMAGE_DOWNLOAD_FAILED.accept(sent, failedReason));
                return;
            }

            queueMessage.thenAccept(sent -> {
                sent.editMessageEmbeds(ON_IMAGES_SAVED.apply(download.size(), uploaded.toString())).queue();
                channel.sendMessageEmbeds(ARCHIVE_CONFIRMATION_EMBED.apply(interaction.override))
                    .setActionRows(ActionRow.of(
                        ARCHIVE_CONFIRM_BUTTON.apply(interaction),
                        ARCHIVE_CANCEL_BUTTON.apply(interaction)))
                    .queue();
            });
        };

        // Run the stage
        CompletableFuture.allOf(download.toArray(new CompletableFuture[0])).thenRun(onDownloaded);
    }

    @Override
    public void onConfirmArchive(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction) {

        PlotArchiveEvent event = new PlotArchiveEvent(interaction.getPlotID(), "<@" + hook.getInteraction().getUser().getId() + '>');
        Consumer<Message> onSuccess = defer -> {
            defer.editMessageEmbeds(SUCCESSFULLY_CREATED).queue();

            if(DiscordPS.getPlugin().getShowcase() != null) {
                // If showcase channel exist
                this.queueEmbed(hook,
                    ActionRow.of(SHOWCASE_PLOT_BUTTON.apply(interaction), SHOWCASE_DISMISS_BUTTON.apply(interaction)),
                    SHOWCASE_OPTION_EMBED
                );
            }
        };

        // Queue the message embed instantly as loading
        hook.sendMessageEmbeds(ARCHIVING_PLOT).queue(defer -> {
            if(!interaction.override)
                this.archiveAsNewThread(
                    DiscordPS.getPlugin().getWebhook()::forceRegisterNewPlot,
                    event,
                    ok -> onSuccess.accept(defer),
                    failed -> defer.editMessageEmbeds(ON_CREATE_ERROR.apply(failed.toString())).queue()
                );
            else {
                if(interaction.getArchiveEntry().isEmpty()) {
                    defer.editMessageEmbeds(ARCHIVE_FAILED_OPTION).queue();
                    return;
                }

                PlotSystemThread.UpdateAction action = new PlotSystemThread.UpdateAction(
                        interaction.getPlotID(),
                        interaction.getArchiveEntry().get(),
                        Long.toUnsignedString(interaction.getArchiveEntry().get().messageID()),
                        Long.toUnsignedString(interaction.getArchiveEntry().get().threadID())
                );

                BiConsumer<PlotSystemThread.UpdateAction, ? super Throwable> handler = (ok, error) -> {
                    if(error != null) defer.editMessageEmbeds(ON_CREATE_ERROR.apply(error.toString())).queue();
                    else onSuccess.accept(defer);
                };

                DiscordPS.getPlugin()
                    .getWebhook()
                    .updatePlot(action, event, ThreadStatus.archived)
                    .whenComplete(handler);
            }
        });
    }

    @Override
    public void onShowcaseConfirmed(InteractionHook hook, OnPlotArchive interaction) {
        // Manually trigger showcase command by redirecting current interaction
        final Consumer<InteractionEvent> commandRedirect = manager -> {
            OnPlotShowcase payload = new OnPlotShowcase(interaction.userID, interaction.eventID, interaction.plotID);
            manager.putPayload(interaction.eventID, payload);
            manager.fromClass(PlotCommand.class)
                    .getShowcaseCommand()
                    .trigger(hook, payload);
        };

        DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
        DiscordPS.getPlugin().getOptInteractionProvider().ifPresentOrElse(
            commandRedirect,
            () -> this.queueEmbed(hook, ON_SHOWCASE_ERROR)
        );
    }


    private void archiveAsNewThread(@NotNull Function<PlotSystemThread, CompletableFuture<Void>> creator,
                                    @NotNull PlotArchiveEvent event,
                                    @NotNull Consumer<Void> onSuccess,
                                    @NotNull Consumer<Throwable> onFailure) {
        PlotEntry plot = PlotEntry.getByID(event.getPlotID());

        if(plot == null) {
            onFailure.accept(PLOT_FETCH_RETURNED_NULL);
            return;
        } else if (plot.ownerUUID() == null) {
            onFailure.accept(PLOT_FETCH_UNKNOWN_OWNER);
            return;
        }

        PlotSystemThread thread = makeArchiveThread(event, plot);

        CompletableFuture<Void> createAction =  creator.apply(thread);

        if(createAction == null) onFailure.accept(PLOT_CREATE_RETURNED_NULL);
        else createAction.whenComplete((ok, error) -> {
            if(error != null) onFailure.accept(error);
            else onSuccess.accept(ok);
        });
    }

    private static @NotNull PlotSystemThread makeArchiveThread(PlotArchiveEvent event, PlotEntry plot) {
        PlotSystemThread thread = new PlotSystemThread(event.getPlotID());

        thread.setProvider(data -> {
            PlotData plotData = new PlotData(plot);
            plotData.setPrimaryStatus(ThreadStatus.archived, true);
            return plotData;
        });

        thread.setApplier(owner -> "[Archived] " + THREAD_NAME.apply(event.getPlotID(), owner.formatOwnerName()));

        thread.setModifier((owner, info) -> info.addHistory(event));

        return thread;
    }
}
