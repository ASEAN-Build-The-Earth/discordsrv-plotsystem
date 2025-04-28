package github.tintinkung.discordps.core.system.layout;

import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.system.AvailableComponent;
import github.tintinkung.discordps.core.system.components.ComponentV2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Layout {

    private final List<LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>>> layout;

    private Layout() {
        this.layout = new ArrayList<>();
    }

    private Layout from(DataArray rawData) {
        for (int i = 0; i < rawData.length(); i++)
            this.layout.add(Layout.parseData(rawData.getObject(i)));
        return this;
    }

    public static Layout fromRawData(DataArray rawData) {
        return new Layout().from(rawData);
    }

    public List<LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>>> getLayout() {
        return layout;
    }

    public static @Nullable LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>> parseData(@NotNull DataObject rawData) {
        if(!rawData.hasKey("id")) return null;

        int id = rawData.getInt("id");

        int type = AvailableComponent.unpackComponent(id);

        AvailableComponent componentType = AvailableComponent.get(type);

        if(componentType == AvailableComponent.UNKNOWN) return null;

        return switch (componentType) {
            case INFO: yield InfoComponent.from(rawData);
            case STATUS: yield StatusComponent.from(rawData);
            default: yield null;
        };
    }
}
