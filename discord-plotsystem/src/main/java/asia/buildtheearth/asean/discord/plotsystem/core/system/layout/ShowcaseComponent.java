package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.BuildTeamLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.LayoutComponentProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.MemberOwnable;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.components.api.TextDisplay;
import asia.buildtheearth.asean.discord.components.api.TextThumbnailSection;
import asia.buildtheearth.asean.discord.components.api.Thumbnail;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.SHOWCASE;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.ShowcaseComponent.SHOWCASE_INFO;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.ShowcaseComponent.SHOWCASE_THUMBNAIL;


public final class ShowcaseComponent
        extends LayoutComponentProvider<Container, AvailableComponent.ShowcaseComponent>
        implements LayoutComponent<Container> {

    private final int packedID;
    private String thumbnailURL;
    private UUID thumbnailOwner;
    private String ownerName;
    private String showcaseMessage;
    private String googleMapURL;
    private @Nullable String attachment = null;

    public static @NotNull String makeShowcaseMessage(int plotID,
                                                      @NotNull String threadID,
                                                      @NotNull String cityProjectID,
                                                      @NotNull String countryCode,
                                                      @NotNull String ownerName) {

        String country = DiscordPS.getMessagesLang().get(BuildTeamLang.getCountry().getName(countryCode), countryCode);
        String city = DiscordPS.getMessagesLang().get(BuildTeamLang.getCityProject().getName(cityProjectID), cityProjectID);

        return DiscordPS.getMessagesLang().get(PlotInformation.SHOWCASE_TITLE)
            .replace(Format.OWNER, ownerName)
            .replace(Format.PLOT_ID, String.valueOf(plotID))
            .replace(Format.COUNTRY, country)
            .replace(Format.CITY, city)
            .replace(Format.THREAD_ID, threadID);
    }

    /**
     * Create an empty status component with minimum data.
     *
     * @param id     The packed ID of this component
     * @param layout The layout position of this component
     */
    private ShowcaseComponent(int id, int layout) {
        super(layout, SHOWCASE, AvailableComponent.ShowcaseComponent.VALUES);
        this.packedID = id;
        this.setProvider(() -> new Container(this.packedID));
    }

    /**
     * Create a complete status component by {@link PlotData}
     *
     * @param layout The layout position of this component
     * @param data   The plot data to apply to this status
     */
    public ShowcaseComponent(int layout, @NotNull String threadID, @NotNull MemberOwnable owner, @NotNull PlotData data) {
        this(SHOWCASE.pack(layout), layout);

        this.thumbnailOwner = owner.getOwner().getUniqueId();
        this.thumbnailURL = owner.getAvatarAttachmentOrURL();
        this.ownerName = (owner.isOwnerHasDiscord() ? owner.getOwnerMentionOrName()
                + " (" + owner.getOwner().getName() + ")"
                : owner.formatOwnerName());

        this.googleMapURL = "https://www.google.com/maps/place/" + data.getGeoCoordinates();
        this.showcaseMessage = makeShowcaseMessage(
                data.getPlot().plotID(),
                threadID,
                data.getPlot().cityProjectID(),
                data.getPlot().countryCode(),
                this.ownerName
        );

        owner.getAvatarFile()
            .map(File::getName)
            .ifPresentOrElse(this::setThumbnailAttachment,
                () -> this.setThumbnailURL(owner.getAvatarURL().toString())
            );
        this.register(SHOWCASE_THUMBNAIL, this::registerShowcaseThumbnail);
    }

    /**
     * Restore a status component from raw data
     *
     * @param id         The component ID to restore
     * @param layout     The layout position to restore
     * @param components The raw components data to rebuild
     * @throws ParsingException         If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    private ShowcaseComponent(int id, int layout, @NotNull DataArray components) throws ParsingException, IllegalArgumentException {
        this(id, layout);

        // Action row won't work with layout ID
        int lastIndex = components.length() - 1;
        DataObject actionRow = components.getObject(lastIndex);
        this.googleMapURL = actionRow.getArray("components").getObject(0).getString("url");

        super.rebuild(components.remove(lastIndex), this::rebuildComponent);
    }

    private TextThumbnailSection registerShowcaseThumbnail(int packedID) {
        return registerShowcaseThumbnail(packedID, this.pack(SHOWCASE_INFO));
    }

    private TextThumbnailSection registerShowcaseThumbnail(int packedID, int displayID) {
        // Save ownerData in thumbnail description field,
        // so we can parse for avatar owner when rebuilding this component
        String identifier = this.thumbnailOwner == null? this.ownerName : this.thumbnailOwner.toString();
        String description = this.attachment != null? identifier + ':' + this.attachment : identifier;
        Thumbnail thumbnail = new Thumbnail(this.thumbnailURL, description);
        TextDisplay text = new TextDisplay(displayID, this.showcaseMessage);
        return new TextThumbnailSection(packedID, thumbnail).addTextDisplay(text);
    }

    /**
     * Rebuild a new status component from raw data
     *
     * @param rawData The raw {@link DataObject} to be rebuilt
     * @return A new status component instance with all the data restored
     * @throws ParsingException         If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    @Contract("_ -> new")
    public static @NotNull ShowcaseComponent from(@NotNull DataObject rawData) throws ParsingException, IllegalArgumentException {
        int id = rawData.getInt("id");
        int layout = AvailableComponent.unpackPosition(id);
        DataArray components = rawData.getArray("components");

        return new ShowcaseComponent(id, layout, components);
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
        this.attachment = filename;
        this.thumbnailURL = "attachment://" + filename;
    }

    /**
     * {@inheritDoc}
     */
    public Container build() {
        return super.build(Container::addComponent).addActionRow(ActionRow.of(
            Button.link(this.googleMapURL, DiscordPS.getMessagesLang().get(PlotInformation.MAP_LABEL))
        ));
    }

    /**
     * {@inheritDoc}
     */
    protected void rebuildComponent(int packedID, @NotNull DataObject component) throws ParsingException, IllegalArgumentException {
        switch (AvailableComponent.StatusComponent.get(AvailableComponent.unpackSubComponent(packedID))) {
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
                String memberUUID, attachment = null;
                try {
                    String[] parts = description.split(Pattern.quote(":"), 2);
                    if (parts.length > 0) {
                        memberUUID = parts[0];
                        if (parts.length == 2)
                            attachment = parts[1];
                    }
                    else throw new IllegalArgumentException("Malformed identifier description");

                    MemberOwnable owner = new MemberOwnable(memberUUID);
                    this.thumbnailOwner = UUID.fromString(description);
                    this.ownerName = (owner.isOwnerHasDiscord() ? owner.getOwnerMentionOrName()
                            + " (" + owner.getOwner().getName() + ")"
                            : owner.formatOwnerName());
                } catch (IllegalArgumentException ex) {
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

                // Restore display message
                this.showcaseMessage = thumbnail.getString("content");

                // Register the builder
                int displayID = thumbnail.getInt("id");
                this.register(SHOWCASE_THUMBNAIL, id -> this.registerShowcaseThumbnail(id, displayID));
                break;
            }
            case null, default:
                throw new IllegalArgumentException("Unknown sub type parsing ShowcaseComponent");
        }
    }
}
