package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.LayoutComponentProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookStatusProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;
import asia.buildtheearth.asean.discord.plotsystem.core.system.MemberOwnable;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.components.api.TextDisplay;
import asia.buildtheearth.asean.discord.components.api.TextThumbnailSection;
import asia.buildtheearth.asean.discord.components.api.Thumbnail;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Locale;
import java.util.UUID;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.STATUS;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.StatusComponent.*;

public final class StatusComponent
    extends LayoutComponentProvider<Container, AvailableComponent.StatusComponent>
    implements LayoutComponent<Container> {

    private final int packedID;
    private Color accentColor;
    private StatusMessage statusMessage;
    private String thumbnailURL;
    private UUID thumbnailOwner;
    private String ownerName;

    /**
     * Create an empty status component with minimum data.
     *
     * @param id The packed ID of this component
     * @param layout The layout position of this component
     * @param color The accent color of this component
     */
    private StatusComponent(int id, int layout, Color color) {
        super(layout, STATUS, AvailableComponent.StatusComponent.VALUES);
        this.packedID = id;
        this.accentColor = color;
        this.setProvider(() -> new Container(this.packedID, this.accentColor, false));
    }

    /**
     * Create a complete status component by {@link PlotData}
     *
     * @param layout The layout position of this component
     * @param data The plot data to apply to this status
     */
    public StatusComponent(int layout, @NotNull PlotData data) {
        this(
            layout,
            data.getPrimaryStatus(),
            data.getOwner().getUniqueId(),
            data.getOwnerMentionOrName(),
            data.getAvatarAttachmentOrURL()
        );
    }

    /**
     * Create a complete status component
     *
     * @param layout The layout position of this component
     * @param status The primary status for this component
     * @param ownerUUID The plot owner as UUID
     * @param ownerName The formatted owner name to be displayed
     * @param avatarURL Owner's avatar URL to be displayed as thumbnail
     */
    public StatusComponent(int layout,
                           @NotNull ThreadStatus status,
                           @NotNull UUID ownerUUID,
                           @NotNull String ownerName,
                           @NotNull String avatarURL) {
        this(STATUS.pack(layout), layout, status.toTag().getColor());

        // Save ownerData in thumbnail description field,
        // so we can parse for avatar owner when rebuilding this component
        this.statusMessage = DisplayMessage.fromStatus(status);
        this.thumbnailOwner = ownerUUID;
        this.thumbnailURL = avatarURL;
        this.ownerName = ownerName;

        this.register(STATUS_THUMBNAIL,
            id -> new TextThumbnailSection(id, new Thumbnail(this.thumbnailURL, this.thumbnailOwner.toString()))
                .addTextDisplay(new TextDisplay(this.pack(STATUS_INFO), this.statusMessage.getMessage(this.ownerName)))
        );
    }

    /**
     * Restore a status component from raw data
     *
     * @param id The component ID to restore
     * @param layout The layout position to restore
     * @param color The accent color to restore
     * @param components The raw components data to rebuild
     *
     * @throws ParsingException If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    private StatusComponent(int id, int layout, int color, @NotNull DataArray components) throws ParsingException, IllegalArgumentException  {
        this(id, layout, new Color(color));
        super.rebuild(components, this::rebuildComponent);
    }

    /**
     * Rebuild a new status component from raw data
     *
     * @param rawData The raw {@link DataObject} to be rebuilt
     * @return A new status component instance with all the data restored
     * @throws ParsingException If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    @Contract("_ -> new")
    public static @NotNull StatusComponent from(@NotNull DataObject rawData) throws ParsingException, IllegalArgumentException {
        int id = rawData.getInt("id");
        int color = rawData.getInt("accent_color");
        int layout = AvailableComponent.unpackPosition(id);
        DataArray components = rawData.getArray("components");

        return new StatusComponent(id, layout, color, components);
    }

    public void setAccentColor(Color accentColor) {
        this.accentColor = accentColor;
    }

    public void changeStatusMessage(DisplayMessage displayMessage) {
        this.statusMessage = displayMessage;
    }

    public UUID getThumbnailOwner() {
        return thumbnailOwner;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String url) {
        this.thumbnailURL = url;
    }

    public void setThumbnailAttachment(String filename) {
        this.setThumbnailURL("attachment://" + filename);
    }

    public Container build() {
        return super.build(Container::addComponent);
    }

    /**
     * {@inheritDoc}
     */
    protected void rebuildComponent(int packedID, @NotNull DataObject component) throws ParsingException, IllegalArgumentException {
        switch(AvailableComponent.StatusComponent.get(AvailableComponent.unpackSubComponent(packedID))) {
            case STATUS_INFO:
            case STATUS_THUMBNAIL: {
                DataObject media = component.getObject("accessory").getObject("media");
                String description = component.getObject("accessory").getString("description");
                String url = media.getString("url");
                String proxyURL = media.getString("proxy_url");

                // Extract owner data from description (snowflake or UUID)
                try {
                    this.thumbnailOwner = UUID.fromString(description);
                    this.ownerName = new MemberOwnable(description).getOwnerMentionOrName();
                }
                catch (IllegalArgumentException ex) {
                    // Invalid UUID string, possible outdated data version
                    this.thumbnailOwner = null;
                    this.ownerName = description;
                }

                // Check for sent plot image attachment
                FileUtil.matchFilenameByURL(url, Constants.BUILDER_AVATAR_FILE, this::setThumbnailAttachment, failure ->
                    // Fallback to proxy URL if failed
                    FileUtil.matchFilenameByURL(proxyURL, Constants.BUILDER_AVATAR_FILE, this::setThumbnailAttachment, ignored ->
                        // Accept raw URL if nothing match
                        this.setThumbnailURL(failure)
                    )
                );

                // Restore display message
                int displayID = component.getArray("components").getObject(0).getInt("id");
                this.statusMessage = () -> component.getArray("components").getObject(0).getString("content");

                // Register the builder
                this.register(STATUS_THUMBNAIL, id -> new TextThumbnailSection(id,
                    new Thumbnail(this.thumbnailURL, this.thumbnailOwner == null? this.ownerName : this.thumbnailOwner.toString()))
                        .addTextDisplay(new TextDisplay(displayID, this.statusMessage.getMessage(this.ownerName)))
                );
                break;
            }
            case null, default: throw new IllegalArgumentException("Unknown sub type parsing StatusComponent");
        }
    }

    /**
     * Represents a message template for each thread status.
     * <p>
     * Use {@link #getMessage(String)} to insert a formatted owner name into
     * the message by replacing the <code>{owner}</code> placeholder.
     */
    @FunctionalInterface
    private interface StatusMessage {

        /**
         * Returns the raw message template for this status.
         * <p>
         * May include a <code>{owner}</code> placeholder
         * that can be dynamically replaced.
         *
         * @return The unformatted message template
         */
        @NotNull String getMessage();

        /**
         * Returns the formatted message with the owner's name inserted.
         *
         * @param owner The display name of the owner to insert
         * @return The formatted message with <code>{owner}</code> replaced
         */
        default @NotNull String getMessage(@NotNull String owner) {
            return getMessage().replace("{owner}", owner);
        }
    }


    /**
     * Saved message presets for displaying by thread status
     */
    public enum DisplayMessage implements StatusMessage, WebhookStatusProvider {
        ON_GOING("status-messages.on-going"),
        FINISHED("status-messages.finished"),
        REJECTED("status-messages.rejected"),
        APPROVED("status-messages.approved"),
        ARCHIVED("status-messages.archived"),
        ABANDONED("status-messages.abandoned");

        private final MessageLang message;

        DisplayMessage(String message) {
            this.message = () -> message;
        }

        public static DisplayMessage fromTag(@NotNull AvailableTag tag) {
            return valueOf(tag.name());
        }

        public static DisplayMessage fromStatus(@NotNull ThreadStatus status) {
            return valueOf(status.name().toUpperCase(Locale.ENGLISH));
        }

        @Override
        public @NotNull String getMessage() {
            return DiscordPS.getMessagesLang().get(this.message);
        }

        @Override
        public @NotNull ThreadStatus toStatus() {
            return valueOf(ThreadStatus.class, this.name().toLowerCase(Locale.ENGLISH));
        }

        @Override
        public @NotNull AvailableTag toTag() {
            return valueOf(AvailableTag.class, this.name());
        }
    }
}
