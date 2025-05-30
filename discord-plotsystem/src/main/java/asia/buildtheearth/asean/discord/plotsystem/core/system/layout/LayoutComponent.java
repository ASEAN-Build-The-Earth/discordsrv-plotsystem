package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import asia.buildtheearth.asean.discord.plotsystem.core.system.components.api.ComponentV2;

public interface LayoutComponent<T extends ComponentV2> {

    T build();
}
