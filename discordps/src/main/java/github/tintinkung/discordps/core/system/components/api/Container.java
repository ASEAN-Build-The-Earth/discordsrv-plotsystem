package github.tintinkung.discordps.core.system.components.api;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;

import java.awt.Color;
import java.util.ArrayList;

/**
 * <a href="https://discord.com/developers/docs/components/reference#container">Container Component</a>
 */
public class Container extends ComponentV2 {
    protected final DataArray components;

    public Container(int componentID, int accentColor, boolean spoiler) {
        super(
                17,
                componentID,
                "accent_color", accentColor,
                "components", new ArrayList<>(),
                "spoiler", spoiler
        );

        this.components = this.getArray("components");
    }

    /**
     * Create an un-tracked container with no ID and color
     */
    public Container() {
        super(17, "components", new ArrayList<>());
        this.components = this.getArray("components");
    }

    /**
     * Create a container with no accent color
     */
    public Container(int componentID) {
        super(17, componentID, "components", new ArrayList<>());
        this.components = this.getArray("components");
    }

    public Container(int componentID, Color accentColor, boolean spoiler) {
        this(componentID, accentColor.getRGB() & 0xFFFFFF, spoiler);
    }

    public void addComponent(ComponentV2 component) {
        components.add(component);
    }

    public Container addActionRow(ActionRow component) {
        components.add(component.toData());
        return this;
    }

    public void setAccentColor(Color color) {
        this.put("accent_color", color.getRGB() & 0xFFFFFF);
    }
}
