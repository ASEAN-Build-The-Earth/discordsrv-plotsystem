package github.tintinkung.discordps.core.system.components;

import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;

import java.util.ArrayList;
import java.util.Map;

public class MediaGallery extends ComponentV2 {
    protected final DataArray items;

    public MediaGallery(int componentID) {
        super(12, componentID, "items", new ArrayList<>());
        this.items = this.getArray("items");
    }

    public MediaGallery addMedia(String url, String description, boolean spoiler) {
        items.add(Map.of(
            "media", Map.of("url", url),
            "description", description,
            "spoiler", spoiler)
        );
        return this;
    }

    public MediaGallery addMedia(String url, String description) {
        return this.addMedia(url, description, false);
    }
}
