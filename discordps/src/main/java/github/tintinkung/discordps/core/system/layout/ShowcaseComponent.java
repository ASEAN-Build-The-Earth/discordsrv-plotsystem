package github.tintinkung.discordps.core.system.layout;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.system.AvailableComponent;
import github.tintinkung.discordps.core.system.MemberOwnable;
import github.tintinkung.discordps.core.system.PlotData;
import github.tintinkung.discordps.core.system.components.api.Container;
import github.tintinkung.discordps.core.system.components.api.TextDisplay;
import github.tintinkung.discordps.core.system.components.api.TextThumbnailSection;
import github.tintinkung.discordps.core.system.components.api.Thumbnail;
import github.tintinkung.discordps.utils.FileUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static github.tintinkung.discordps.core.system.AvailableComponent.SHOWCASE;
import static github.tintinkung.discordps.core.system.AvailableComponent.ShowcaseComponent.SHOWCASE_INFO;
import static github.tintinkung.discordps.core.system.AvailableComponent.ShowcaseComponent.SHOWCASE_THUMBNAIL;


public final class ShowcaseComponent
        extends LayoutComponentProvider<Container, AvailableComponent.ShowcaseComponent>
        implements LayoutComponent<Container> {

    private final int packedID;
    private String thumbnailURL;
    private UUID thumbnailOwner;
    private String ownerName;
    private String showcaseMessage;
    private String googleMapURL;

    /**
     * Default showcase display message
     */
    public static final String SHOWCASE_MESSAGE_FORMAT = """
    # :earth_asia: %s, %s
    > ## :house: Plot by %s
    > -# Tracked as Plot ID #%d in <#%s>
    """;

    public static @NotNull String makeShowcaseMessage(int plotID,
                                                      @NotNull String threadID,
                                                      @NotNull String city,
                                                      @NotNull String country,
                                                      @NotNull String ownerName) {

        return SHOWCASE_MESSAGE_FORMAT.formatted(country, city, ownerName, plotID, threadID);
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
                data.getPlot().cityName(),
                data.getPlot().countryName(),
                this.ownerName
        );

        this.register(SHOWCASE_THUMBNAIL,
                id -> new TextThumbnailSection(id, new Thumbnail(this.thumbnailURL, this.thumbnailOwner.toString()))
                        .addTextDisplay(new TextDisplay(this.pack(SHOWCASE_INFO), this.showcaseMessage))
        );
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

    public void setThumbnailURL(String url) {
        this.thumbnailURL = url;
    }

    public void setThumbnailAttachment(String filename) {
        this.setThumbnailURL("attachment://" + filename);
    }

    public Container build() {
        return super.build(Container::addComponent).addActionRow(ActionRow.of(
            Button.link(this.googleMapURL, "Google Map")
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
                String description = component.getObject("accessory").getString("description");
                String url = media.getString("url");
                String proxyURL = media.getString("proxy_url");

                // Extract owner data from description (snowflake or UUID)
                try {
                    MemberOwnable owner = new MemberOwnable(description);
                    this.thumbnailOwner = UUID.fromString(description);
                    this.ownerName = (owner.isOwnerHasDiscord() ? owner.getOwnerMentionOrName()
                            + " (" + owner.getOwner().getName() + ")"
                            : owner.formatOwnerName());
                } catch (IllegalArgumentException ex) {
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
                this.showcaseMessage = component.getArray("components").getObject(0).getString("content");

                // Register the builder
                this.register(SHOWCASE_THUMBNAIL, id -> new TextThumbnailSection(id,
                        new Thumbnail(this.thumbnailURL, this.thumbnailOwner == null ? this.ownerName : this.thumbnailOwner.toString()))
                        .addTextDisplay(new TextDisplay(displayID, this.showcaseMessage))
                );
                break;
            }
            case null, default:
                throw new IllegalArgumentException("Unknown sub type parsing ShowcaseComponent");
        }
    }
}
