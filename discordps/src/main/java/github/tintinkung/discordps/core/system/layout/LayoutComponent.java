package github.tintinkung.discordps.core.system.layout;

import github.tintinkung.discordps.core.system.components.ComponentV2;

public interface LayoutComponent<T extends ComponentV2> {

    T build();
}
