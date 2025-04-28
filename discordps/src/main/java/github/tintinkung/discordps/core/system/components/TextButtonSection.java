package github.tintinkung.discordps.core.system.components;

import org.jetbrains.annotations.NotNull;

public class TextButtonSection extends Section {
    public TextButtonSection(int componentID, @NotNull github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button accessory) {
        super(componentID, accessory.toData().toMap());
    }
}
