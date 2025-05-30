package asia.buildtheearth.asean.discord.plotsystem.core.system.components.api;

import org.jetbrains.annotations.NotNull;

public class TextThumbnailSection extends Section {
    public TextThumbnailSection(int componentID, @NotNull Thumbnail accessory) {
        super(componentID, accessory.toMap());
    }

    public TextThumbnailSection(@NotNull Thumbnail accessory) {
        super(accessory.toMap());
    }
}
