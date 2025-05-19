package github.tintinkung.discordps.core.system.components.buttons;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.tintinkung.discordps.core.system.components.PluginComponent;

public class PluginButton extends PluginComponent<Button> {

    public PluginButton(Button component) throws IllegalArgumentException {
        super(component);
    }
}
