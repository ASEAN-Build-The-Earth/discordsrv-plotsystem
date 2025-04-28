package github.tintinkung.discordps.core.system.components;

import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;

import java.util.ArrayList;
import java.util.Map;

class Section extends ComponentV2 {
    protected final DataArray components;

    public Section(int componentID, Map<String, Object> accessory) {
        super(
            9,
            componentID,
            "components", new ArrayList<>(),
            "accessory", accessory
        );

        this.components = this.getArray("components");
    }

    public Section addTextDisplay(TextDisplay textDisplay) {
        this.components.add(textDisplay);
        return this;
    }
}
