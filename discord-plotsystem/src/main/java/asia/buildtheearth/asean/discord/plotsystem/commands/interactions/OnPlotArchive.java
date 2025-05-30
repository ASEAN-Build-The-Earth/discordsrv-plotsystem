package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.Layout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class OnPlotArchive extends Interaction implements PlotInteraction {
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

    public int getPlotID() {
        return (int) plotID;
    }

    public Optional<WebhookEntry> getArchiveEntry() {
        return Optional.ofNullable(this.archiveEntry);
    }

    public Optional<Set<String>> getPreviousAttachment() {
        return Optional.ofNullable(this.previousAttachment);
    }

    public void setArchiveEntry(@Nullable WebhookEntry archiveEntry) {
        this.archiveEntry = archiveEntry;
    }

    public void setPreviousAttachment(@NotNull Set<String> previousAttachment) {
        this.previousAttachment = previousAttachment;
    }
}
