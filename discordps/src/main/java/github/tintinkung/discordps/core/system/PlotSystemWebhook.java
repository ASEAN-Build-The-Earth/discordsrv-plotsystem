package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.events.*;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.providers.WebhookProvider;
import github.tintinkung.discordps.core.system.components.PluginComponent;
import github.tintinkung.discordps.core.system.components.api.Container;
import github.tintinkung.discordps.core.system.components.api.TextDisplay;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.api.ComponentV2;
import github.tintinkung.discordps.core.system.layout.InfoComponent;
import github.tintinkung.discordps.core.system.layout.Layout;
import github.tintinkung.discordps.core.system.layout.StatusComponent;
import github.tintinkung.discordps.core.system.embeds.StatusEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static github.tintinkung.discordps.core.system.PlotSystemThread.THREAD_NAME;
import static github.tintinkung.discordps.core.system.WebhookDataBuilder.WebhookData;

public final class PlotSystemWebhook extends AbstractPlotSystemWebhook {

    /**
     * Create a new PlotSystemWebhook instance,
     * this should only happen once for {@link DiscordPS} to register.
     *
     * @param webhook The forum webhook manager instance
     */
    public PlotSystemWebhook(ForumWebhook webhook) {
        super(webhook);
    }

    /**
     * Get the provider of this webhook which
     * provides webhook as references and validation method.
     *
     * @return The provider as interface
     * @see WebhookProvider#validateWebhook(WebhookProvider...)
     */
    public WebhookProvider getProvider() {
        return this.webhook.getProvider();
    }


    /**
     * {@inheritDoc}
     */
    @Nullable
    protected CompletableFuture<Void> newThreadForPlotID(@NotNull PlotSystemThread thread,
                                                         boolean register,
                                                         boolean force) {

        if(!force) {
            Optional<WebhookEntry> existingPlot = WebhookEntry.ifPlotExisted(thread.getPlotID());
            if(existingPlot.isPresent()) return this.addNewExistingPlot(existingPlot.get(), register);
        }

        PlotEntry plot = PlotEntry.getByID(thread.getPlotID());

        if(plot == null) {
            Notification.sendErrorEmbed(PLOT_ENTRY_NOT_EXIST.apply(thread.getPlotID()));
            return null;
        }

        // Prepare plot data
        PlotData plotData = thread.getProvider().apply(plot);
        String threadName = thread.getApplier().apply(plotData);

        InfoComponent infoComponent = new InfoComponent(0, plotData);
        StatusComponent statusComponent = new StatusComponent(1, plotData);

        // Optional modification to info component if provided
        thread.getModifier().accept(plotData, infoComponent);

        List<ComponentV2> components = List.of(infoComponent.build(), statusComponent.build());

        return this.createNewPlotThread(threadName, plotData, components).handle((optMessage, error) -> {
            if(error != null) {
                ON_PLOT_CREATION_EXCEPTION.accept(thread.getPlotID(), error);
                throw new CompletionException(error);
            }

            if(register) this.registerNewPlot(plotData, thread.getPlotID(), optMessage::ifPresentOrElse);

            return null;
        });
    }

    /**
     * Create a new plot as forum thread with default values and register it to {@link WebhookEntry}.
     *
     * @param plotID A plot ID to be created
     */
    @Nullable
    public CompletableFuture<Void> createAndRegisterNewPlot(int plotID) {
        return this.createAndRegisterNewPlot(new PlotSystemThread(plotID));
    }

    /**
     * Create a new plot as forum thread and register it to {@link WebhookEntry}.
     *
     * @param thread The thread provider to get data from
     */
    @Nullable
    public CompletableFuture<Void> forceRegisterNewPlot(@NotNull PlotSystemThread thread) {
        return this.newThreadForPlotID(thread, true, true);
    }

    /**
     * Create a new plot as forum thread and register it to {@link WebhookEntry}.
     *
     * @param thread The thread provider to get data from
     */
    @Nullable
    public CompletableFuture<Void> createAndRegisterNewPlot(@NotNull PlotSystemThread thread) {
        return this.newThreadForPlotID(thread, true, false);
    }

    /**
     * Create a new plot as untracked thread,
     * this thread will not be able to be updated by the system after created.
     *
     * @param thread The thread provider to get data from
     */
    @Nullable
    public CompletableFuture<Void> createNewUntrackedPlot(@NotNull PlotSystemThread thread) {
        return this.newThreadForPlotID(thread, false, true);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    protected CompletableFuture<Void> addNewExistingPlot(@NotNull PlotSystemThread thread,
                                                         @Nullable Consumer<PlotData> onSuccess) {

        PlotEntry plot = PlotEntry.getByID(thread.getPlotID());
        Optional<String> threadID = thread.getOptID().map(Long::toUnsignedString);

        if(plot == null || threadID.isEmpty()) {
            Notification.sendErrorEmbed(PLOT_ENTRY_NOT_EXIST.apply(thread.getPlotID()));
            return null;
        }

        // Prepare plot data
        PlotData plotData = thread.getProvider().apply(plot);

        AvailableTag tag = ThreadStatus.fromPlotStatus(plot.status()).toTag();
        long tagID = tag.getTag().getIDLong();
        String threadName = thread.getApplier().apply(plotData);
        PlotReclaimEvent event = new PlotReclaimEvent(thread.getPlotID(), plotData.getOwnerMentionOrName());

        LayoutUpdater layoutUpdater = component -> updateExistingClaim(event, component, plotData, tag);

        CompletableFuture<Optional<MessageReference>> updateThreadLayoutAction = this.webhook.queueNewUpdateAction(
            this.webhook.getInitialLayout(threadID.get(), true).map(optLayout -> optLayout.flatMap(layoutUpdater)),
            data -> this.webhook.editInitialThreadMessage(threadID.get(), data, true)
        );

        CompletableFuture<Optional<DataObject>> editThreadAction = this.webhook
                .modifyThreadChannel(threadID.get(), threadName, Set.of(tagID), null, null, null, true)
                .submit();

        CompletableFuture<Void> allAction = CompletableFuture.allOf(
            updateThreadLayoutAction.whenComplete(HANDLE_LAYOUT_EDIT_ERROR),
            editThreadAction.whenComplete(HANDLE_THREAD_EDIT_ERROR)
        );

        return allAction.whenComplete((ok, error) -> {
            if(error != null) ON_PLOT_UPDATING_EXCEPTION.accept(error);
            if(onSuccess != null) onSuccess.accept(plotData);
        });
    }

    /**
     * Create and register a plot status by a given plot data.
     * Will create an initial status message with help button and documentation link.
     *
     * @param plotData The plot information
     * @param plotID The plot ID
     * @param threadIDLong The thread ID this plot is created on
     */
    protected void registerNewPlot(@NotNull PlotData plotData, int plotID, long threadIDLong) {
        String threadID = Long.toUnsignedString(threadIDLong);


        WebhookData statusData = new WebhookDataBuilder()
                .setEmbeds(Collections.singletonList(new StatusEmbed(plotData, plotData.getPrimaryStatus()).build()))
                .build();

        // When status message is sent: this is the actual message we use to track per ID,
        // this is truly unique which as put as primary key in database.
        // Therefore: we attach interactions component using its message ID as the component ID.
        Consumer<MessageReference> onMessageEntrySent = message -> {
            // Save message data as new webhook entry
            plotData.getOwnerDiscord().ifPresentOrElse(
                (member) -> WebhookEntry.insertNewEntry(message.getMessageIdLong(),
                    threadIDLong,
                    plotID,
                    plotData.getPrimaryStatus(),
                    plotData.getOwner().getUniqueId().toString(),
                    member.getId()
                ),
                () -> WebhookEntry.insertNewEntry(message.getMessageIdLong(),
                    threadIDLong,
                    plotID,
                    plotData.getPrimaryStatus(),
                    plotData.getOwner().getUniqueId().toString()
                )
            );

            // Attach interaction row to message
            List<ActionRow> interactions = this.createInitialInteraction(plotID, message.getMessageIdLong(), plotData.getPrimaryStatus(), plotData);

            WebhookData interactionData = new WebhookDataBuilder().setComponents(interactions).build();

            webhook.editThreadMessage(threadID, message.getMessageId(), interactionData, true)
                    .submitAfter(100, TimeUnit.MILLISECONDS).whenComplete(HANDLE_BUTTON_ATTACH_ERROR);
        };

        // Send status interaction embed
        // This is where the user can interact with etc. "Help" button
        webhook.sendMessageInThread(threadID, statusData, false, true).queue(optMsg ->
            optMsg.ifPresentOrElse(onMessageEntrySent, () -> Notification.sendErrorEmbed(PLOT_REGISTER_EXCEPTION)),
            ON_PLOT_REGISTER_EXCEPTION
        );

        PLOT_CREATE_NOTIFICATION.accept(plotData, threadID);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public <T extends PlotEvent>
    CompletableFuture<PlotSystemThread.UpdateAction> updatePlot(@NotNull PlotSystemThread.UpdateAction action,
                                                                @Nullable T event,
                                                                @NotNull ThreadStatus status) {
        String messageID = action.messageID();
        String threadID = action.threadID();
        AvailableTag tag = status.toTag();
        MemberOwnable owner = new MemberOwnable(action.entry().ownerUUID());

        LayoutUpdater layoutUpdater = component -> fetchLayoutData(event, component, action, owner, tag);
        MessageUpdater messageUpdater = message -> fetchStatusMessage(event, message, owner, status);

        // 1st update the thread layout components (the one that display main plot information)
        final CompletableFuture<Optional<MessageReference>> updateLayoutAction = this.webhook.queueNewUpdateAction(
            this.webhook.getInitialLayout(threadID, true).map(optLayout -> optLayout.flatMap(layoutUpdater)),
            data -> this.webhook.editInitialThreadMessage(threadID, data, true)
        );

        // 2nd update thread data, in this case is the status tag
        final CompletableFuture<Optional<DataObject>> updateThreadAction = this.fetchThreadData(threadID, event, owner, tag);

        // 3rd update the tracker status message
        final CompletableFuture<Optional<MessageReference>> updateMessageAction = this.webhook.queueNewUpdateAction(
            this.webhook.getWebhookMessage(threadID, messageID, true).map(optMessage -> optMessage.flatMap(messageUpdater)),
            data -> this.webhook.editThreadMessage(threadID, messageID, data, true)
        );

        // Wait for all action to complete and return it whether error or not
        // Error action will be handled as notification log
        final CompletableFuture<Void> allAction = CompletableFuture.allOf(
            updateLayoutAction.whenComplete(HANDLE_LAYOUT_EDIT_ERROR),
            updateThreadAction.whenComplete(HANDLE_THREAD_EDIT_ERROR),
            updateMessageAction.whenComplete(HANDLE_MESSAGE_EDIT_ERROR)
        );

        return allAction.handle((success, error) -> {
            if(error != null) ON_PLOT_UPDATE_EXCEPTION.accept(event, error);

            this.updateEntryStatus(action.entry().messageID(), status, event);

            return action;
        });
    }

    /**
     * Send ComponentV2 notification to a thread channel
     *
     * @param threadID The thread ID to send notification to
     * @param component The notification component to be sent to
     */
    public void sendNotification(@NotNull String threadID, @NotNull ComponentV2 component) {
        WebhookData data = new WebhookDataBuilder()
                .forceComponentV2()
                .setComponentsV2(Collections.singletonList(component))
                .build();

        this.webhook.sendMessageInThread(threadID, data, true, true).queue();
    }

    /**
     * Trigger plot review event by type as the following:
     * <ul>
     *     <li>{@link PlotApprovedEvent} notify owner as their plot is approved and attach feedback button.</li>
     *     <li>{@link PlotRejectedEvent} notify owner as their plot is rejected and attach rejected reason button.</li>
     *     <li>{@link PlotFeedbackEvent} update the plot's feedback button and notify owner that feedback is available.</li>
     * </ul>
     *
     * @param event The review event
     * @param <T> THe type of plot review event.
     */
    public <T extends PlotReviewEvent> void onPlotReview(@NotNull T event) {
        switch (event) {
            case PlotApprovedEvent approved: {
                this.updatePlot(approved, ThreadStatus.approved, action -> {
                    // Reply and ping user that their plot got reviewed

                    if(action.entry().ownerID() != null) {

                        Container container = new Container();

                        container.addComponent(
                            new TextDisplay("## :tada: Your plot has been **approved**!\n"
                            + "<@" + action.entry().ownerID() + "> We will notify your review feedback by the button above.")
                        );

                        this.sendNotification(action.threadID(), container);
                    }

                    this.attachFeedbackButton(ButtonStyle.SUCCESS, APPROVED_NO_FEEDBACK_LABEL, action);
                });
                return;
            }
            case PlotRejectedEvent rejected: {
                this.updatePlot(rejected, ThreadStatus.rejected, action -> {
                    // Reply and ping user that their plot got reviewed
                    if(action.entry().ownerID() != null) {
                        Container container = new Container();

                        container.addComponent(
                            new TextDisplay("## :broken_heart: Your plot has been *rejected*\n"
                            + "<@" + action.entry().ownerID() + "> We will notify the rejected reason by the button above, "
                            + " you can re-submit your plot again to get an approval.")
                        );

                        this.sendNotification(action.threadID(), container);
                    }

                    this.attachFeedbackButton(ButtonStyle.DANGER, REJECTED_NO_FEEDBACK_LABEL, action);
                });
                return;
            }
            case PlotFeedbackEvent feedback: {
                this.setFeedback(feedback.getFeedback(), PlotSystemThread.UpdateAction.fromEvent(event));
                return;
            }
            default: throw new IllegalStateException("Illegal PlotReviewEvent: " + event);
        }
    }

    /**
     * Fetch for plot's feedback button and set it enabled,
     * as well as set the feedback in {@link WebhookEntry}.
     *
     * @param feedback The feedback to be saved in {@link WebhookEntry}
     * @param action The update action for where the feedback button located
     */
    private void setFeedback(String feedback, @Nullable PlotSystemThread.UpdateAction action) {
        if(action == null) return;

        // TODO: plot-system hard-coded this string, maybe find a better way for this
        if(Objects.equals(feedback, "No Feedback")) return;

        // Send notification to owner when feedback button is set
        MessageUpdater messageUpdater = onFeedbackSet(action);

        // Queue the action
        CompletableFuture<Optional<MessageReference>> setFeedbackAction = this.webhook.queueNewUpdateAction(
            this.webhook.getWebhookMessage(action.threadID(), action.messageID(), true).map(opt -> opt.flatMap(messageUpdater)),
            data -> this.webhook.editThreadMessage(action.threadID(), action.messageID(), data, true)
        );

        setFeedbackAction.whenComplete((success, failed) -> {
            if(failed != null) ON_PLOT_FEEDBACK_EXCEPTION.accept(failed);

            try { // Update feedback data entry
                WebhookEntry.updateEntryFeedback(action.entry().messageID(), feedback);
            }
            catch (SQLException ex) {
                Notification.sendErrorEmbed(PLOT_FEEDBACK_SQL_EXCEPTION.apply(action.plotID()), ex.toString());
            }
        });
    }

    /**
     * Fetch the feedback message by update action and update the attached feedback buttons,
     * as well as send notification to plot owner.
     *
     * @param action The update action of this feedback event
     * @return Message updater for applying the updated data as {@link WebhookData}
     * @see #fetchFeedbackMessage(Message, Consumer)
     */
    private @NotNull MessageUpdater onFeedbackSet(@NotNull  PlotSystemThread.UpdateAction action) {
        return message -> this.fetchFeedbackMessage(message, label -> {
            Container container = new Container();

            container.addComponent(
                new TextDisplay("## :pencil: Your plot has been reviewed!!\n"
                + "<@" + action.entry().ownerID() + "> click the **"
                + label + "** button above to view the your feedback.")
            );
            this.sendNotification(action.threadID(), container);
        });
    }

    /**
     * Attach feedback button to plot status embed
     *
     * @param style The button style
     * @param label The button label
     * @param action The update action to what plot will be updated
     */
    private void attachFeedbackButton(ButtonStyle style, String label, @Nullable PlotSystemThread.UpdateAction action) {
        if(action == null) return;

        BiFunction<Long, Long, Button> onButtonSet = (messageID, userID) -> Button.of(
            style, AvailableButton.FEEDBACK_BUTTON.resolve(messageID, userID, action.plotID()), label
        );

        MessageUpdater messageUpdater = message -> this.fetchFeedbackButton(message, onButtonSet);

        this.webhook.queueNewUpdateAction(
            this.webhook.getWebhookMessage(action.threadID(), action.messageID(), true).map(opt -> opt.flatMap(messageUpdater)),
            data -> this.webhook.editThreadMessage(action.threadID(), action.messageID(), data, true)
        );
    }


    /**
     * Fetch and add new claim to plot status.
     * Will replace the latest claim to the given status if given repeated member.
     *
     * @param event The reclaim event to add as history message
     * @param component The layout to fetch
     * @param owner The new claim owner
     * @param tag The claim status as tag reference
     * @return Updated data
     */
    @NotNull
    private static Optional<WebhookData> updateExistingClaim(@NotNull PlotReclaimEvent event,
                                                             @NotNull Layout component,
                                                             MemberOwnable owner,
                                                             AvailableTag tag) {
        // Update layout data
        List<ComponentV2> updated = new ArrayList<>();
        int latestPosition = 0;
        boolean repeatedLatestMember = false;
        boolean doUploadAvatar = false;
        Map<UUID, String> savedUUID = new HashMap<>();

        for(LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>> layout : component.getLayout()) {
            switch (layout) {
                case InfoComponent infoComponent:
                    infoComponent.addHistory(event);
                    infoComponent.setAccentColor(tag.getColor());
                    break;
                case StatusComponent statusComponent:
                    savedUUID.put(statusComponent.getThumbnailOwner(), statusComponent.getThumbnailURL());
                    // Latest status component
                    if (latestPosition + 1 == component.getLayout().size()) {
                        repeatedLatestMember = owner.getOwner().getUniqueId().equals(statusComponent.getThumbnailOwner());
                    }

                    if (repeatedLatestMember) {
                        statusComponent.changeStatusMessage(StatusComponent.DisplayMessage.fromTag(tag));
                        statusComponent.setAccentColor(tag.getColor());

                    } // TODO: Consider editing old status when it got reclaimed

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + layout);
            }

            latestPosition += 1;
            updated.add(layout.build());
        }

        // Add new claim owner to the status
        if(!repeatedLatestMember) {

            // Check if attachment for avatar is already in the message
            UUID plotOwner = owner.getOwner().getUniqueId();
            String avatarURL = null;

            // If avatar is not saved, upload a new one
            if (savedUUID.containsKey(plotOwner))
                avatarURL = savedUUID.get(plotOwner);
            else doUploadAvatar = true;

            if (avatarURL == null) avatarURL = owner.getAvatarAttachmentOrURL();

            StatusComponent newClaim = new StatusComponent(
                    latestPosition,
                    tag.toStatus(),
                    plotOwner,
                    owner.getOwnerMentionOrName(),
                    avatarURL
            );
            updated.add(newClaim.build());
        }

        WebhookData updatedData = new WebhookDataBuilder()
                .forceComponentV2()
                .setComponentsV2(updated)
                .build();

        if(doUploadAvatar) {
            savedUUID.keySet().forEach(UUID -> {
                if(UUID != null && !UUID.equals(owner.getOwner().getUniqueId()))
                    new MemberOwnable(UUID.toString()).getAvatarFile().ifPresent(updatedData::addFile);
            });
            owner.getAvatarFile().ifPresent(updatedData::addFile);
        }

        return Optional.of(updatedData);
    }

    /**
     * Updates the thread's metadata on Discord based on the given plot event.
     * <p>
     * This method modifies the thread's tags using the specified {@link AvailableTag}. If the event is a
     * {@link PlotArchiveEvent}, it additionally updates the thread's name to reflect archival status and
     * sets the auto-archive duration to 1 day (1440 minutes).
     * </p>
     *
     * @param threadID the ID of the Discord thread to update
     * @param event the event triggering the update; if {@code null}, only tag data will be updated
     * @param owner the {@link MemberOwnable} instance representing the event's owner
     * @param tag the {@link AvailableTag} to apply to the thread
     * @param <T> the type of the plot event
     * @return a {@link CompletableFuture} containing an {@link Optional} result from the thread update
     */
    @NotNull
    private <T extends PlotEvent>
    CompletableFuture<Optional<DataObject>> fetchThreadData(@NotNull String threadID,
                                                            @Nullable T event,
                                                            @NotNull MemberOwnable owner,
                                                            @NotNull AvailableTag tag) {
        Set<Long> tags = Set.of(tag.getTag().getIDLong());
        if(event instanceof PlotArchiveEvent) {
            // Archive event will insert archive prefix to thread name and auto close the thread in one day (1440 minutes)
            String threadName = "[Archived] " + THREAD_NAME.apply(event.getPlotID(), owner.formatOwnerName());

            return this.webhook.modifyThreadChannel(threadID, threadName, tags, 1440, null, null, true).submit();
        }
        else return this.webhook.editThreadChannelTags(threadID, tags, true).submit();
    }

    /**
     * Fetch the layout component with new event by the given layout data
     * and return as a new updated webhook data.
     *
     * @param event The event
     * @param component The layout component to be updated
     * @param action The update action to what will be updated
     * @param owner The owner of this layout data
     * @param tag The primary tag to be applied
     * @return The given layout updated and built to webhook data
     * @param <T> The type of event that will be fetched
     */
    @NotNull
    private <T extends PlotEvent>
    Optional<WebhookData> fetchLayoutData(@Nullable T event,
                                          @NotNull Layout component,
                                          @NotNull PlotSystemThread.UpdateAction action,
                                          @NotNull MemberOwnable owner,
                                          @NotNull AvailableTag tag) {
        boolean modified = false;

        // Update layout data
        List<ComponentV2> updated = new ArrayList<>();
        List<File> imageList = new ArrayList<>();
        List<String> ownerList = new ArrayList<>();

        for(LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>> layout : component.getLayout()) {
            switch (layout) {
                // Edit info component accent and history as well as add new images if exist
                case InfoComponent infoComponent:
                    // Edit accent base on updated event
                    if (!infoComponent.getAccentColor().equals(tag.getColor())) {
                        infoComponent.addHistory(event);
                        infoComponent.setAccentColor(tag.getColor());
                    }

                    Function<File, Boolean> fetcher = file -> {
                        imageList.add(file);
                        return infoComponent.addAttachment(file.getName()) == null;
                    };

                    // Fetch the plot's media every update call,
                    // return as modified if info component successfully registered image gallery
                    modified = PlotData.fetchMediaFolder(PlotData.checkMediaFolder(action.plotID()), fetcher);

                    if(modified) infoComponent.registerImageGallery();

                    break;
                // Check for status and sync it with event type
                case StatusComponent statusComponent:
                    // correct owner
                    if (action.entry().ownerUUID().equals(owner.getOwner().getUniqueId().toString())) {
                        statusComponent.setAccentColor(tag.getColor());
                        statusComponent.changeStatusMessage(StatusComponent.DisplayMessage.fromTag(tag));
                    }

                    if(statusComponent.getThumbnailOwner() != null)
                        ownerList.add(statusComponent.getThumbnailOwner().toString());

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + layout);
            }
            updated.add(layout.build());
        }

        WebhookData updatedData = new WebhookDataBuilder().forceComponentV2().setComponentsV2(updated).build();

        if(modified) {
            imageList.forEach(updatedData::addFile);

            // Reference to previous attachment will get reset if there are new file attached.
            // We have to attach it back to the message to restore the attachment data.
            ownerList.forEach(member -> new MemberOwnable(member).getAvatarFile().ifPresent(updatedData::addFile));
        }

        return Optional.of(updatedData);
    }

    /**
     * Fetch a new status message as a webhook data.
     *
     * @param owner The owner of this plot data
     * @param status The status of this plot
     * @return New {@link StatusComponent} built as webhook data ready to be used in API call
     */
    @NotNull
    private <T extends PlotEvent>
    Optional<WebhookData> fetchStatusMessage(T event,
                                             Message message,
                                             MemberOwnable owner,
                                             ThreadStatus status) {
        StatusEmbed statusEmbed = new StatusEmbed(owner, status);

        // No interaction to update
        if(message.getActionRows().isEmpty()) return Optional.of(
                new WebhookDataBuilder()
                        .setEmbeds(Collections.singletonList(statusEmbed.build()))
                        .build()
        );

        // TODO: Remove button base on undo event
        ActionRow componentRow = message.getActionRows().getFirst();

        // Clear action row on completion event
        if(event instanceof PlotArchiveEvent || event instanceof  PlotAbandonedEvent) {
            List<Button> buttons = new ArrayList<>();

            // Make all interactive button disabled
            componentRow.getButtons().forEach(button -> PluginComponent.getOpt(button).ifPresent(component -> {
                switch (AvailableButton.valueOf(component.getType())) {
                    // Ignore if feedback button is already attached
                    case AvailableButton.FEEDBACK_BUTTON:
                        buttons.add(Button.primary(component.getRawID() + "/Locked", Emoji.fromUnicode("U+1F512")).asDisabled());
                        break;
                    case AvailableButton.HELP_BUTTON:
                        buttons.add(Button.secondary(component.getRawID() + "/Locked", button.getLabel()).asDisabled());
                }
            }));

            if(buttons.isEmpty()) buttons.add(PLOT_DOCUMENTATION_BUTTON.asDisabled());

            return Optional.of(new WebhookDataBuilder()
                    .setEmbeds(Collections.singletonList(statusEmbed.build()))
                    .setComponents(Collections.singletonList(ActionRow.of(buttons)))
                    .build()
            );
        }

        return Optional.of(new WebhookDataBuilder()
                .setEmbeds(Collections.singletonList(statusEmbed.build()))
                .build()
        );
    }

    /**
     * Fetch a new feedback button into message.
     *
     * @param message The message to fetch a feedback button to.
     * @param onButtonSet Invoke when setting button with message ID and owner ID, use this to supply button style.
     * @return The updated component data built as {@link github.tintinkung.discordps.core.system.WebhookDataBuilder.WebhookData}
     */
    @NotNull
    private Optional<WebhookData> fetchFeedbackButton(@NotNull Message message,
                                                      @NotNull BiFunction<Long, Long, Button> onButtonSet) {
        if(message.getActionRows().isEmpty()) return Optional.empty();

        // Clone our component button and add a new, feedback button
        ActionRow componentRow = message.getActionRows().getFirst();
        List<Button> buttons = new ArrayList<>();

        componentRow.getButtons().forEach((button) -> {
            // Add all sent button back
            if(button == null) return;

            // Parse for data in help button
            PluginComponent.getOpt(button).ifPresent(component -> {
                AvailableButton buttonID = AvailableButton.valueOf(component.getType());

                switch (buttonID) {
                    // Ignore if feedback button is already attached
                    case AvailableButton.FEEDBACK_BUTTON: return;
                    // Feedback button is not yet attached:
                    // Clone setting from a previously attached help button
                    // since this button's existence also confirms user owner in its ID
                    case AvailableButton.HELP_BUTTON: buttons.add(
                            onButtonSet.apply(message.getIdLong(), component.getUserIDLong()).asDisabled()
                    );
                }
            });

            buttons.add(button);
        });

        List<ActionRow> updatedRow = List.of(ActionRow.of(buttons));

        return Optional.of(new WebhookDataBuilder().setComponents(updatedRow).build());
    }

    /**
     * Fetch the status message for feedback button and edit the button as enabled.
     *
     * @param message The status message
     * @return The updated status message built to webhook data
     */
    @NotNull
    private Optional<WebhookData> fetchFeedbackMessage(@NotNull Message message,
                                                       @NotNull Consumer<String> onSuccess) {
        if(message.getActionRows().isEmpty()) return Optional.empty();

        // Clone our component button and add a new, feedback button
        ActionRow componentRow = message.getActionRows().getFirst();
        List<Button> buttons = new ArrayList<>();

        componentRow.getButtons().forEach((button) -> {
            // Add all sent button back
            if(button == null) return;

            // Parse for data in feedback button
            try {
                PluginButton component = new PluginButton(button);

                // If feedback button is already attached
                // update it to functional button
                // (the initial feedback button is set as "No Feedback"
                if(AvailableButton.valueOf(component.getType()) == AvailableButton.FEEDBACK_BUTTON) {

                    String label = button.getLabel();

                    buttons.add(switch (button.getStyle()) {
                        case DANGER -> button.withLabel(label = REJECTED_FEEDBACK_LABEL).asEnabled();
                        case SUCCESS -> button.withLabel(label = APPROVED_FEEDBACK_LABEL).asEnabled();
                        default -> button.asDisabled();
                    });

                    // Reply and ping user that their plot got reviewed
                    onSuccess.accept(label);
                    return;
                }
            }
            catch (IllegalArgumentException ignored) { }

            buttons.add(button);
        });

        List<ActionRow> updatedRow = List.of(ActionRow.of(buttons));

        return Optional.of(new WebhookDataBuilder().setComponents(updatedRow).build());
    }
}
