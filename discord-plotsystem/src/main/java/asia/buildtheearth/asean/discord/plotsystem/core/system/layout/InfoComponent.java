package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.events.*;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.LayoutComponentProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.api.*;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.api.Container;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.HistoryMessage;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.INFO;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.InfoComponent.*;

import java.awt.Color;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class InfoComponent
    extends LayoutComponentProvider<Container, AvailableComponent.InfoComponent>
    implements LayoutComponent<Container> {

    private static final char FETCH_SIGNATURE = '$';

    private StringBuilder histories;
    private Color accentColor;
    private final int packedID;
    private final HashMap<String, String> attachedImage;

    // Metadata
    private record Metadata(String titleFormat,
                            String historyTitle,
                            String historyPrefix,
                            String googleMapLabel) {};

    // Cache metadata statically
    private static final Metadata METADATA = new Metadata(
        DiscordPS.getMessagesLang().get(PlotInformation.INFO_TITLE),
        DiscordPS.getMessagesLang().get(PlotInformation.HISTORIES_TITLE),
        DiscordPS.getMessagesLang().get(PlotInformation.HISTORIES_PREFIX),
        DiscordPS.getMessagesLang().get(PlotInformation.MAP_LABEL)
    );

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
        this.histories.append(METADATA.historyTitle());

        // Prepare component data
        this.register(INFO_TITLE, id -> new TextDisplay(id,
            makeTitle(data.getPlot().plotID(), data.getPlot().cityName(), data.getPlot().countryName())
        ));
        this.register(INFO_LOCATION, id -> {
            Button plotLinkButton = Button.link("https://www.google.com/maps/place/" + data.getGeoCoordinates(), METADATA.googleMapLabel());
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

    public @NotNull String makeTitle(int plotID, @NotNull String city, @NotNull String country) {
        return METADATA.titleFormat()
            .replace("{plotID}", String.valueOf(plotID))
            .replace("{country}", country)
            .replace("{city}", city);
    }

    @Contract(pure = true)
    private static @NotNull String makeLocation(@NotNull String geoCoordinates) {
        return "```" + geoCoordinates + "```";
    }

    private void appendHistories(@Nullable String history) {
        if(history == null || StringUtils.isBlank(history)) return;
        if(!histories.isEmpty()) histories.append('\n');
        if(history.charAt(0) == FETCH_SIGNATURE)
            histories.append("-# ").append(METADATA.historyPrefix()).append('*').append(history.substring(1)).append('*');
        else histories.append(METADATA.historyPrefix()).append(' ').append(history);
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

    public <T extends PlotEvent> @NotNull String formatHistoryMessage(@Nullable T event) {
        return switch (event) {
            case PlotCreateEvent ignored -> formatHistoryMessage(HistoryMessage.ON_CREATED);
            case PlotSubmitEvent ignored -> formatHistoryMessage(HistoryMessage.ON_SUBMITTED);
            case PlotApprovedEvent ignored -> formatHistoryMessage(HistoryMessage.ON_APPROVED);
            case PlotRejectedEvent ignored -> formatHistoryMessage(HistoryMessage.ON_REJECTED);
            case PlotAbandonedEvent ignored -> formatHistoryMessage(HistoryMessage.ON_ABANDONED);
            case PlotArchiveEvent archive -> formatHistoryMessage(HistoryMessage.ON_ARCHIVED, archive.getOwner());
            case PlotReclaimEvent reclaim-> formatHistoryMessage(HistoryMessage.ON_RECLAIMED, reclaim.getOwner());
            case null, default -> formatHistoryMessage(HistoryMessage.ON_SYSTEM_FETCH);
        };
    }

    private @NotNull String formatHistoryMessage(@NotNull HistoryMessage type) {
        return formatHistoryMessage(type, Function.identity());
    }

    private @NotNull String formatHistoryMessage(@NotNull HistoryMessage type,
                                                 @NotNull String owner) {
        return formatHistoryMessage(type, message -> message.replace("{owner}", owner));
    }

    private @NotNull String formatHistoryMessage(@NotNull HistoryMessage type,
                                                 @NotNull Function<String, String> message) {
        String lang = DiscordPS.getMessagesLang().get(type);

        if(type == HistoryMessage.ON_SYSTEM_FETCH)
            return formatHistoryMessage(message.apply(FETCH_SIGNATURE + lang));
        else return formatHistoryMessage(message.apply(lang));
    }

    /**
     * Add history message to this info embed with this format:
     * <p><b>20/04/2021</b> • {message}</p>
     *
     * @param message The history message to be added.
     */
    private @NotNull String formatHistoryMessage(@NotNull String message) {
        return message.replace("{timestamp}", String.valueOf(Instant.now().getEpochSecond()));
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
                    Button button = Button.link(component.getObject("accessory").getString("url"), METADATA.googleMapLabel());
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
