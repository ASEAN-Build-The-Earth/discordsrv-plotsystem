package github.tintinkung.discordps.core.system.layout;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.providers.LayoutComponentProvider;
import github.tintinkung.discordps.core.system.AvailableComponent;
import github.tintinkung.discordps.core.system.components.api.ComponentV2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Plot-System thread layout constructor class.
 *
 * <p>When constructed, create an array as {@link LayoutData}
 * that currently include {@link InfoComponent} and {@link StatusComponent}.</p>
 *
 * @see #getLayout() Get the layout data
 * @see #from(DataArray) Construct a new layout from raw data
 */
public class Layout {

    /**
     * A functional interface representing a consumer to modifies layout-specific data
     * within a {@link LayoutComponentProvider} before it is built.
     *
     * <p>Invoked for each layout component before it is transformed into a {@link ComponentV2} instance.</p>
     */
    @FunctionalInterface
    public interface LayoutBuilder extends Consumer<LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>>> {}

    /**
     * A typed list that holds layout component providers used to construct
     * a collection of {@link ComponentV2} instances.
     */
    public static class LayoutData extends ArrayList<LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>>> {}

    /**
     * The layout data associated with this instance.
     */
    private final LayoutData layout;

    /**
     * Constructs a new layout with empty data.
     */
    private Layout() {
        this.layout = new LayoutData();
    }

    /**
     * Populates this layout by parsing raw component data from an API response.
     *
     * @param rawData the raw data as a {@link DataArray} containing component objects
     * @return this layout instance for method chaining
     */
    @Contract("_ -> this")
    private Layout from(@NotNull DataArray rawData) throws ParsingException, IllegalArgumentException {
        for (int i = 0; i < rawData.length(); i++) {
            LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>> data = Layout.parseData(rawData.getObject(i));
            if(data != null) this.layout.add(Layout.parseData(rawData.getObject(i)));
        }
        return this;
    }

    /**
     * Creates a new layout by parsing the given raw data array.
     *
     * @param rawData The raw data as a {@link DataArray} containing component objects
     * @return An optional for a new {@code Layout} instance populated with parsed data
     */
    public static Optional<Layout> fromRawData(@NotNull DataArray rawData) {
        try {
            Layout layout = new Layout().from(rawData);
            return Optional.ofNullable(layout.getLayout().isEmpty()? null : layout);
        }
        catch (ParsingException | IllegalArgumentException ex) {
            DiscordPS.error("[Internal] Exception occurred trying to parse layout from raw data: " + ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the layout data associated with this instance.
     *
     * @return the {@link LayoutData}
     */
    public LayoutData getLayout() {
        return layout;
    }

    /**
     * Build all layout into raw component data
     *
     * @param builder Action to do before building each layout
     * @return Built layout as a list of {@link ComponentV2}
     */
    public Collection<ComponentV2> buildLayout(EnumMap<AvailableComponent, LayoutBuilder> builder) {
        return this.layout.stream().map(layout -> {
            if(builder.containsKey(layout.getType()))
                builder.get(layout.getType()).accept(layout);
            return layout.build();
        }).collect(Collectors.toList());
    }

    /**
     * Parse and rebuilt a previously sent layout component data to its original object
     *
     * @param rawData The raw data as a {@link DataObject}
     * @return The layout component that is rebuilt from the raw data, null if the component is unknown
     * @throws ParsingException If the parser failed to parse raw data keys
     * @throws IllegalArgumentException If an internal/unknown error occurred during the parsing process
     */
    public static @Nullable LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>> parseData(@NotNull DataObject rawData) throws ParsingException, IllegalArgumentException  {
        if(!rawData.hasKey("id")) return null;

        int id = rawData.getInt("id");

        int type = AvailableComponent.unpackComponent(id);

        AvailableComponent componentType = AvailableComponent.get(type);

        if(componentType == AvailableComponent.UNKNOWN) return null;

        return switch (componentType) {
            case INFO: yield InfoComponent.from(rawData);
            case STATUS: yield StatusComponent.from(rawData);
            case SHOWCASE: yield ShowcaseComponent.from(rawData);
            default: yield null;
        };
    }
}
