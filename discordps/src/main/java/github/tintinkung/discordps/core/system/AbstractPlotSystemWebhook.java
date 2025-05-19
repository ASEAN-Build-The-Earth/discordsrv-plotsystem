package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.events.PlotEvent;
import github.tintinkung.discordps.api.events.PlotSubmitEvent;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.components.api.ComponentV2;
import github.tintinkung.discordps.core.system.layout.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

sealed abstract class AbstractPlotSystemWebhook permits PlotSystemWebhook {

    /**
     * The webhook instance, provide all functionality in webhook management.
     */
    protected final ForumWebhook webhook;

    /**
     * Initialize webhook instance
     *
     * @param webhook The forum webhook manager instance
     */
    protected AbstractPlotSystemWebhook(ForumWebhook webhook) {
        this.webhook = webhook;
    }

    /**
     * Represent action that update layout component,
     * invoking with {@link Layout} which process data to
     * {@link github.tintinkung.discordps.core.system.WebhookDataBuilder.WebhookData} ready to request to API
     */
    @FunctionalInterface
    protected interface LayoutUpdater extends Function<Layout, Optional<WebhookDataBuilder.WebhookData>> {}

    /**
     * Represent action that update message status of a plot entry,
     * invoking with {@link Message} which process data to
     * {@link github.tintinkung.discordps.core.system.WebhookDataBuilder.WebhookData} ready to request to API
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
            Notification.sendErrorEmbed(PLOT_UPDATE_SQL_EXCEPTION.apply(event), ex.toString());
        }
    }

    /**
     * Create new thread for the given plot ID,
     * will fall back to {@link #addNewExistingPlot(PlotSystemThread, Consumer)}
     * if an entry already exist indication a re-claim plot.
     *
     * @param thread   The plot ID to be created
     * @param register If true, will register this plot in database entry.
     * @param force    By default, will check if the plot already exist in the database or not and it will override that plot entry if existed.
     *                 By setting this to {@code true}, will ignore the checks and create new thread forcefully.
     * @return Null only if plot entry does not exist per plot-system database, else a handled completable future.
     */
    protected abstract CompletableFuture<Void> newThreadForPlotID(@NotNull PlotSystemThread thread,
                                                                  boolean register,
                                                                  boolean force);

    protected abstract void registerNewPlot(@NotNull PlotData plotData,
                                            int plotID,
                                            long threadID);

    protected void registerNewPlot(PlotData plotData,
                                   int plotID,
                                   @NotNull BiConsumer<Consumer<MessageReference>, Runnable> initialMessage) {
        initialMessage.accept(
            message -> this.registerNewPlot(plotData, plotID, message.getMessageIdLong()),
            () -> Notification.sendErrorEmbed(PLOT_CREATION_EXCEPTION)
        );
    }

    /**
     * Suppose that a plot entry exist, fetch it with a new plot-system data as a new plot entry.
     *
     * @param thread The thread provider to get data from
     * @param onSuccess Action invoked on success with the plot's information
     * @return The handled future that completed when all queued action is done
     */
    @Nullable
    protected abstract CompletableFuture<Void> addNewExistingPlot(@NotNull PlotSystemThread thread,
                                                                  @Nullable Consumer<PlotData> onSuccess);

    /**
     * Suppose that a plot entry exist,
     * fetch it with from the existing entry and register if enabled.
     * This will fetch the plot as {@link github.tintinkung.discordps.api.events.PlotReclaimEvent} internally.
     *
     * @param entry The existing entry to be reclaimed as
     * @param register Whether to register this plot to database ot make untracked
     * @return The handled future that completed when all queued action is done
     */
    @Nullable
    protected CompletableFuture<Void> addNewExistingPlot(@NotNull WebhookEntry entry, boolean register) {
        final Consumer<PlotData> registerNewPlot = plotData -> this.registerNewPlot(plotData, entry.plotID(), entry.threadID());
        final PlotSystemThread thread = new PlotSystemThread(entry.plotID(), entry.threadID());

        return this.addNewExistingPlot(thread, register? registerNewPlot : null);
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
     * Update plot by the given action with specified event.
     *
     * @param action The update action that specify what plot entry to be updated
     * @param event The referring event that trigger this action
     * @param status The primary status to update to
     * @param whenComplete Invoked when the update action is completed returning this action
     * @param <T> The type of referring event that activate this action
     */
    public <T extends PlotEvent>
    void updatePlot(@NotNull PlotSystemThread.UpdateAction action,
                    @Nullable T event,
                    @NotNull ThreadStatus status,
                    @NotNull Consumer<PlotSystemThread.UpdateAction> whenComplete) {
        this.updatePlot(action, event, status).whenComplete((update, error) -> whenComplete.accept(update));
    }

    /**
     * Update plot by the given action with specified event.
     *
     * @param event The referring event that trigger this action
     * @param status The primary status to update to
     * @param <T> The type of referring event that activate this action
     */
    public <T extends PlotEvent>
    void updatePlot(@NotNull T event, @NotNull ThreadStatus status) {
        this.updatePlot(event, status, null);
    }

    /**
     * Update plot by the given event,
     * will create a new {@link PlotSystemThread.UpdateAction#fromEvent(PlotEvent)}
     * for updating plot by event information.
     *
     * @param event The referring event that trigger this action
     * @param status The primary status to update to
     * @param whenComplete Invoked when the update action is completed returning this action
     * @param <T> The type of referring event that activate this action
     */
    public <T extends PlotEvent> void updatePlot(@NotNull T event,
                                                 @NotNull ThreadStatus status,
                                                 @Nullable Consumer<PlotSystemThread.UpdateAction> whenComplete) {
        PlotSystemThread.UpdateAction action = PlotSystemThread.UpdateAction.fromEvent(event);

        if(action == null) return;

        if(event instanceof PlotSubmitEvent) PLOT_SUBMIT_NOTIFICATION.accept(action.plotID(), action.threadID());

        if(whenComplete == null) this.updatePlot(action, event, status);
        else this.updatePlot(action, event, status).thenAccept(whenComplete);
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
     *     <li>{@code on_going} {@code finished} Help & docs button</li>
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
        ActionRow documentationRow = ActionRow.of(PLOT_DOCUMENTATION_BUTTON);

        Function<Long, ActionRow> interactionRow = userID -> ActionRow.of(
                this.newHelpButton(messageID, userID, plotID),
                PLOT_DOCUMENTATION_BUTTON
        );

        Function<Long, ActionRow> approvedRow = userID -> ActionRow.of(
                Button.success(AvailableButton.FEEDBACK_BUTTON.resolve(messageID, userID, plotID), APPROVED_FEEDBACK_LABEL)
        );

        Function<Long, ActionRow> rejectedRow = userID -> ActionRow.of(
                Button.danger(AvailableButton.FEEDBACK_BUTTON.resolve(messageID, userID, plotID), REJECTED_FEEDBACK_LABEL)
        );

        return (owner.getOwnerDiscord().isPresent()) ? switch (status) {
            case on_going, finished -> List.of(interactionRow.apply(owner.getOwnerDiscord().get().getIdLong()));
            case approved -> List.of(approvedRow.apply(owner.getOwnerDiscord().get().getIdLong()));
            case rejected -> List.of(rejectedRow.apply(owner.getOwnerDiscord().get().getIdLong()));
            default -> null;
        } : switch (status) {
            case on_going, finished, rejected -> List.of(documentationRow);
            default -> null;
        };
    }

    protected static final String REJECTED_FEEDBACK_LABEL = "Show Reason";
    protected static final String APPROVED_FEEDBACK_LABEL = "View Feedback";
    protected static final String REJECTED_NO_FEEDBACK_LABEL = "No Reason Yet";
    protected static final String APPROVED_NO_FEEDBACK_LABEL = "No Feedback Yet";

    protected Button newHelpButton(long messageID, long ownerID, int plotID) {
        return Button.primary(AvailableButton.HELP_BUTTON.resolve(messageID, ownerID, plotID), "Help");
    }

    protected static final Button PLOT_DOCUMENTATION_BUTTON = Button.link(
            "https://asean.buildtheearth.asia/intro/getting-started/building-first-build/plot-system",
            "Documentation"
    );

    protected static final BiConsumer<PlotData, String> PLOT_CREATE_NOTIFICATION = (plotData, threadID) -> Notification.sendMessageEmbeds(
        new EmbedBuilder()
            .setTitle(":hammer_pick: New Plot Created at <#" + threadID + ">")
            .setDescription("By " + plotData.getOwnerMentionOrName() + " (Plot ID: " + plotData.getPlot().plotID() + ")")
            .setColor(Color.GREEN)
            .build()
    );

    // TODO: Expose this to config so we can decide who to ping
    protected static final BiConsumer<Integer, String> PLOT_SUBMIT_NOTIFICATION = (plotID, threadID) -> Notification.sendMessageEmbeds(
        "<@501366655624937472> <@480350715735441409> <@728196906395631637> <@939467710247604224> <@481786697860775937>",
        new EmbedBuilder()
            .setTitle(":bell: Plot #" + plotID + " Has just submitted <t:" + Instant.now().getEpochSecond() + ":R>")
            .setDescription("Tracker: <#" + threadID + ">")
            .setColor(Color.CYAN)
            .build()
    );

    protected static final Function<Integer, String> PLOT_CREATE_RUNTIME_EXCEPTION = plotID ->
            "Runtime exception **creating** new plot thread, "
            + "The plot ID #`" + plotID + "` "
            + "may or may not be tracked correctly by the system depending on the error.";

    protected static final Function<PlotEvent, String> PLOT_UPDATE_RUNTIME_EXCEPTION = event ->
            "Runtime exception **updating** plot data (" + event.getClass().getSimpleName() + "), "
            + "The plot ID #`" + event.getPlotID() + "` "
            + "may or may not be tracked correctly by the system depending on the error.";

    protected static final Function<Integer, String> PLOT_FEEDBACK_SQL_EXCEPTION = plotID ->
            "Failed update webhook feedback in the database,"
            + "The plot ID #`" + plotID + "` "
            + "feedback button will not be functional, the user will not be able to view it.";

    protected static final Function<PlotEvent, String> PLOT_UPDATE_SQL_EXCEPTION = event ->
            "SQL exception occurred **updating** plot data (" + event.getClass().getSimpleName() + "), "
            + "The plot ID #`" + event.getPlotID() + "` "
            + "may or may not be tracked by the system depending on the error.";

    protected static final Function<Integer, String> PLOT_ENTRY_NOT_EXIST = plotID ->
            "Failed to create and register new plot because its data does not exist in plot-system database, "
            + "The plot ID #`" + plotID + "` will not be tracked by the system.";

    protected static final String PLOT_UPDATING_EXCEPTION = "Error occurred while updating plot data.";
    protected static final String PLOT_CREATION_EXCEPTION = "Failed to resolve data trying to create new plot.";
    protected static final String PLOT_REGISTER_EXCEPTION = "Failed to resolve data trying to register new plot to database.";
    protected static final String PLOT_FEEDBACK_EXCEPTION = "Error occurred while setting plot's feedback data.";

    protected static final Consumer<? super Throwable> ON_PLOT_UPDATING_EXCEPTION = error -> {
        DiscordPS.error(PLOT_UPDATING_EXCEPTION, error);
        Notification.sendErrorEmbed(PLOT_UPDATING_EXCEPTION, error.toString());
    };

    protected static final BiConsumer<Integer, ? super Throwable> ON_PLOT_CREATION_EXCEPTION = (plotID, error) -> {
        DiscordPS.error(PLOT_CREATION_EXCEPTION, error);
        Notification.sendErrorEmbed(PLOT_CREATE_RUNTIME_EXCEPTION.apply(plotID), error.toString());
    };

    protected static final Consumer<? super Throwable> ON_PLOT_REGISTER_EXCEPTION = error -> {
        DiscordPS.error(PLOT_REGISTER_EXCEPTION, error);
        Notification.sendErrorEmbed(PLOT_REGISTER_EXCEPTION, error.toString());
    };

    protected static final Consumer<? super Throwable> ON_PLOT_FEEDBACK_EXCEPTION = error -> {
        DiscordPS.error(PLOT_FEEDBACK_EXCEPTION, error);
        Notification.sendErrorEmbed(PLOT_FEEDBACK_EXCEPTION, error.toString());
    };

    protected static final BiConsumer<PlotEvent, ? super Throwable> ON_PLOT_UPDATE_EXCEPTION = (event, error) -> {
        DiscordPS.error(PLOT_UPDATE_RUNTIME_EXCEPTION.apply(event), error);
        Notification.sendErrorEmbed(PLOT_UPDATE_RUNTIME_EXCEPTION.apply(event), error.toString());
    };

    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_BUTTON_ATTACH_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread interaction attach action returned an exception", failure);
            Notification.sendErrorEmbed("Failed to attach interactions (buttons) to a plot entry", failure.toString());
        }
    };

    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_THREAD_EDIT_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread data update action returned an exception", failure);
            Notification.sendErrorEmbed("Failed to update thread data (this include thread name and its tags)", failure.toString());
        }
    };

    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_MESSAGE_EDIT_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread message update action returned an exception", failure);
            Notification.sendErrorEmbed("Failed to update plot status message.", failure.toString());
        }
    };

    protected static final BiConsumer<Optional<?>, ? super Throwable> HANDLE_LAYOUT_EDIT_ERROR = (success, failure) -> {
        if(failure != null) {
            DiscordPS.error("A thread layout update action returned an exception", failure);
            Notification.sendErrorEmbed("Failed to update thread layout.", failure.toString());
        }
    };
}