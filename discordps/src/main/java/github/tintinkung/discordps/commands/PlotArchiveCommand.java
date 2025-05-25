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
import github.tintinkung.discordps.core.system.io.lang.LangPaths;
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
import static github.tintinkung.discordps.Constants.GREEN;
import static github.tintinkung.discordps.Constants.ORANGE;

import static github.tintinkung.discordps.core.system.PlotSystemThread.THREAD_NAME;
import static github.tintinkung.discordps.core.system.io.lang.PlotArchiveCommand.*;
import static github.tintinkung.discordps.core.system.io.lang.Notification.CommandMessage;

class PlotArchiveCommand extends AbstractPlotArchiveCommand {

    public PlotArchiveCommand(@NotNull String name, @NotNull String plotID, @NotNull String override) {
        super(name);

        this.setDescription(getLang(DESC));

        this.addOption(OptionType.INTEGER, plotID, getLang(DESC_PLOT_ID), true);

        this.addOption(OptionType.BOOLEAN, override, getLang(DESC_OVERRIDE), true);
    }

    @Override
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotArchive payload) {
        String threadID;
        Optional<WebhookEntry> existingPlot = WebhookEntry.ifPlotExisted(payload.getPlotID());

        if(existingPlot.isPresent())
            threadID = Long.toUnsignedString(existingPlot.get().threadID());
        else { // Entry does not exist, nothing to deleted
            this.queueEmbed(hook, getEmbed(ORANGE, EMBED_NOTHING_TO_ARCHIVE));
            return;
        }

        this.queueEmbed(hook, formatInfoEmbed(threadID, payload.plotID));

        // Non-overriding archive will go straight to thread creation
        if(!payload.override) {
            this.queueImagesOption(hook, payload);
            return;
        }

        // Override is true, fetch the thread layout to verify its component data
        CompletableFuture<Optional<Layout>> fetchLayout = DiscordPS.getPlugin().getWebhook().getThreadLayout(threadID);

        fetchLayout.whenComplete((optLayout, error) -> {
            if(error != null) {
                this.queueEmbed(hook, errorEmbed(MESSAGE_ON_LAYOUT_FAILED, error.toString()));
                return;
            }

            optLayout.ifPresentOrElse(layout -> layout.getLayout().forEach(component -> {
                if (component instanceof InfoComponent infoComponent) {

                    File folder = PlotData.prepareMediaFolder(payload.getPlotID());
                    Optional<List<File>> optFiles = Optional.empty();

                    if(!folder.exists()) {
                        if(folder.mkdirs()) this.queueEmbed(hook, getEmbed(GREEN, EMBED_MEDIA_FOLDER_CREATED));
                        else this.queueEmbed(hook, errorEmbed(getEmbed(EMBED_MEDIA_FOLDER_FAILED), folder.getAbsolutePath()));
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
                                this.queueEmbed(hook, formatPendingImageWarning(String.join(", ", pendingFiles)));
                        });
                    }

                    // Plot exist with some images
                    ActionRow options = ActionRow.of(
                            Button.ARCHIVE_NOW.get(payload),
                            Button.ATTACH_IMAGES.get(payload),
                            Button.PROVIDE_IMAGES.get(payload),
                            Button.ARCHIVE_DISMISS.get(payload)
                    );

                    payload.setPreviousAttachment(infoComponent.getAttachments());
                    payload.setArchiveEntry(existingPlot.get());
                    this.queueImagesOption(hook, options,
                        this.formatImageEmbed(EMBED_IMAGE_ATTACHED, payload.plotID)
                    );
                }
            }), () -> this.queueEmbed(hook,
                errorEmbed(MESSAGE_ON_LAYOUT_FAILED,
                "Thread layout returned empty, possibly an internal error occurred.")
            ));
        });
    }

    @Override
    public void onImagesProvided(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction) {
        this.queueEmbed(hook,
            ActionRow.of(
                Button.ARCHIVE_CONFIRM.get(interaction),
                Button.ARCHIVE_CANCEL.get(interaction)),
            formatConfirmEmbed(interaction.override));
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
                    .forEach(getCacheDeleter(channel)));
        } else { // Exit if media folder has not been initialized
            channel.sendMessageEmbeds(
                errorEmbed(MESSAGE_MEDIA_FOLDER_NOT_EXIST, mediaFolder.getAbsolutePath())
            ).queue();
            return;
        }

        Function<String, File> fileLocation = fileName -> mediaFolder.toPath().resolve(fileName).toFile();

        // Queue the message embed instantly as loading
        CompletableFuture<Message> queueMessage = channel.sendMessageEmbeds(getEmbed(ORANGE, EMBED_ON_UPLOAD)).submit();

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
                    DiscordPS.error("Failed to download file to resource", ex);
                    DiscordPS.error("Failed to download file to: " + fileLocation.apply(PLOT_IMAGE_FILE));

                    failed = new RuntimeException(ex.toString());
                }
            }

            if(failed != null) {
                String failedReason = failed.getMessage();
                queueMessage.thenAccept(sent -> onImageDownloadFailed(sent, failedReason));
                return;
            }

            queueMessage.thenAccept(sent -> {
                sent.editMessageEmbeds(onMediaSaved(download.size(), uploaded.toString())).queue();
                channel.sendMessageEmbeds(formatConfirmEmbed(interaction.override))
                    .setActionRows(ActionRow.of(
                        Button.ARCHIVE_CONFIRM.get(interaction),
                        Button.ARCHIVE_CANCEL.get(interaction)))
                    .queue();
            });
        };

        // Run the stage
        CompletableFuture.allOf(download.toArray(new CompletableFuture[0])).thenRun(onDownloaded);
    }

    @Override
    public void onConfirmArchive(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction) {

        PlotArchiveEvent event = new PlotArchiveEvent(interaction.getPlotID(), "<@" + hook.getInteraction().getUser().getId() + '>');

        // Success handler after everything is completed
        Consumer<Message> onSuccess = defer -> {
            defer.editMessageEmbeds(getEmbed(GREEN, EMBED_ARCHIVE_SUCCESSFUL)).queue();

            // Notify the event is successful
            interaction.getArchiveEntry().ifPresent(entry -> Notification.notify(
                CommandMessage.PLOT_ARCHIVE,
                String.valueOf(entry.plotID()),
                hook.getInteraction().getUser().getId(),
                Long.toUnsignedString(entry.threadID())
            ));

            if(DiscordPS.getPlugin().getShowcase() != null) {
                // If showcase channel exist
                this.queueEmbed(hook,
                    ActionRow.of(
                        Button.SHOWCASE_PLOT.get(interaction),
                        Button.SHOWCASE_DISMISS.get(interaction)),
                    getEmbed(GREEN, EMBED_SHOWCASE_THIS_PLOT)
                );
            }
        };

        // Queue the message embed instantly as loading
        hook.sendMessageEmbeds(getEmbed(ORANGE, EMBED_ON_ARCHIVE)).queue(defer -> {
            if(!interaction.override)
                this.archiveAsNewThread(
                    DiscordPS.getPlugin().getWebhook()::forceRegisterNewPlot,
                    event,
                    ok -> onSuccess.accept(defer),
                    failed -> defer.editMessageEmbeds(errorEmbed(failed.toString())).queue()
                );
            else {
                if(interaction.getArchiveEntry().isEmpty()) {
                    defer.editMessageEmbeds(errorEmbed(MESSAGE_INTERACTION_EMPTY)).queue();
                    return;
                }

                PlotSystemThread.UpdateAction action = new PlotSystemThread.UpdateAction(
                        interaction.getPlotID(),
                        interaction.getArchiveEntry().get(),
                        Long.toUnsignedString(interaction.getArchiveEntry().get().messageID()),
                        Long.toUnsignedString(interaction.getArchiveEntry().get().threadID())
                );

                BiConsumer<PlotSystemThread.UpdateAction, ? super Throwable> handler = (ok, error) -> {
                    if(error != null) defer.editMessageEmbeds(errorEmbed(error.toString())).queue();
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
    public void onShowcaseConfirmed(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction) {
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
            () -> this.queueEmbed(hook, errorEmbed(MESSAGE_CANNOT_SHOWCASE))
        );
    }


    private void archiveAsNewThread(@NotNull Function<PlotSystemThread, CompletableFuture<Void>> creator,
                                    @NotNull PlotArchiveEvent event,
                                    @NotNull Consumer<Void> onSuccess,
                                    @NotNull Consumer<Throwable> onFailure) {
        PlotEntry plot = PlotEntry.getByID(event.getPlotID());

        if(plot == null) {
            onFailure.accept(Error.PLOT_FETCH_RETURNED_NULL.get());
            return;
        } else if (plot.ownerUUID() == null) {
            onFailure.accept(Error.PLOT_FETCH_UNKNOWN_OWNER.get());
            return;
        }

        PlotSystemThread thread = makeArchiveThread(event, plot);

        CompletableFuture<Void> createAction =  creator.apply(thread);

        if(createAction == null) onFailure.accept(Error.PLOT_CREATE_RETURNED_NULL.get());
        else createAction.whenComplete((ok, error) -> {
            if(error != null) onFailure.accept(error);
            else onSuccess.accept(ok);
        });
    }

    private @NotNull PlotSystemThread makeArchiveThread(PlotArchiveEvent event, PlotEntry plot) {
        PlotSystemThread thread = new PlotSystemThread(event.getPlotID());

        thread.setProvider(data -> {
            PlotData plotData = new PlotData(plot);
            plotData.setPrimaryStatus(ThreadStatus.archived, true);
            return plotData;
        });

        thread.setApplier(owner -> getLangManager().get(LangPaths.ARCHIVED_PREFIX)
            + " " + THREAD_NAME.apply(event.getPlotID(), owner.formatOwnerName()));

        thread.setModifier((owner, info) -> info.addHistory(event));

        return thread;
    }
}
