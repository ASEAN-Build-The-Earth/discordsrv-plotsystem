package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.LayoutComponentProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.components.api.ComponentV2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to retrieve {@linkplain LayoutComponentProvider layout}
 * component instances from raw discord api {@linkplain DataObject}
 *
 * @see #fromRawData(DataArray, AvailableComponent...) Construct a new layout from raw data
 * @see #getLayout() Get the layout data
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
     * Acts as whitelists if not empty.
     */
    private final AvailableComponent[] filter;

    /**
     * Constructs a new layout with empty data.
     */
    private Layout(AvailableComponent... filter) {
        this.layout = new LayoutData();
        this.filter = filter;
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
            this.parseData(rawData.getObject(i)).ifPresent(this.layout::add);
        }
        return this;
    }

    /**
     * Creates a new layout by parsing the given raw data array.
     *
     * @param rawData The raw data as a {@link DataArray} containing component objects
     * @return An optional for a new {@code Layout} instance populated with parsed data
     */
    public static Optional<Layout> fromRawData(@NotNull DataArray rawData,
                                               AvailableComponent... filter) {
        try {
            Layout layout = new Layout(filter).from(rawData);
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
            if(builder.containsKey(layout.getLayoutType()))
                builder.get(layout.getLayoutType()).accept(layout);
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
    public Optional<
        LayoutComponentProvider<? extends ComponentV2, ? extends Enum<?>
    >> parseData(@NotNull DataObject rawData) throws ParsingException, IllegalArgumentException  {
        if(!rawData.hasKey("id")) return Optional.empty();

        int id = rawData.getInt("id");

        int type = AvailableComponent.unpackComponent(id);

        AvailableComponent layoutType = AvailableComponent.get(type);
        Optional<DataObject> optional = Optional.of(rawData);

        return Optional
            .ofNullable(this.rebuild(layoutType))
            .flatMap(layout -> optional.map(layout::apply));
    }

    /**
     * Static mapper on to each available components' rebuild function.
     *
     * @param component Component type to map its static rebuilding function.
     * @return Rebuild function which returns rebuilt instance.
     */
    @Nullable
    @Contract(pure = true)
    public Function<DataObject, LayoutComponentProvider<
        ? extends ComponentV2,
        ? extends Enum<?>
    >> rebuild(@NotNull AvailableComponent component) {
        if(this.filter.length > 0 && Stream.of(this.filter).noneMatch(component::equals))
            return null; // whitelist components inside our filter (if exist)

        return switch (component) {
            case INFO -> InfoComponent::from;
            case STATUS -> StatusComponent::from;
            case SHOWCASE -> ShowcaseComponent::from;
            case NOTIFICATION -> NotificationComponent::from;
            case UNKNOWN -> null;
        };
    }
}
