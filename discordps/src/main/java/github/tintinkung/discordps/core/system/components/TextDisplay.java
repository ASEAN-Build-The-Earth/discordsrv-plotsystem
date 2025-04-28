package github.tintinkung.discordps.core.system.components;

public class TextDisplay extends ComponentV2 {

    public TextDisplay(int componentID, @org.jetbrains.annotations.NotNull String content) {
        super(
            10,
            componentID,
            "content", content
        );
    }

    public TextDisplay(@org.jetbrains.annotations.NotNull String content) {
        super(10, "content", content);
    }
}
