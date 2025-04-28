package github.tintinkung.discordps.core.system.layout;

import github.scarsz.discordsrv.dependencies.commons.io.FilenameUtils;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.system.AvailableComponent;
import github.tintinkung.discordps.core.system.PlotData;
import github.tintinkung.discordps.core.system.components.*;
import github.tintinkung.discordps.core.system.components.Container;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

import static github.tintinkung.discordps.core.system.AvailableComponent.STATUS;
import static github.tintinkung.discordps.core.system.AvailableComponent.StatusComponent.*;

public final class StatusComponent
    extends LayoutComponentProvider<Container, AvailableComponent.StatusComponent>
    implements LayoutComponent<Container> {

    private final int packedID;
    private Color accentColor;

    public StatusComponent(int id, int layout, Color color) {
        super(layout, STATUS, AvailableComponent.StatusComponent.VALUES);
        this.packedID = id;
        this.accentColor = color;
        this.setProvider(() -> new Container(this.packedID, this.accentColor, false));
    }

    public StatusComponent(int layout, @NotNull PlotData data) {
        this(STATUS.pack(layout), layout, data.getStatus().toTag().getColor());

        // Apply base on avatar resource
        data.getAvatarFile().ifPresentOrElse(
            (file) -> this.register(STATUS_THUMBNAIL, id -> this.createComponent(id,
                "attachment://" + file.getName(),
                data.formatOwnerName(),
                data.getOwnerMentionOrName()
            )),
            () -> this.register(STATUS_THUMBNAIL, id -> this.createComponent(id,
                data.getAvatarURL().toString(),
                data.formatOwnerName(),
                data.getOwnerMentionOrName()
            ))
        );
    }

    public TextThumbnailSection createComponent(int id, String thumbnailURL, String thumbnailName, String ownerName) {
        return (TextThumbnailSection) new TextThumbnailSection(id, new Thumbnail(thumbnailURL, thumbnailName))
            .addTextDisplay(new TextDisplay(this.pack(STATUS_INFO),
                "## Claimed by " + ownerName
                + "\nUse this thread to track this claim's progressions,"
                + "\nOur friend Bob The Land Lord will help notify about this below.")
        );
    }

    public StatusComponent(int id, int layout, int color, @NotNull DataArray components) {
        this(id, layout, new Color(color));
        super.rebuild(components, this::rebuildComponent);
    }

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

    public Container build() {
        return super.build(Container::addComponent);
    }

    protected void rebuildComponent(int packedID, @NotNull DataObject component) {
        switch(AvailableComponent.StatusComponent.get(AvailableComponent.unpackSubComponent(packedID))) {
            case STATUS_INFO:
            case STATUS_THUMBNAIL: {
                this.register(STATUS_THUMBNAIL, id -> {
                    DataObject media = component.getObject("accessory").getObject("media");
                    String description = component.getObject("accessory").getString("description");
                    String url = media.getString("url");
                    String proxyURL = media.getString("proxy_url");
                    Thumbnail thumbnail;
                    // Retrieve stored avatar image in the player's media path
                    if (FilenameUtils.getBaseName(url).startsWith("avatar-image")) {
                        int queryStart = url.indexOf(63);
                        String name = FilenameUtils.getName(url.substring(0, queryStart));
                        thumbnail = new Thumbnail("attachment://" + name, description);
                    }
                    else if (FilenameUtils.getBaseName(proxyURL).startsWith("avatar-image")) {
                        int queryStart = proxyURL.indexOf(63);
                        String name = FilenameUtils.getName(proxyURL.substring(0, queryStart));
                        thumbnail = new Thumbnail("attachment://" + name, description);
                    }
                    else thumbnail = new Thumbnail(url, description);

                    TextThumbnailSection field = new TextThumbnailSection(packedID, thumbnail);
                    field.addTextDisplay(new TextDisplay(component.getArray("components").getObject(0).getString("content")));

                    return new TextThumbnailSection(id, thumbnail)
                            .addTextDisplay(new TextDisplay(
                                    component.getArray("components").getObject(0).getString("content")
                            ));
                });
                break;
            }
            case null, default: throw new IllegalArgumentException("Unknown sub type parsing StatusComponent");
        }
    }
}
