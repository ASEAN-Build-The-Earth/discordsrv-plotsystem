package github.tintinkung.discordps.core.system.components;

public class TextThumbnailSection extends Section {
    public TextThumbnailSection(int componentID, Thumbnail accessory) {
        super(componentID, accessory.toMap());
    }
}
