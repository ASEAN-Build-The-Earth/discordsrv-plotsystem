package github.tintinkung.discordps.core.system.components;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Thumbnail extends ComponentV2 {

    public Thumbnail(int componentID, @NotNull String url, @NotNull String description) {
        super(
            11,
            componentID,
            "media", Map.of("url", url),
            "description", description
        );
    }

    public Thumbnail(@NotNull String url, @NotNull String description) {
        super(
            11,
            "media", Map.of("url", url),
            "description", description
        );
    }
}
