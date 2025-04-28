package github.tintinkung.discordps.core.system.layout;

import github.scarsz.discordsrv.dependencies.commons.io.FilenameUtils;
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
import github.tintinkung.discordps.core.system.components.*;
import github.tintinkung.discordps.core.system.components.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static github.tintinkung.discordps.core.system.AvailableComponent.INFO;
import static github.tintinkung.discordps.core.system.AvailableComponent.InfoComponent.*;

import java.awt.*;
import java.time.Instant;

public final class InfoComponent
    extends LayoutComponentProvider<Container, AvailableComponent.InfoComponent>
    implements LayoutComponent<Container> {

    private StringBuilder histories;
    private final int packedID;
    private Color accentColor;

    public InfoComponent(int id, int layout, Color color) {
        super(layout, INFO, AvailableComponent.InfoComponent.VALUES);
        this.packedID = id;
        this.accentColor = color;

        // Main container that stores all components
        this.setProvider(() -> new Container(this.packedID, this.accentColor, false));
    }

    public InfoComponent(int layout, @NotNull PlotData data) {
        this(INFO.pack(layout), layout, data.getStatus().toTag().getColor());

        // Track history field as StringBuilder
        this.histories = new StringBuilder();
        this.histories.append("## Plot Histories");

        // Prepare component data
        this.register(INFO_TITLE, id -> new TextDisplay(id,
                makeTitle(data.getPlot().plotID(), data.getPlot().cityName(), data.getPlot().countryName())
        ));
        this.register(INFO_LOCATION, id -> {
            Button plotLinkButton = Button.link(" https://www.google.com/maps/place/" + data.getGeoCoordinates(), "Google Map");
            TextButtonSection field = new TextButtonSection(id, plotLinkButton);
            field.addTextDisplay(new TextDisplay(makeLocation(data.getDisplayCords())));
            return field;
        });
        this.register(INFO_SEPARATOR, id -> new Separator(id, true));
        this.register(INFO_HISTORY, id -> new TextDisplay(id, this.histories.toString()));

        // Add gallery section if images file exist
        if(!data.getImageFiles().isEmpty()) this.register(INFO_GALLERY, id -> {
            MediaGallery gallery = new MediaGallery(id);

            data.getImageFiles().forEach((image) -> {
                if(image.exists() && image.isFile())
                    gallery.addMedia("attachment://" + image.getName(), image.getName());
            });

            return gallery;
        });
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

    private static String makeTitle(int plotID, @NotNull String city, @NotNull String country) {
        return "# :house: Plot #" + plotID + " (" + String.join(", ", country, city) + ")";
    }

    private static String makeLocation(@NotNull String geoCoordinates) {
        return "```" + geoCoordinates + "```";
    }

    private void appendHistories(@Nullable String history) {
        if(history == null || StringUtils.isBlank(history)) return;
        if(!histories.isEmpty()) histories.append("\n");
        histories.append(":small_blue_diamond: ").append(history);
    }

    /**
     * Set the container accent color
     * @param accentColor The color
     */
    public void setAccentColor(Color accentColor) {
        this.accentColor = accentColor;
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
    public <T extends PlotEvent> void addHistory(@NotNull T event) {
        switch (event) {
            case PlotCreateEvent ignored -> addHistoryFormatted("Plot is claimed and under construction.");
            case PlotSubmitEvent ignored -> addHistoryFormatted("Plot submitted and awaiting review.");
            case PlotApprovedEvent ignored -> addHistoryFormatted("Plot has been approved");
            case PlotRejectedEvent ignored -> addHistoryFormatted("Plot has been rejected");
            case PlotAbandonedEvent ignored -> addHistoryFormatted("Plot has been abandoned by the builder.");
            default -> appendHistories(null);
        }
    }

    /**
     * Add history message to this info embed with this format:
     * <p><b>20/04/2021</b> • {message}</p>
     * @param message The history message to be added.
     */
    private void addHistoryFormatted(String message) {
        appendHistories("<t:" + Instant.now().getEpochSecond() + ":d> • " + message);
    }

    public Container build() {
        return super.build(Container::addComponent);
    }

    protected void rebuildComponent(int packedID, @NotNull DataObject component) {
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
                this.register(INFO_GALLERY, id -> {
                    MediaGallery gallery = new MediaGallery(id);
                    DataArray items = component.getArray("items");

                    for (int i = 0; i < items.length(); i++) {
                        DataObject image = items.getObject(i);

                        DataObject media = image.getObject("media");
                        String description = image.getString("description");
                        String url = media.getString("url");
                        String proxyURL = media.getString("proxy_url");

                        // Retrieve stored image in the media path
                        if (FilenameUtils.getBaseName(url).startsWith(Constants.PLOT_IMAGE_FILE)) {
                            int queryStart = url.indexOf(63);
                            String name = FilenameUtils.getName(url.substring(0, queryStart));
                            gallery.addMedia("attachment://" + name, description);
                        }
                        else if (FilenameUtils.getBaseName(proxyURL).startsWith(Constants.PLOT_IMAGE_FILE)) {
                            int queryStart = proxyURL.indexOf(63);
                            String name = FilenameUtils.getName(proxyURL.substring(0, queryStart));
                            gallery.addMedia("attachment://" + name, description);
                        }
                        else gallery.addMedia(url, description);
                    }

                    return gallery;
                });
            }
            case null, default -> throw new IllegalArgumentException("Unknown sub type parsing StatusComponent");
        }
    }
}
