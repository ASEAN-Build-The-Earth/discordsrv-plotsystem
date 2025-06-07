package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Payload for the command {@code /plotctl archive} and {@code /review archive}
 *
 * @see asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotArchiveEvent
 */
public final class OnPlotArchive extends Interaction implements PlotInteraction {
    /**
     * The plot ID to be archived
     */
    public final long plotID;

    /**
     * Whether to override an already existing entry
     */
    public final boolean override;

    /**
     * Attachment if this plot already have some images to archive
     */
    private @Nullable Set<String> previousAttachment;

    /**
     * Webhook entry to be updated as archive if overriding is enabled
     */
    private  @Nullable WebhookEntry archiveEntry;

    /**
     * Create a plot-archive payload
     *
     * @param userID The interaction owner ID (snowflake)
     * @param eventID The initial event ID (snowflake)
     * @param plotID The plot ID of this archive process
     */
    public OnPlotArchive(long userID, long eventID, long plotID, boolean override) {
        super(userID, eventID);
        this.plotID = plotID;
        this.override = override;
    }

    /**
     * {@inheritDoc}
     */
    public int getPlotID() {
        return (int) plotID;
    }

    /**
     * Get the interaction archiving entry.
     *
     * @return Optional of the entry, returning empty
     *         if the archive entry has not been set properly.
     */
    public Optional<WebhookEntry> getArchiveEntry() {
        return Optional.ofNullable(this.archiveEntry);
    }

    /**
     * Get the previously attached attachment data of the archiving entry.
     *
     * @return An optional of the attachment data where,
     *         if no previous attachment is found, will return an empty.
     */
    public Optional<Set<String>> getPreviousAttachment() {
        return Optional.ofNullable(this.previousAttachment);
    }

    /**
     * Set an archive entry to be used in this interaction.
     *
     * @param archiveEntry The {@link WebhookEntry} data to be used.
     */
    public void setArchiveEntry(@Nullable WebhookEntry archiveEntry) {
        this.archiveEntry = archiveEntry;
    }

    /**
     * Set the previous attached attachments from the archiving entry.
     *
     * @param previousAttachment The set of previous attachments if found.
     */
    public void setPreviousAttachment(@NotNull Set<String> previousAttachment) {
        this.previousAttachment = previousAttachment;
    }
}
