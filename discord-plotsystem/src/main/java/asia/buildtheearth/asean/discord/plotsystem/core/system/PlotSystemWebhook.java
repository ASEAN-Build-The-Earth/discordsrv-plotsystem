package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.components.PluginComponent;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.ReviewComponent;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.utils.concurrent.DelayedCompletableFuture;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.LayoutComponentProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.components.api.TextDisplay;
import asia.buildtheearth.asean.discord.components.api.ComponentV2;
import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotNotification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.InfoComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.StatusComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.embeds.StatusEmbed;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.components.WebhookDataBuilder.WebhookData;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.ErrorMessage;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotMessage;

/**
 * Main class for managing {@link ForumWebhook} used by all Plot-System functionality.
 *
 * <p>Class majority is used via {@link asia.buildtheearth.asean.discord.plotsystem.core.listeners.PlotSystemListener}
 * with some selective method usage for manual controls.</p>
 */
public final class PlotSystemWebhook extends AbstractPlotSystemWebhook {

    /**
     * Create a new PlotSystemWebhook instance,
     * this should only happen once for {@link DiscordPS} to register.
     *
     * @param webhook The forum webhook manager instance
     */
    public PlotSystemWebhook(DiscordPS plugin, ForumWebhook webhook) {
        super(plugin, webhook);
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
                                                         @Nullable PlotCreateData plot,
                                                         boolean register,
                                                         boolean force) {

        if(plot == null) return CompletableFuture.failedFuture(
            new RuntimeException("Received null data to create plot.")
        );

        if(!force) {
            Optional<WebhookEntry> existingPlot = WebhookEntry.ifPlotExisted(thread.getPlotID());
            if(existingPlot.isPresent()) return this.addNewExistingPlot(existingPlot.get(), plot, register);
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
     * @param plotData initial plot data to create
     */
    @Nullable
    public CompletableFuture<Void> createAndRegisterNewPlot(int plotID,
                                                            @NotNull PlotCreateData plotData) {
        return this.createAndRegisterNewPlot(new PlotSystemThread(plotID), plotData);
    }

    /**
     * Create a new plot as forum thread and register it to {@link WebhookEntry}.
     *
     * @param thread The thread provider to get data from
     * @param plotData initial plot data to create
     */
    @Nullable
    public CompletableFuture<Void> forceRegisterNewPlot(@NotNull PlotSystemThread thread,
                                                        @NotNull PlotCreateData plotData) {
        return this.newThreadForPlotID(thread, plotData, true, true);
    }

    /**
     * Create a new plot as forum thread and register it to {@link WebhookEntry}.
     *
     * @param thread The thread provider to get data from
     * @param plotData initial plot data to create
     */
    @Nullable
    public CompletableFuture<Void> createAndRegisterNewPlot(@NotNull PlotSystemThread thread,
                                                            @NotNull PlotCreateData plotData) {
        return this.newThreadForPlotID(thread, plotData, true, false);
    }

    /**
     * Create a new plot as untracked thread,
     * this thread will not be able to be updated by the system after created.
     *
     * @param thread The thread provider to get data from
     * @param plotData initial plot data to create
     */
    @Nullable
    public CompletableFuture<Void> createNewUntrackedPlot(@NotNull PlotSystemThread thread,
                                                          @NotNull PlotCreateData plotData) {
        return this.newThreadForPlotID(thread, plotData, false, true);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    protected CompletableFuture<Void> addNewExistingPlot(@NotNull PlotSystemThread thread,
                                                         @NotNull PlotCreateData plot,
                                                         @Nullable String removeMemberID,
                                                         @Nullable Consumer<PlotData> onSuccess) {
        Optional<String> threadID = thread.getOptID().map(Long::toUnsignedString);

        if(threadID.isEmpty()) {
            Notification.notify(
                ErrorMessage.PLOT_REGISTER_ENTRY_EXCEPTION,
                String.valueOf(thread.getPlotID())
            );
            return null;
        }

        // Prepare plot data
        PlotData plotData = thread.getProvider().apply(plot);

        AvailableTag tag = ThreadStatus.valueOf(plot.status().getName()).toTag();
        long tagID = tag.getTag().getIDLong();
        String threadName = thread.getApplier().apply(plotData);

        @SuppressWarnings("deprecation")
        PlotReclaimEvent event = new PlotReclaimEvent(thread.getPlotID(), plotData.getOwnerMentionOrName());

        // Update thread layout to correct status
        LayoutUpdater layoutUpdater = component -> updateExistingClaim(event, component, plotData, tag);

        CompletableFuture<Optional<MessageReference>> updateThreadLayoutAction = this.webhook.queueNewUpdateAction(
            this.webhook.getInitialLayout(threadID.get(), true).map(optLayout -> optLayout.flatMap(layoutUpdater)),
            data -> this.webhook.editInitialThreadMessage(threadID.get(), data, true)
        );

        // Edit thread's status tag
        CompletableFuture<Optional<DataObject>> editThreadAction = this.webhook
            .modifyThreadChannel(threadID.get(), threadName, Set.of(tagID), null, null, null, true)
            .submit();

        // Optionally add new owner of exist (and not the same as previous owner)
        Optional<CompletableFuture<Void>> optAddMemberAction = plotData.getOwnerDiscord().map(ISnowflake::getId)
            .map(newOwner -> (removeMemberID != null && !removeMemberID.equals(newOwner))
                ? this.webhook.addThreadMember(threadID.get(), newOwner).submit() : null
            );

        // Remove previous member
        CompletableFuture<Void> removeMemberAction = optAddMemberAction
            .map(exist -> this.webhook.removeThreadMember(threadID.get(), removeMemberID).submit())
            .orElse(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> addMemberAction = optAddMemberAction.orElse(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> allAction = CompletableFuture.allOf(
            updateThreadLayoutAction.whenComplete(HANDLE_LAYOUT_EDIT_ERROR),
            editThreadAction.whenComplete(HANDLE_THREAD_EDIT_ERROR),
            removeMemberAction.whenComplete(HANDLE_EDIT_MEMBER_ERROR),
            addMemberAction.whenComplete(HANDLE_EDIT_MEMBER_ERROR)
        );

        return allAction.whenComplete((ok, error) -> {
            if(error != null) ON_PLOT_OVERRIDING_EXCEPTION.accept(error);
            if(onSuccess != null) onSuccess.accept(plotData);
        });
    }

    /**
     * {@inheritDoc}
     */
    protected void registerNewPlot(@NotNull PlotData plotData, int plotID, long threadIDLong) {
        String threadID = Long.toUnsignedString(threadIDLong);

        StatusEmbed statusEmbed = new StatusEmbed(
            plotData, plotData.getPrimaryStatus(),
            this.getMessageReferenceURL(threadID, threadID)
        );

        WebhookData statusData = new WebhookDataBuilder()
                .setEmbeds(Collections.singletonList(statusEmbed.build()))
                .build();

        // When status message is sent: this is the actual message we use to track per ID,
        // this is truly unique which is put as primary key in database.
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

            // Add owner to the thread if not already added by mentions
            CompletableFuture<Void> addMemberAction = plotData.getOwnerDiscord().map(ISnowflake::getId)
                    .map(newOwner -> this.webhook.addThreadMember(threadID, newOwner).submit())
                    .orElseGet(() -> CompletableFuture.completedFuture(null));

            // Add interaction data to the status message after it is sent
            DelayedCompletableFuture<Optional<MessageReference>> addInteractionAction = this.webhook
                    .editThreadMessage(threadID, message.getMessageId(), interactionData, true)
                    .submitAfter(100, TimeUnit.MILLISECONDS);

            CompletableFuture.allOf(
                addInteractionAction.whenComplete(HANDLE_BUTTON_ATTACH_ERROR),
                addMemberAction.whenComplete(HANDLE_EDIT_MEMBER_ERROR)
            ).thenAccept(success -> {
                // Notify the thread that plot is created
                this.onNotification(NotificationType.ON_CREATED, plotID, PlotMessage::getPlotMessage,
                    (notification, notifyMessage) -> {
                        this.sendNotification(
                            notification,
                            threadID,
                            plotData.getOwnerMentionOrName()
                        );

                        Notification.notify(
                            notifyMessage,
                            threadID,
                            String.valueOf(plotID),
                            plotData.getOwnerMentionOrName()
                        );
                    }
                );
            });
        };

        // Send status interaction embed
        // This is where the user can interact with etc. "Help" button
        webhook.sendMessageInThread(threadID, statusData, false, true).queue(optMsg ->
            optMsg.ifPresentOrElse(onMessageEntrySent, () -> Notification.notify(ErrorMessage.PLOT_REGISTER_UNKNOWN_EXCEPTION)),
            error -> ON_PLOT_REGISTER_EXCEPTION.accept(plotID, error)
        );
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

        LayoutUpdater layoutUpdater = component -> fetchLayoutData(action.plotID(), event, component, owner, tag);
        MessageUpdater messageUpdater = message -> fetchStatusMessage(event, message, threadID, owner, status);

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
     * {@inheritDoc}
     */
    protected void sendNotification(@NotNull PlotNotification type,
                                 @NotNull String threadID,
                                 @NotNull Function<String, String> content) {

        // Exit if this notification is disabled
        if(!this.plugin.getConfig().getBoolean(type.getKey(), true)) return;

        // Parse message from language file
        TextDisplay display = new TextDisplay(content.apply(DiscordPS.getMessagesLang().get(type)));

        Container container = new Container();

        container.addComponent(display);

        WebhookData data = new WebhookDataBuilder()
                .forceComponentV2()
                .setComponentsV2(Collections.singletonList(container))
                .build();

        this.webhook.sendMessageInThread(threadID, data, true, true).queue();
    }

    public @NotNull CompletableFuture<?> sendFeedback(int plotID,
                                                      @NotNull WebhookEntry entry,
                                                      @NotNull String rawContent,
                                                      @NotNull String threadID,
                                                      @Nullable String feedbackID) {

        String title = this.metadata.newFeedbackNotification().replace(Format.OWNER, this.parseOwnerMention(entry));
        Optional<List<File>> reviewMedia = ReviewComponent.getOptMedia(plotID, feedbackID);
        ReviewComponent component = new ReviewComponent(rawContent, null, Constants.BLUE, reviewMedia.orElse(null));

        Collection<ComponentV2> components = (StringUtils.isBlank(title))
                ? Collections.singletonList(component.build())
                : List.of(new TextDisplay(title), component.build());

        WebhookDataBuilder.WebhookData webhookData = new WebhookDataBuilder()
                .setComponentsV2(components)
                .forceComponentV2()
                .build();

        reviewMedia.ifPresent(files -> files.forEach(webhookData::addFile));

        return this.webhook.sendMessageInThread(threadID, webhookData, true, true).submit();
    }

    /**
     * Update a plot as submitted
     *
     * @param event The submit event containing plot ID to update
     */
    public void onPlotSubmit(@NotNull PlotSubmitEvent event) {
        this.updatePlot(event, ThreadStatus.finished, plot ->
            onNotification(NotificationType.ON_SUBMITTED, plot.plotID(), PlotMessage::getPlotMessage,
                (notification, message) -> {
                    final String ownerMention = parseOwnerMention(plot.entry());
                    this.sendNotification(
                        notification,
                        String.valueOf(plot.plotID()),
                        plot.threadID(),
                        ownerMention
                    );

                    Notification.notify(
                        message,
                        plot.threadID(),
                        String.valueOf(event.getPlotID()),
                        String.valueOf(Instant.now().getEpochSecond())
                    );
                }
            )
        );
    }

    /**
     * Update a plot as abandoned
     *
     * @param event The submit event containing plot ID to update
     */
    public void onPlotAbandon(@NotNull PlotAbandonedEvent event) {
        this.updatePlot(event, ThreadStatus.abandoned, plot ->
            onNotification(NotificationType.ON_ABANDONED, plot.plotID(), notification -> {
                final String ownerMention = parseOwnerMention(plot.entry());

                this.sendNotification(notification,
                    plot.threadID(),
                    ownerMention
                );

                Notification.notify(
                    PlotMessage.getAbandonMessage(event.getType()),
                    plot.threadID(),
                    String.valueOf(event.getPlotID())
                );
            }
        ));
    }

    /**
     * Trigger plot undo event by type as the following:
     * <ul>
     *     <li>{@link PlotUndoReviewEvent} notify owner as their plot review has been revoked.</li>
     *     <li>{@link PlotUndoSubmitEvent} notify owner that they undid their plot submission.</li>
     * </ul>
     *
     * @param event The undo event
     * @param <T> THe type of plot undo event.
     */
    public <T extends PlotUndoEvent> void onPlotUndo(@NotNull T event) {
        switch (event) {
            case PlotUndoReviewEvent undo:
                this.updatePlot(undo, ThreadStatus.finished,
                    action -> onNotification(NotificationType.ON_UNDO_REVIEW, action.plotID(),
                        notification -> this.sendNotification(notification, action.threadID())
                    ));
                return;
            case PlotUndoSubmitEvent undo:
                this.updatePlot(undo, ThreadStatus.on_going,
                    action -> onNotification(NotificationType.ON_UNDO_SUBMIT, action.plotID(),
                        notification -> this.sendNotification(notification, action.threadID())
                    ));
                return;
            default: throw new IllegalStateException("Illegal PlotUndoEvent: " + event);
        }
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
                    onNotification(NotificationType.ON_APPROVED, action.plotID(), PlotMessage::getPlotMessage,
                        (notification, message) -> {
                            final String ownerMention = parseOwnerMention(action.entry());
                            this.sendNotification(notification, action.threadID(), ownerMention);
                            Notification.notify(message, action.threadID(), String.valueOf(action.plotID()));
                        }
                    );

                    this.attachFeedbackButton(ButtonStyle.SUCCESS, this.metadata.approvedNoFeedbackLabel(), action);
                });
                return;
            }
            case PlotRejectedEvent rejected: {
                this.updatePlot(rejected, ThreadStatus.rejected, action -> {
                    // Reply and ping user that their plot got reviewed
                    onNotification(NotificationType.ON_REJECTED, action.plotID(), PlotMessage::getPlotMessage,
                        (notification, message) -> {
                            final String ownerMention = parseOwnerMention(action.entry());
                            this.sendNotification(notification, action.threadID(), ownerMention);
                            Notification.notify(message, action.threadID(), String.valueOf(action.plotID()));
                        }
                    );

                    this.attachFeedbackButton(ButtonStyle.DANGER, this.metadata.rejectedNoFeedbackLabel(), action);
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
     * Send notification to a plot for inactivity notice using {@link InactivityNoticeEvent}
     *
     * @param event The {@link InactivityNoticeEvent} containing the plot ID and abandonment timestamp
     */
    @SuppressWarnings("deprecation")
    public void onPlotInactivity(@NotNull InactivityNoticeEvent event) {
        WebhookEntry.ifPlotExisted(event.getPlotID()).ifPresent(entry -> {
            String threadID = Long.toUnsignedString(entry.threadID());
            String owner = parseOwnerMention(entry);
            Instant timestamp = switch (event.getTimestamp()) {
                case java.time.LocalDate date -> date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
                case java.time.LocalDateTime dateTime -> dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                case java.time.ZonedDateTime zonedDateTime -> zonedDateTime.toInstant();
                case java.time.OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
                default -> throw new IllegalArgumentException(
                    "Unsupported timestamp given by: " + event.getClass().getSimpleName()
                    + " type: " + event.getTimestamp().getClass()
                );
            };

            onNotification(NotificationType.ON_INACTIVITY, event.getPlotID(), notification ->
                this.sendNotification(notification, threadID, content -> content
                    .replace(Format.OWNER, owner)
                    .replace(Format.TIMESTAMP, String.valueOf(timestamp.getEpochSecond())))
            );
        });
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
                Notification.sendErrorEmbed(
                    ErrorMessage.PLOT_FEEDBACK_SQL_EXCEPTION,
                    ex.toString(),
                    String.valueOf(action.plotID())
                );
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
    private @NotNull MessageUpdater onFeedbackSet(@NotNull PlotSystemThread.UpdateAction action) {
        return message -> this.fetchFeedbackMessage(message, label ->
            onNotification(NotificationType.ON_REVIEWED, action.plotID(), notification -> {
                final String owner = parseOwnerMention(action.entry());
                this.sendNotification(notification, label, action.threadID(), owner);
            })
        );
    }

    /**
     * Figure out owner's mention from plot entry.
     *
     * @param entry The plot entry to get
     * @return Discord mention if the owner has discord account linked,
     *         else will fetch for the owner's minecraft name from UUID
     * @see Bukkit#getOfflinePlayer(UUID)
     */
    private String parseOwnerMention(@NotNull WebhookEntry entry) {
        try {
            UUID ownerUUID = UUID.fromString(entry.ownerUUID());

            return entry.ownerID() != null
                ? "<@" + entry.ownerID() + '>'
                : Bukkit.getOfflinePlayer(ownerUUID).getName();
        }
        catch (IllegalArgumentException ignored) {
            return LanguageFile.NULL_LANG;
        }
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
    private static Optional<WebhookData> updateExistingClaim(@SuppressWarnings("deprecation")
                                                             @NotNull PlotReclaimEvent event,
                                                             @NotNull Layout component,
                                                             MemberOwnable owner,
                                                             AvailableTag tag) {
        // Update layout data
        List<ComponentV2> updated = new ArrayList<>();
        int latestPosition = 0;
        boolean repeatedLatestMember = false;
        boolean doUploadAvatar = false;
        Map<UUID, String> savedUUID = new HashMap<>();

        for(LayoutComponentProvider<?, ?> layout : component.getLayout()) {
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
                .suppressMentions()
                .suppressNotifications()
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
            if(tag != AvailableTag.ARCHIVED) DiscordPS.warning("Expected tag: "
                + AvailableTag.ARCHIVED.name()
                + " for " + event.getClass().getSimpleName()
                + "(Got: " + tag.name() + ")"
            );

            // Archive event will insert archive prefix to thread name if specified
            final String threadName = PlotSystemWebhook
                    .getOptArchiveThreadName(event.getPlotID())
                    .map(thread -> thread.apply(owner))
                    .orElse(null);

            // Archived thread will be auto locked in one day (1440 minutes)
            return this.webhook.modifyThreadChannel(threadID, threadName, tags, 1440, null, null, true).submit();
        }
        return this.webhook.editThreadChannelTags(threadID, tags, true).submit();
    }

    /**
     * Fetch the layout component with new event by the given layout data
     * and return as a new updated webhook data.
     *
     * @param plotID The plotID to be updated
     * @param event The event
     * @param component The layout component to be updated
     * @param owner The owner of this layout data
     * @param tag The primary tag to be applied
     * @return The given layout updated and built to webhook data
     * @param <T> The type of event that will be fetched
     */
    @NotNull
    private <T extends PlotEvent>
    Optional<WebhookData> fetchLayoutData(int plotID,
                                          @Nullable T event,
                                          @NotNull Layout component,
                                          @NotNull MemberOwnable owner,
                                          @NotNull AvailableTag tag) {
        // Check if layout attachment(s) is modified
        boolean modified = false;

        // Update layout data
        List<ComponentV2> updated = new ArrayList<>();
        List<File> imageList = new ArrayList<>();
        List<String> ownerList = new ArrayList<>();

        for(LayoutComponentProvider<?, ?> layout : component.getLayout()) {
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
                    modified = PlotData.fetchMediaFolder(PlotData.checkMediaFolder(plotID), fetcher);

                    if(modified) infoComponent.registerImageGallery();

                    break;
                // Check for status and sync it with event type
                case StatusComponent statusComponent:
                    if(statusComponent.getThumbnailOwner() != null) {
                        ownerList.add(statusComponent.getThumbnailOwner().toString());
                        // If owner is confirmed to not be right, skip the process
                        if(!statusComponent.getThumbnailOwner().equals(owner.getOwner().getUniqueId()))
                            break;
                    }
                    else { // unknown owner
                        String error = "Cannot verify owner of a status component in the position: "
                                + statusComponent.getLayoutPosition() + " (Plot ID: " + plotID + ")";
                        DiscordPS.warning(error);
                        Notification.sendErrorEmbed(error);
                    }

                    statusComponent.setAccentColor(tag.getColor());
                    statusComponent.changeStatusMessage(StatusComponent.DisplayMessage.fromTag(tag));

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + layout);
            }
            updated.add(layout.build());
        }

        WebhookData updatedData = new WebhookDataBuilder()
                .suppressNotifications()
                .suppressMentions()
                .forceComponentV2()
                .setComponentsV2(updated)
                .build();

        if(modified) {
            imageList.forEach(updatedData::addFile);

            // Reference to previous attachment will get reset if there are new file attached.
            // We have to attach it back to the message to restore the attachment data.
            ownerList.forEach(member -> new MemberOwnable(member).getAvatarFile().ifPresent(updatedData::addFile));
        }

        return Optional.of(updatedData);
    }

    /**
     * Fetch a new status message by constructing a new {@link StatusEmbed} and build it as a webhook data.
     *
     * <p>This also fetch if (any) new button interactions is needed
     * from looking at the {@linkplain PlotEvent event} instance.</p>
     *
     * @param event The event that triggers this update action, can be null.
     * @param message The current message containing the status embed to be updated.
     * @param threadID The thread ID of this plot.
     * @param owner The owner of this plot data.
     * @param status The status of this plot.
     * @return New {@link StatusComponent} built as webhook data ready to be used in API call.
     * @param <T> The event representing this current status message.
     */
    @NotNull
    private <T extends PlotEvent>
    Optional<WebhookData> fetchStatusMessage(@Nullable T event,
                                             @NotNull Message message,
                                             @NotNull String threadID,
                                             @NotNull MemberOwnable owner,
                                             @NotNull ThreadStatus status) {

        StatusEmbed statusEmbed = new StatusEmbed(owner, status, this.getMessageReferenceURL(threadID, threadID));
        WebhookDataBuilder data = new WebhookDataBuilder().setEmbeds(Collections.singletonList(statusEmbed.build()));

        // No interaction to update
        if(message.getActionRows().isEmpty()) return Optional.of(data.build());

        // Check interaction to update
        ActionRow componentRow = message.getActionRows().getFirst();

        // Set new component if it is modified
        fetchInteractionButton(event, componentRow).ifPresent(data::setComponents);

        return Optional.of(data.build());
    }

    /**
     * Fetch the status interaction row if it should be updated or not
     *
     * @param plotEvent The event to determine interactions to update
     * @param interactionRow The action row object to be updated
     * @return Optional of the updated component, empty if there's nothing to update
     * @param <T> The type of plot event
     */
    @NotNull
    private <T extends PlotEvent>
    Optional<Collection<? extends ActionRow>> fetchInteractionButton(@Nullable T plotEvent,
                                                                     @NotNull ActionRow interactionRow) {
        List<Button> buttons = new ArrayList<>();

        switch(plotEvent) {
            case PlotClosureEvent ignored: // Make all interactive button disabled if the plot is closed
                interactionRow.getButtons().forEach(button -> PluginComponent.getOpt(this.plugin, button).ifPresentOrElse(component -> {
                    switch (AvailableButton.valueOf(component.getType())) {
                        case AvailableButton.FEEDBACK_BUTTON:
                            buttons.add(
                                Button.secondary(component.getRawID() + "/Locked",
                                Emoji.fromUnicode("U+1F512")).asDisabled()
                            );
                            break;
                        case AvailableButton.HELP_BUTTON:
                            buttons.add(
                                Button.primary(component.getRawID() + "/Locked",
                                button.getLabel()).asDisabled()
                            );
                            break;
                    }
                }, () -> {
                    if(buttons.isEmpty()) buttons.add(button.asDisabled());
                }));
                break;
            case PlotUndoEvent event:
                // Undo review: Filter out feedback button from the interaction row
                if(event instanceof PlotUndoReviewEvent)
                    interactionRow.getButtons().forEach(button ->
                        PluginComponent.getOpt(this.plugin, button).ifPresentOrElse(component -> {
                            if (AvailableButton.valueOf(component.getType()) == AvailableButton.HELP_BUTTON)
                                buttons.add(component.get());
                        }, () -> buttons.add(button))
                    );
                break;
            case null, default: return Optional.empty();
        }

        if(buttons.isEmpty()) return Optional.empty();
        else return Optional.of(Collections.singletonList(ActionRow.of(buttons)));
    }

    /**
     * Fetch a new feedback button into message.
     *
     * @param message The message to fetch a feedback button to.
     * @param onButtonSet Invoke when setting button with message ID and owner ID, use this to supply button style.
     * @return The updated component data built as {@link WebhookData}
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
            PluginComponent.getOpt(this.plugin, button).ifPresent(component -> {
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
                PluginButton component = new PluginButton(this.plugin, button);

                // If feedback button is already attached
                // update it to functional button
                // (the initial feedback button is set as "No Feedback"
                if(AvailableButton.valueOf(component.getType()) == AvailableButton.FEEDBACK_BUTTON) {

                    String label = button.getLabel();

                    buttons.add(switch (button.getStyle()) {
                        case DANGER -> button.withLabel(label = this.metadata.rejectedFeedbackLabel()).asEnabled();
                        case SUCCESS -> button.withLabel(label = this.metadata.approvedFeedbackLabel()).asEnabled();
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
