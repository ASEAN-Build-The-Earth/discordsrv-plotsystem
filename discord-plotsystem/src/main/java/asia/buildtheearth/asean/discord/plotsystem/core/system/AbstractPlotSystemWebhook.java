package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.api.events.NotificationType;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotNotificationEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.PluginProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.LangPaths;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.components.api.ComponentV2;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotNotification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


import static asia.buildtheearth.asean.discord.plotsystem.core.system.PlotSystemThread.THREAD_NAME;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.ErrorMessage;

/**
 * Abstract for plot-system webhook management
 *
 * @see PlotSystemWebhook
 */
sealed abstract class AbstractPlotSystemWebhook extends PluginProvider permits PlotSystemWebhook {

    /**
     * The webhook instance, provide all functionality in webhook management.
     */
    protected final ForumWebhook webhook;

    /**
     * memorized metadata per instance
     *
     * @see Metadata
     */
    protected final Metadata metadata;

    /**
     * Initialize webhook instance
     *
     * @param webhook The forum webhook manager instance
     */
    protected AbstractPlotSystemWebhook(DiscordPS plugin, ForumWebhook webhook) {
        super(plugin);
        this.webhook = webhook;
        this.metadata = new Metadata(
            DiscordPS.getMessagesLang().get(PlotInformation.HELP_LABEL),
            DiscordPS.getMessagesLang().get(PlotInformation.REJECTED_FEEDBACK_LABEL),
            DiscordPS.getMessagesLang().get(PlotInformation.APPROVED_FEEDBACK_LABEL),
            DiscordPS.getMessagesLang().get(PlotInformation.REJECTED_NO_FEEDBACK_LABEL),
            DiscordPS.getMessagesLang().get(PlotInformation.APPROVED_NO_FEEDBACK_LABEL),
            DiscordPS.getMessagesLang().get(PlotInformation.NEW_FEEDBACK_NOTIFICATION),
            Button.link(
                DiscordPS.getMessagesLang().get(PlotInformation.DOCS_URL),
                DiscordPS.getMessagesLang().get(PlotInformation.DOCS_LABEL)
            )
        );
    }

    /**
     * Represent action that update layout component,
     * invoking with {@link Layout} which process data to
     * {@link asia.buildtheearth.asean.discord.components.WebhookDataBuilder.WebhookData} ready to request to API
     */
    @FunctionalInterface
    protected interface LayoutUpdater extends Function<Layout, Optional<WebhookDataBuilder.WebhookData>> {}

    /**
     * Represent action that update message status of a plot entry,
     * invoking with {@link Message} which process data to
     * {@link asia.buildtheearth.asean.discord.components.WebhookDataBuilder.WebhookData} ready to request to API
     */
    @FunctionalInterface
    protected interface MessageUpdater extends Function<Message, Optional<WebhookDataBuilder.WebhookData>> {}

    /**
     * Restore thread's component layout from thread ID
     *
     * @param threadID The thread ID as snowflake string
     * @return The future when completed, return the thread layout as an optional (empty if failed)
     */
    public @NotNull CompletableFuture<Optional<Layout>> getThreadLayout(String threadID) {
        return webhook.getInitialLayout(threadID, true).submit();
    }

    /**
     * Query update to status entry for the giving message ID
     *
     * @param messageID The entry as message ID to update
     * @param status The status to update into
     * @param event Update event for debugging if an SQL exception occurs
     */
    public void updateEntryStatus(long messageID, ThreadStatus status, PlotEvent event) {
        try {
            WebhookEntry.updateThreadStatus(messageID, status);
        } catch (SQLException ex) {
            Notification.sendErrorEmbed(
                ErrorMessage.PLOT_UPDATE_SQL_EXCEPTION,
                ex.toString(),
                String.valueOf(event.getPlotID()),
                event.getClass().getSimpleName()
            );
        }
    }

    /**
     * Create new thread for the given plot ID,
     * will fall back to {@link AbstractPlotSystemWebhook#addNewExistingPlot(WebhookEntry, PlotCreateData, boolean)}
     * if an entry already existed indication a re-claim plot.
     *
     * @param thread   The plot ID to be created
     * @param plot     The initial plot data to create the thread with
     * @param register If true, will register this plot in database entry.
     * @param force    By default, will check if the plot already exist in the database or not and it will override that plot entry if existed.
     *                 By setting this to {@code true}, will ignore the checks and create new thread forcefully.
     * @return Null only if plot entry does not exist per plot-system database, else a handled completable future.
     */
    protected abstract CompletableFuture<Void> newThreadForPlotID(@NotNull PlotSystemThread thread,
                                                                  @NotNull PlotCreateData plot,
                                                                  boolean register,
                                                                  boolean force);

    /**
     * Create and register a plot status by a given plot data.
     * Will create an initial status message with help button and documentation link.
     *
     * @param plotData The plot information
     * @param plotID The plot ID
     * @param threadID The thread ID this plot is created on
     */
    protected abstract void registerNewPlot(@NotNull PlotData plotData,
                                            int plotID,
                                            long threadID);

    /**
     * Wrapper for {@link AbstractPlotSystemWebhook#registerNewPlot(PlotData, int, long)}
     * to register new plot based on the initial message reference.
     *
     * @param plotData The plot data to register this plot
     * @param plotID The plot ID to register this plot
     * @param initialMessage Action required to receive message reference for registering this plot.
     *                       Conventionally is using as {@link Optional#ifPresentOrElse(Consumer, Runnable)}
     */
    protected void registerNewPlot(PlotData plotData,
                                   int plotID,
                                   @NotNull BiConsumer<Consumer<MessageReference>, Runnable> initialMessage) {
        initialMessage.accept(
            message -> this.registerNewPlot(plotData, plotID, message.getMessageIdLong()),
            () -> Notification.notify(ErrorMessage.PLOT_CREATE_UNKNOWN_EXCEPTION)
        );
    }

    /**
     * Suppose that a plot entry exist, fetch it with a new plot-system data as a new plot entry.
     *
     * @param thread The thread provider to get data from
     * @param plot The new plot creation data to be edited to this existing plot
     * @param removeMemberID Optionally remove the previous owner if exist
     * @param onSuccess Action invoked on success with the plot's information
     * @return The handled future that completed when all queued action is done
     */
    @Nullable
    protected abstract CompletableFuture<Void> addNewExistingPlot(@NotNull PlotSystemThread thread,
                                                                  @NotNull PlotCreateData plot,
                                                                  @Nullable String removeMemberID,
                                                                  @Nullable Consumer<PlotData> onSuccess);

    /**
     * Suppose that a plot entry exist,
     * fetch it with from the existing entry and register if enabled.
     * This will fetch the plot as {@link asia.buildtheearth.asean.discord.plotsystem.api.events.PlotReclaimEvent} internally.
     *
     * @param entry The existing entry to be reclaimed as
     * @param register Whether to register this plot to database ot make untracked
     * @return The handled future that completed when all queued action is done
     */
    @Nullable
    protected CompletableFuture<Void> addNewExistingPlot(@NotNull WebhookEntry entry,
                                                         @NotNull PlotCreateData plot,
                                                         boolean register) {
        final Consumer<PlotData> registerNewPlot = plotData -> this.registerNewPlot(plotData, entry.plotID(), entry.threadID());
        final PlotSystemThread thread = new PlotSystemThread(entry.plotID(), entry.threadID());
        final Optional<String> removeMemberID = Optional.ofNullable(entry.ownerID());
        return this.addNewExistingPlot(thread, plot, removeMemberID.orElse(null), register? registerNewPlot : null);
    }

    /**
     * Update plot by the given action with specified event.
     *
     * @param action The update action that specify what plot entry to be updated
     * @param event The referring event that trigger this action, null event will be defined as a system fetch
     * @param status The primary status to update to
     * @return The same action as a future that is completed when all staged action is completed
     * @param <T> The type of referring event that activate this action
     */
    @NotNull
    public abstract <T extends PlotEvent>
    CompletableFuture<PlotSystemThread.UpdateAction> updatePlot(@NotNull PlotSystemThread.UpdateAction action,
                                                                @Nullable T event,
                                                                @NotNull ThreadStatus status);

    /**
     * Update plot by the given event creating a new {@link PlotSystemThread.UpdateAction#fromEvent(PlotEvent)}
     * for updating plot by the event information.
     *
     * <p>Note: This will fetch a latest plot entry to be updated.
     * To update more specifically, provide {@link PlotSystemThread.UpdateAction}
     * and use {@link #updatePlot(PlotSystemThread.UpdateAction, PlotEvent, ThreadStatus)} instead.</p>
     *
     * @param event The referring event that trigger this action
     * @param status The primary status to update to
     * @param whenComplete Invoked when the update action is completed returning this action
     * @param <T> The type of referring event that activate this action
     */
    public <T extends PlotEvent> void updatePlot(@NotNull T event,
                                                 @NotNull ThreadStatus status,
                                                 @NotNull Consumer<PlotSystemThread.UpdateAction> whenComplete) {
        PlotSystemThread.UpdateAction action = PlotSystemThread.UpdateAction.fromEvent(event);
        if(action == null) return;
        this.updatePlot(action, event, status).thenAccept(whenComplete);
    }

    /**
     * Create a new plot thread to webhook forum as well as attach media file to its initial message.
     *
     * @param threadName The thread name
     * @param plotData The plot data information
     * @param componentsV2 The layout component the thread will be initialized with
     * @return Queued future to be handled, on success will return
     *         a non-empty optional with the initial message reference as value
     */
    @NotNull
    public CompletableFuture<Optional<MessageReference>> createNewPlotThread(@NotNull String threadName,
                                                                             @NotNull PlotData plotData,
                                                                             @NotNull Collection<? extends ComponentV2> componentsV2) {

        WebhookDataBuilder.WebhookData data = new WebhookDataBuilder()
                .setThreadName(threadName)
                .setComponentsV2(componentsV2)
                .forceComponentV2()
                .build();

        // Attach files
        plotData.getAvatarFile().ifPresent(data::addFile);
        if(!plotData.getImageFiles().isEmpty()) plotData.getImageFiles().forEach(data::addFile);

        return this.webhook
            .newThreadFromWebhook(data, plotData.getStatusTags(), true, true)
            .submit();
    }

    /**
     * Create interactive layout by status as follows:
     * <ul>
     *     <li>{@code on_going} {@code finished} Help and Docs button</li>
     *     <li>{@code approved} Approval feedback button</li>
     *     <li>{@code rejected} Rejected reason button</li>
     * </ul>
     * <p>Note: interactive button will not be added if owner does not have a discord account.</p>
     *
     * @param plotID The plot ID of this interaction
     * @param messageID The message ID to attach this interaction
     * @param status The plot status to determined interaction layout
     * @param owner The plot owner that can interaction with the layout
     * @return The component layout as a singleton list of action row, null if the given status has no interactions
     */
    protected @Nullable List<ActionRow> createInitialInteraction(int plotID,
                                                                 long messageID,
                                                                 @NotNull ThreadStatus status,
                                                                 @NotNull MemberOwnable owner) {
        ActionRow documentationRow = ActionRow.of(this.metadata.documentationButton());

        Function<Long, ActionRow> interactionRow = userID -> ActionRow.of(
            this.newHelpButton(messageID, userID, plotID),
            this.metadata.documentationButton()
        );

        Function<Long, ActionRow> approvedRow = userID -> ActionRow.of(Button.success(
            AvailableButton.FEEDBACK_BUTTON.resolve(messageID, userID, plotID),
            this.metadata.approvedFeedbackLabel())
        );

        Function<Long, ActionRow> rejectedRow = userID -> ActionRow.of(Button.danger(
            AvailableButton.FEEDBACK_BUTTON.resolve(messageID, userID, plotID),
            this.metadata.rejectedFeedbackLabel())
        );

        // Interactive row if owner has discord to interact with it
        List<ActionRow> interactiveRow = switch (status) {
            case on_going, finished -> List.of(interactionRow.apply(owner.getOwnerDiscord().get().getIdLong()));
            case approved -> List.of(approvedRow.apply(owner.getOwnerDiscord().get().getIdLong()));
            case rejected -> List.of(rejectedRow.apply(owner.getOwnerDiscord().get().getIdLong()));
            default -> null;
        };

        // Static row if owner discord is not present
        List<ActionRow> staticRow = switch (status) {
            case on_going, finished, rejected -> List.of(documentationRow);
            default -> null;
        };

        return owner.getOwnerDiscord().isPresent()? interactiveRow : staticRow;
    }

    /**
     * Optionally get archive thread name if configured a prefix for it.
     *
     * @param plotID The plotID to of the thread
     * @return Optional if the config {@link LangPaths#ARCHIVED_PREFIX} specified a prefix for an archival thread.
     */
    public static Optional<PlotSystemThread.ThreadNameApplier> getOptArchiveThreadName(int plotID) {
        String prefix = DiscordPS.getSystemLang().get(LangPaths.ARCHIVED_PREFIX);
        PlotSystemThread.ThreadNameApplier threadName = null;

        if(!StringUtils.isBlank(prefix))
            threadName = owner -> prefix + " " + THREAD_NAME.apply(plotID, owner.formatOwnerName());

        return Optional.ofNullable(threadName);
    }

    /**
     * Dispatches a plot notification event and handles message display logic if not cancelled.
     *
     * <p>Conventionally using the sender for forwarding to one of {@link #sendNotification(PlotNotification, String, Function)}
     * and using the received {@link asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotMessage PlotMessage}
     * To send {@link Notification} to the system channel</p>
     *
     * @param type     The {@link NotificationType} representing the notification to send
     * @param plotID   The ID of the plot this notification is related to
     * @param message  A function to resolve the localized message content to display,
     *                 given the {@link NotificationType}
     * @param sender   A bi-consumer to handle the delivery logic of the notification,
     *                 accepting the {@link PlotNotification} and its resolved {@code PlotMessage}
     *
     * @see PlotNotificationEvent
     * @see NotificationType
     * @see asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotMessage
     */
    public void onNotification(@NotNull NotificationType type,
                               int plotID,
                               @NotNull Function<@NotNull NotificationType, asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotMessage> message,
                               @NotNull BiConsumer<@NotNull PlotNotification, asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotMessage> sender) {
        @SuppressWarnings("deprecation")
        PlotNotificationEvent notification = DiscordPS.getPlugin().callEvent(
            new PlotNotificationEvent(plotID, type)
        );

        if(notification == null || notification.isCancelled()) return;

        sender.accept(PlotNotification.from(type), message.apply(type));
    }

    /**
     * Dispatches a plot notification event and handles delivery if not cancelled.
     *
     * <p>Conventionally using the sender for forwarding to one of {@link #sendNotification(PlotNotification, String, Function)}
     * and optionally send {@link Notification} to the system channel</p>
     *
     * @param type     The {@link NotificationType} representing the notification to send
     * @param plotID   The ID of the plot this notification is related to
     * @param sender   A consumer to handle the delivery logic of the notification,
     *                 accepting the {@link PlotNotification} instance
     *
     * @see PlotNotificationEvent
     * @see NotificationType
     */
    public void onNotification(@NotNull NotificationType type,
                               int plotID,
                               @NotNull Consumer<@NotNull PlotNotification> sender) {
        @SuppressWarnings("deprecation")
        PlotNotificationEvent notification = DiscordPS.getPlugin().callEvent(
            new PlotNotificationEvent(plotID, type)
        );

        if(notification == null || notification.isCancelled()) return;

        sender.accept(PlotNotification.from(type));
    }

    /**
     * Implementation for sending a plot notification message to a thread.
     * This is skipped if the configured notification by {@link PlotNotification} is set to {@code false}.
     *
     * @param type The notification message to send to
     * @param threadID The thread ID to send notification to
     * @param content The content of the notification as a modifier function
     *
     * @see #sendNotification(PlotNotification, String)
     * @see #sendNotification(PlotNotification, String, String)
     * @see #sendNotification(PlotNotification, String, String, String)
     */
    protected abstract void sendNotification(@NotNull PlotNotification type,
                                             @NotNull String threadID,
                                             @NotNull Function<String, String> content);

    /**
     * Send a plot notification message with no placeholder variable
     *
     * @param type  The notification message to send to
     * @param threadID The thread ID to send notification to
     */
    public void sendNotification(@NotNull PlotNotification type,
                                 @NotNull String threadID) {
        this.sendNotification(type, threadID, Function.identity());
    }

    /**
     * Send a plot notification message with owner ID placeholder
     *
     * @param type The notification message to send to
     * @param threadID The thread ID to send notification to
     *                 <i>(placeholder: {@code {threadID}})</i>
     * @param owner Owner ID to apply to this notification message
     *              <i>(placeholder: {@code {owner}})</i>
     */
    public void sendNotification(@NotNull PlotNotification type,
                                 @NotNull String threadID,
                                 @Nullable String owner) {
        this.sendNotification(type, threadID, content -> content
                .replace(Format.THREAD_ID, threadID)
                .replace(Format.OWNER, owner == null? LanguageFile.NULL_LANG : owner));
    }

    /**
     * Special notification that notifies the review button label.
     *
     * @param type The notification message to send to
     * @param threadID The thread ID to send notification to
     * @param owner  Owner ID to apply to this notification message
     *               <i>(placeholder: {@code {owner}})</i>
     * @param label  Button label apply to this notification message
     *               <i>(placeholder: {@code {label}})</i>
     */
    protected void sendNotification(@NotNull PlotNotification type,
                                    @NotNull String label,
                                    @NotNull String threadID,
                                    @Nullable String owner) {
        this.sendNotification(type, threadID, content -> content
                .replace(Format.LABEL, label)
                .replace(Format.OWNER, owner == null? LanguageFile.NULL_LANG : owner));
    }

    /** Create a plot's help button by owner and message. */
    protected Button newHelpButton(long messageID, long ownerID, int plotID) {
        return Button.primary(AvailableButton.HELP_BUTTON.resolve(messageID, ownerID, plotID), this.metadata.helpButtonLabel());
    }

    /** Unknown error when overriding plot data, happens from an exception in completable future. */
    protected static final Consumer<? super Throwable> ON_PLOT_OVERRIDING_EXCEPTION = error -> {
        DiscordPS.error("Error occurred adding new plot to existing data.", error);
        Notification.sendErrorEmbed(ErrorMessage.PLOT_UPDATE_UNKNOWN_EXCEPTION, error.toString());
    };

    /** Unknown error when creating new plot, happens from an exception in completable future. */
    protected static final BiConsumer<Integer, ? super Throwable> ON_PLOT_CREATION_EXCEPTION = (plotID, error) -> {
        DiscordPS.error("Failed to resolve data trying to create new plot.", error);
        Notification.sendErrorEmbed(ErrorMessage.PLOT_CREATE_EXCEPTION, error.toString(), String.valueOf(plotID));
    };

    /** Exception during plot registration, happen if {@link ForumWebhook#sendMessageInThread(String, WebhookDataBuilder.WebhookData, boolean, boolean) failed.} */
    protected static final BiConsumer<Integer, ? super Throwable> ON_PLOT_REGISTER_EXCEPTION = (plotID, error) -> {
        DiscordPS.error("Failed to resolve data trying to register new plot to database.", error);
        Notification.sendErrorEmbed(ErrorMessage.PLOT_REGISTER_ENTRY_EXCEPTION, error.toString(), String.valueOf(plotID));
    };

    /** Unknown error when setting feedback button, happens from an exception in completable future. */
    protected static final Consumer<? super Throwable> ON_PLOT_FEEDBACK_EXCEPTION = error -> {
        DiscordPS.error("Error occurred while setting plot's feedback data.", error);
        Notification.sendErrorEmbed(ErrorMessage.PLOT_FEEDBACK_UNKNOWN_EXCEPTION, error.toString());
    };

    /** Unknown error when updating plot data, happens from an exception in completable future. */
    protected static final BiConsumer<PlotEvent, ? super Throwable> ON_PLOT_UPDATE_EXCEPTION = (event, error) -> {
        DiscordPS.error("Error occurred while updating plot data.", error);
        Notification.sendErrorEmbed(
            ErrorMessage.PLOT_UPDATE_EXCEPTION,
            error.toString(),
            String.valueOf(event.getPlotID()),
            event.getClass().getSimpleName()
        );
    };

    /**
     * Consumer to handle for button attaching action.
     *
     * @see CompletableFuture#whenComplete(BiConsumer)
     */
    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_BUTTON_ATTACH_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread interaction attach action returned an exception", failure);
            Notification.sendErrorEmbed(ErrorMessage.FAILED_ATTACH_BUTTON, failure.toString());
        }
    };

    /**
     * Consumer to handle for thread editing action.
     *
     * @see CompletableFuture#whenComplete(BiConsumer)
     */
    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_THREAD_EDIT_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread data update action returned an exception", failure);
            Notification.sendErrorEmbed(ErrorMessage.FAILED_THREAD_EDIT, failure.toString());
        }
    };

    /**
     * Consumer to handle for status message editing action.
     *
     * @see CompletableFuture#whenComplete(BiConsumer)
     */
    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_MESSAGE_EDIT_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread message update action returned an exception", failure);
            Notification.sendErrorEmbed(ErrorMessage.FAILED_MESSAGE_EDIT, failure.toString());
        }
    };

    /**
     * Consumer to handle for plot layout editing action.
     *
     * @see CompletableFuture#whenComplete(BiConsumer)
     */
    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_LAYOUT_EDIT_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread layout update action returned an exception", failure);
            Notification.sendErrorEmbed(ErrorMessage.FAILED_LAYOUT_EDIT, failure.toString());
        }
    };

    /**
     * Consumer to handle for plot's previous owner removal.
     *
     * @see CompletableFuture#whenComplete(BiConsumer)
     */
    protected static final BiConsumer<Void, ? super Throwable> HANDLE_EDIT_MEMBER_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread member edit action returned an exception", failure);
            Notification.sendErrorEmbed(ErrorMessage.FAILED_EDIT_MEMBER, failure.toString());
        }
    };

    /**
     * Plot Metadata from messages language file, mostly button label data.
     * <p>Below are the default label of each buttons</p>
     *
     * @param helpButtonLabel "Help" label
     * @param rejectedFeedbackLabel "Show Reason" label
     * @param approvedFeedbackLabel "View Feedback" label
     * @param rejectedNoFeedbackLabel "No Feedback Yet" label
     * @param approvedNoFeedbackLabel "No Feedback Yet" label
     * @param documentationButton Documentation URL button
     */
    protected record Metadata(
            String helpButtonLabel,
            String rejectedFeedbackLabel,
            String approvedFeedbackLabel,
            String rejectedNoFeedbackLabel,
            String approvedNoFeedbackLabel,
            String newFeedbackNotification,
            Button documentationButton
    ) { }
}