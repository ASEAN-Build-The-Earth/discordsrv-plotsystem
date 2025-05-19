package github.tintinkung.discordps.core.system.layout;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.api.events.*;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.system.AvailableComponent;
import github.tintinkung.discordps.core.system.PlotData;
import github.tintinkung.discordps.core.system.components.api.*;
import github.tintinkung.discordps.core.system.components.api.Container;
import github.tintinkung.discordps.utils.FileUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static github.tintinkung.discordps.core.system.AvailableComponent.INFO;
import static github.tintinkung.discordps.core.system.AvailableComponent.InfoComponent.*;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

public final class InfoComponent
    extends LayoutComponentProvider<Container, AvailableComponent.InfoComponent>
    implements LayoutComponent<Container> {

    private static final char FETCH_SIGNATURE = '$';

    private StringBuilder histories;
    private Color accentColor;
    private final int packedID;
    private final HashMap<String, String> attachedImage;

    public InfoComponent(int id, int layout, Color color) {
        super(layout, INFO, AvailableComponent.InfoComponent.VALUES);
        this.packedID = id;
        this.accentColor = color;
        this.attachedImage = new HashMap<>();

        // Main container that stores all components
        this.setProvider(() -> new Container(this.packedID, this.accentColor, false));
    }

    public InfoComponent(int layout, @NotNull PlotData data) {
        this(INFO.pack(layout), layout, data.getPrimaryStatus().toTag().getColor());

        // Track history field as StringBuilder
        this.histories = new StringBuilder();
        this.histories.append("## Plot Histories");

        // Prepare component data
        this.register(INFO_TITLE, id -> new TextDisplay(id,
                "# " + makeTitle(data.getPlot().plotID(), data.getPlot().cityName(), data.getPlot().countryName())
        ));
        this.register(INFO_LOCATION, id -> {
            Button plotLinkButton = Button.link("https://www.google.com/maps/place/" + data.getGeoCoordinates(), "Google Map");
            TextButtonSection field = new TextButtonSection(id, plotLinkButton);
            field.addTextDisplay(new TextDisplay(makeLocation(data.getDisplayCords())));
            return field;
        });
        this.register(INFO_SEPARATOR, id -> new Separator(id, true));
        this.register(INFO_HISTORY, id -> new TextDisplay(id, this.histories.toString()));

        // Add gallery section if images file exist
        if(!data.getImageFiles().isEmpty()) data.getImageFiles().forEach((image) -> {
            if(image.exists() && image.isFile()) this.addAttachment(image.getName());
        });

        this.registerImageGallery();
    }

    private InfoComponent(int id, int layout, int color, @NotNull DataArray components) throws ParsingException, IllegalArgumentException {
        this(id, layout, new Color(color));
        super.rebuild(components, this::rebuildComponent);
    }

    public static @NotNull InfoComponent from(@NotNull DataObject data) throws ParsingException, IllegalArgumentException {

        int id = data.getInt("id");
        int color = data.getInt("accent_color");
        int layout = AvailableComponent.unpackPosition(id);
        DataArray components = data.getArray("components");

        return new InfoComponent(id, layout, color, components);
    }

    public static @NotNull String makeTitle(int plotID, @NotNull String city, @NotNull String country) {
        return ":house: Plot #" + plotID + " (" + String.join(", ", country, city) + ")";
    }

    @Contract(pure = true)
    private static @NotNull String makeLocation(@NotNull String geoCoordinates) {
        return "```" + geoCoordinates + "```";
    }

    private void appendHistories(@Nullable String history) {
        if(history == null || StringUtils.isBlank(history)) return;
        if(!histories.isEmpty()) histories.append("\n");
        if(history.charAt(0) == FETCH_SIGNATURE)
            histories.append("-# :small_blue_diamond:").append('*').append(history.substring(1)).append('*');
        else histories.append(":small_blue_diamond: ").append(history);
    }

    /**
     * Set the container accent color
     * @param accentColor The color
     */
    public void setAccentColor(Color accentColor) {
        this.accentColor = accentColor;
    }

    public Color getAccentColor() {
        return this.accentColor;
    }

    public Set<String> getAttachments() {
        return this.attachedImage.keySet();
    }

    public @Nullable String addAttachment(String name) {
        return this.attachedImage.putIfAbsent(name, "attachment://" + name);
    }

    /**
     * Add new history message into the embed's history field.
     * @param message The history message to add to
     */
    public void addHistory(@NotNull String message) {
        appendHistories(message);
    }

    /**
     * Format a message and add to history field by event.
     * @param event The occurred event.
     */
    public <T extends PlotEvent> void addHistory(@Nullable T event) {
        this.appendHistories(formatHistoryMessage(event));
    }

    public static <T extends PlotEvent> @NotNull String formatHistoryMessage(@Nullable T event) {
        return switch (event) {
            case PlotCreateEvent ignored -> formatHistoryMessage("Plot is claimed and under construction.");
            case PlotSubmitEvent ignored -> formatHistoryMessage("Plot submitted and awaiting review.");
            case PlotApprovedEvent ignored -> formatHistoryMessage("Plot has been approved");
            case PlotRejectedEvent ignored -> formatHistoryMessage("Plot has been rejected");
            case PlotAbandonedEvent ignored -> formatHistoryMessage("Plot has been abandoned by the builder.");
            case PlotArchiveEvent archiveEvent -> formatHistoryMessage("Plot has been archived by " + archiveEvent.getOwner());
            case PlotReclaimEvent reclaimEvent -> formatHistoryMessage("Plot is reclaimed by " + reclaimEvent.getOwner());
            case null, default -> formatHistoryMessage((String) null);
        };
    }

    /**
     * Add history message to this info embed with this format:
     * <p><b>20/04/2021</b> • {message}</p>
     *
     * @param message The history message to be added.
     */
    private static @NotNull String formatHistoryMessage(String message) {
        if(message == null) return FETCH_SIGNATURE + "<t:" + Instant.now().getEpochSecond() + ":D> • Plot has been fetched by the system";
        else return "<t:" + Instant.now().getEpochSecond() + ":D> • " + message;
    }

    /**
     * {@inheritDoc}
     */
    public Container build() {
        return super.build(Container::addComponent);
    }

    /**
     * Register image gallery to this component.
     * <p>Note: This process is skipped if the component
     * has no image attachment data, provide it using {@link #addAttachment(String)}</p>
     */
    public void registerImageGallery() {
        if(this.attachedImage.isEmpty()) return;

        this.register(INFO_GALLERY, id -> {
            MediaGallery gallery = new MediaGallery(id);
            this.attachedImage.forEach((key, value) -> gallery.addMedia(value, key));
            return gallery;
        });
    }

    /**
     * {@inheritDoc}
     */
    protected void rebuildComponent(int packedID, @NotNull DataObject component) throws ParsingException, IllegalArgumentException {
        switch(AvailableComponent.InfoComponent.get(AvailableComponent.unpackSubComponent(packedID))) {
            case INFO_TITLE -> {
                this.register(INFO_TITLE, id -> new TextDisplay(id, component.getString("content")));
            }
            case INFO_LOCATION -> {
                this.register(INFO_LOCATION, id -> {
                    Button button = Button.link(component.getObject("accessory").getString("url"), "Google Map");
                    TextButtonSection field = new TextButtonSection(id, button);
                    field.addTextDisplay(new TextDisplay(component.getArray("components").getObject(0).getString("content")));
                    return field;
                });
            }
            case INFO_HISTORY -> {
                this.histories = new StringBuilder(component.getString("content"));
                this.register(INFO_HISTORY, id -> new TextDisplay(id, this.histories.toString()));
            }
            case INFO_SEPARATOR -> {
                this.register(INFO_SEPARATOR, id -> new Separator(id, true));
            }
            case INFO_GALLERY -> {
                DataArray items = component.getArray("items");

                for (int i = 0; i < items.length(); i++) {
                    DataObject image = items.getObject(i);

                    DataObject media = image.getObject("media");
                    String description = image.getString("description");
                    String url = media.getString("url");
                    String proxyURL = media.getString("proxy_url");

                    // Restore attachment files by checking if it matches attachment prefix constant
                    Consumer<String> matchAttachment = name -> this.attachedImage.put(name, "attachment://" + name);

                    // Check for sent plot image attachment
                    FileUtil.matchFilenameByURL(url, Constants.PLOT_IMAGE_FILE, matchAttachment, failure ->
                        // Fallback to proxy URL if failed
                        FileUtil.matchFilenameByURL(proxyURL, Constants.PLOT_IMAGE_FILE, matchAttachment, ignored ->
                            // Accept raw URL if nothing match
                            this.attachedImage.put(FileUtil.getFilenameFromURL(failure), failure)
                        )
                    );
                }

                this.registerImageGallery();
            }
            case null, default -> throw new IllegalArgumentException("Unknown sub type parsing StatusComponent");
        }
    }
}
