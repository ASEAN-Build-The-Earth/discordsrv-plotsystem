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
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private @Nullable String attachment = null;

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
     * Create a complete status component by {@link PlotData},
     * <strong>attaching</strong> avatar thumbnail image file.
     *
     * @param layout The layout position of this component
     * @param data The plot data to apply to this status
     * @param avatarFilename Owner's avatar URL to be displayed as thumbnail
     */
    public StatusComponent(int layout,
                           @NotNull PlotData data,
                           @NotNull String avatarFilename) {
        this(
                layout,
                data.getPrimaryStatus(),
                data.getOwner().getUniqueId(),
                data.getOwnerMentionOrName(),
                avatarFilename
        );
    }

    /**
     * Create a complete status component by {@link PlotData},
     *      * displaying <strong>HTTP URL</strong> avatar thumbnail image.
     *
     * @param layout The layout position of this component
     * @param data The plot data to apply to this status
     * @param avatarURL Owner's avatar URL to be displayed as thumbnail
     */
    public StatusComponent(int layout,
                           @NotNull PlotData data,
                           @NotNull URL avatarURL) {
        this(
                layout,
                data.getPrimaryStatus(),
                data.getOwner().getUniqueId(),
                data.getOwnerMentionOrName(),
                avatarURL
        );
    }

    /**
     * Create a complete status component,
     * <strong>attaching</strong> avatar thumbnail image file.
     *
     * @param layout The layout position of this component
     * @param status The primary status for this component
     * @param ownerUUID The plot owner as UUID
     * @param ownerName The formatted owner name to be displayed
     * @param avatarFilename Owner's avatar URL to be displayed as thumbnail
     */
    public StatusComponent(int layout,
                           @NotNull ThreadStatus status,
                           @NotNull UUID ownerUUID,
                           @NotNull String ownerName,
                           @NotNull String avatarFilename) {
        this(STATUS.pack(layout), layout, status.toTag().getColor());

        this.attachment = avatarFilename;

        this.registerStatusInfo(DisplayMessage.fromStatus(status), ownerUUID, ownerName);
        this.setThumbnailAttachment(avatarFilename);
        this.register(STATUS_THUMBNAIL, this::registerStatusThumbnail);
    }

    /**
     * Create a complete status component,
     * displaying <strong>HTTP URL</strong> avatar thumbnail image.
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
                           @NotNull URL avatarURL) {
        this(STATUS.pack(layout), layout, status.toTag().getColor());
        this.setThumbnailURL(avatarURL.toString());
        this.registerStatusInfo(DisplayMessage.fromStatus(status), ownerUUID, ownerName);
        this.register(STATUS_THUMBNAIL, this::registerStatusThumbnail);
    }

    private TextThumbnailSection registerStatusThumbnail(int packedID) {
        return registerStatusThumbnail(packedID, this.pack(STATUS_INFO));
    }

    private TextThumbnailSection registerStatusThumbnail(int packedID, int displayID) {
        // Save ownerData in thumbnail description field,
        // so we can parse for avatar owner when rebuilding this component
        String identifier = this.thumbnailOwner == null? this.ownerName : this.thumbnailOwner.toString();
        String description = this.attachment != null? identifier + ':' + this.attachment : identifier;
        Thumbnail thumbnail = new Thumbnail(this.thumbnailURL, description);
        TextDisplay text = new TextDisplay(displayID, this.statusMessage.getMessage(this.ownerName));
        return new TextThumbnailSection(packedID, thumbnail).addTextDisplay(text);
    }

    private void registerStatusInfo(@NotNull StatusMessage status,
                                    @NotNull UUID ownerUUID,
                                    @NotNull String ownerName) {
        this.statusMessage = status;
        this.thumbnailOwner = ownerUUID;
        this.ownerName = ownerName;
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

    /**
     * Set the container accent color of this component
     *
     * @param accentColor The color
     */
    public void setAccentColor(Color accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Change the display message
     *
     * @param displayMessage All possible display messages
     */
    public void changeStatusMessage(DisplayMessage displayMessage) {
        this.statusMessage = displayMessage;
    }

    /**
     * Get this component's owner UUID
     *
     * @return The Minecraft player's UUID
     */
    public UUID getThumbnailOwner() {
        return thumbnailOwner;
    }

    /**
     * Get the thumbnail URL, may either be <code>attachment://</code>
     * if {@link #getThumbnailAttachment} exists, or an http URL.
     *
     * @return Discord attachment compatible URL
     */
    public String getThumbnailURL() {
        return thumbnailURL;
    }

    /**
     * Get the thumbnail attachment filename if this component attached it as a file.
     *
     * @return The filename if exist
     */
    public Optional<String> getThumbnailAttachment() {
        return Optional.ofNullable(this.attachment);
    }

    /**
     * Set the thumbnail as HTTP URL
     *
     * @param url an HTTP url
     */
    public void setThumbnailURL(String url) {
        this.thumbnailURL = url;
        this.attachment = null;
    }

    /**
     * Set the thumbnail image as attachment file upload.
     *
     * @param filename The filename that is uploaded for this component
     */
    public void setThumbnailAttachment(String filename) {
        this.thumbnailURL = "attachment://" + filename;
        this.attachment = filename;
    }

    /**
     * {@inheritDoc}
     */
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
                DataObject thumbnail = component.getArray("components").getObject(0);
                String description = component.getObject("accessory").getString("description");
                String url = media.getString("url");
                String proxyURL = media.getString("proxy_url");

                // This will be retrieved if this thumbnail is a file attachment
                String attachmentID = media.getString("attachment_id", null);

                // Extract owner data from description (snowflake or UUID)
                final StatusMessage content = () -> thumbnail.getString("content");
                String memberUUID, attachment = null;
                try {
                    String[] parts = description.split(Pattern.quote(":"), 2);
                    if (parts.length > 0) {
                        memberUUID = parts[0];
                        if (parts.length == 2)
                            attachment = parts[1];
                    }
                    else throw new IllegalArgumentException("Malformed identifier description");

                    MemberOwnable ownable = new MemberOwnable(memberUUID);
                    this.registerStatusInfo(content,
                            ownable.getOwner().getUniqueId(),
                            ownable.getOwnerMentionOrName());
                }
                catch (IllegalArgumentException ex) {
                    // Invalid UUID string, possible outdated data version
                    this.thumbnailOwner = null;
                    this.ownerName = description;
                }

                if (attachment != null)
                    // attachment filename is retrieved, it can be reused inside component
                    this.setThumbnailAttachment(attachment);
                else if (attachmentID != null) {
                    // thumbnail is of attachment type, but can't determine its filename
                    FileUtil.matchFilenameByURL(url, Constants.BUILDER_AVATAR_FILE, this::setThumbnailAttachment, failure ->
                        FileUtil.matchFilenameByURL(proxyURL, Constants.BUILDER_AVATAR_FILE, this::setThumbnailAttachment, ignored ->
                            this.setThumbnailURL(failure)
                        )
                    );
                } // else the thumbnail is a normal http URL
                else this.setThumbnailURL(url);

                // Restore ID & register thumbnail
                int displayID = thumbnail.getInt("id");
                this.register(STATUS_THUMBNAIL, id -> this.registerStatusThumbnail(id, displayID));
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
