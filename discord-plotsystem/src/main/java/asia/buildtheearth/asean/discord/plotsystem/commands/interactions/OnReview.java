package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.ReviewEvent;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Payload for the command {@code /review edit} and {@code /review send}
 *
 * @see ReviewEvent
 */
public final class OnReview extends Interaction implements PlotFetchInteraction {
    /**
     * The plot ID to fetch on this interaction
     */
    public final long plotID;

    private @Nullable String newReviewMessage;
    private @Nullable WebhookEntry selectedEntry;
    private @Nullable List<SelectOption> fetchOptions;
    private @Nullable List<Message.Attachment> attachments;

    /**
     * Create a plot-delete payload
     *
     * @param userID The interaction owner ID (snowflake)
     * @param eventID The initial event ID (snowflake)
     * @param plotID The plot ID of this delete process
     */
    public OnReview(long userID, long eventID, long plotID) {
        super(userID, eventID);
        this.plotID = plotID;
    }

    /**
     * Get the new review message to exit to selected plot.
     *
     * @return The new review message to be applied
     */
    @Contract(pure = true)
    public @Nullable String getNewReviewMessage() {
        return this.newReviewMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPlotID() {
        return (int) this.plotID;
    }

    /**
     * Set the new review message for the selected entry
     *
     * @param newReviewMessage The new message formatted in markdown
     */
    @Contract(mutates = "this")
    public void setNewReviewMessage(@Nullable String newReviewMessage) {
        this.newReviewMessage = newReviewMessage;
    }

    /**
     * Set the selected entry of this reviewing edits
     *
     * @param selectedEntry The webhook entry to set
     */
    @Contract(mutates = "this")
    public void setSelectedEntry(@Nullable WebhookEntry selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    /**
     * Set the additional attachments for editing this review
     *
     * @param attachments The message attachment data
     */
    @Contract(mutates = "this")
    public void setAttachments(@Nullable List<Message.Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Get the additional attachment images for this review edits.
     *
     * @return An optional of attachment data if existed.
     */
    public Optional<List<Message.Attachment>> getOptAttachments() {
        return Optional.ofNullable(this.attachments);
    }

    /**
     * Get the selected entry for editing this review.
     *
     * @return Null if the entry has not been selected yet,
     *         otherwise return a {@link WebhookEntry} instance
     */
    @Contract(pure = true)
    public @Nullable WebhookEntry getSelectedEntry() {
        return selectedEntry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFetchOptions(@NotNull List<SelectOption> fetchOptions) {
        this.fetchOptions = new ArrayList<>(fetchOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable List<SelectOption> getFetchOptions() {
        return this.fetchOptions;
    }
}
