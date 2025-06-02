package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import asia.buildtheearth.asean.discord.components.api.ComponentV2;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.LayoutComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.BiConsumer;

/**
 * A provider class for layout (top-level) components,
 * registered through the {@link AvailableComponent} enum.
 *
 * <p>Each layout component must have a corresponding subcomponent enum,
 * which defines the position and organization of subcomponents within the layout.</p>
 *
 * @param <T> The component type managed by this provider (must extend {@link ComponentV2}).
 * @param <V> The subcomponent type of this layout, as an {@link AvailableComponent.SubComponent} enum.
 *            The enum defines the positions of each subcomponent and organizes IDs based on their ordinal values.
 */
public abstract class LayoutComponentProvider<T extends ComponentV2, V extends Enum<V> & AvailableComponent.SubComponent>
    implements LayoutComponent<T> {

    /**
     * Component builder interface.
     * Called on each registered subcomponent when building this layout component.
     *
     * @param <T> The type of component to build into.
     */
    @FunctionalInterface
    protected interface Builder<T extends ComponentV2> {
        T build(int packedID);
    }

    private @Nullable LayoutComponent<T> provider;
    private final EnumMap<V, Builder<ComponentV2>> builders;
    private final V[] values;
    private final int layout;
    private final AvailableComponent type;

    /**
     * Construct a new layout component with a given layout position.
     *
     * @param layout The layout position of this component.
     * @param type The component enum identity.
     * @param values The subcomponent values as enum list.
     */
    protected LayoutComponentProvider(int layout, @NotNull AvailableComponent type, V[] values) {
        this.builders = new EnumMap<>(values[0].getDeclaringClass());
        this.values = values;
        this.layout = layout;
        this.type = type;
    }

    /**
     * Get the enum type of this layout component.
     *
     * @return The enum
     */
    public AvailableComponent getType() {
        return type;
    }

    /**
     * Get the layout position of this component.
     *
     * @return Position in integer starting from 0
     */
    public int getLayoutPosition() {
        return layout;
    }

    /**
     * Set this layout's provider for building the actual data.
     *
     * @param provider The provider that will be used to {@link #build()} component data.
     */
    protected void setProvider(@Nullable LayoutComponent<T> provider) {
        this.provider = provider;
    }

    /**
     * Pack a subcomponent id to this component layout.
     * @param type The subcomponent to pack as an enum.
     * @return The packed {@link AvailableComponent} ID.
     */
    protected int pack(V type) {
        return this.type.pack(type, this.layout);
    }

    /**
     * Registers a builder function for the given InfoComponent.
     *
     * @param component The InfoComponent enum constant.
     * @param builder The function that builds the corresponding ComponentV2.
     */
    protected void register(V component, Builder<ComponentV2> builder) {
        builders.putIfAbsent(component, builder);
    }

    /**
     * Builds all registered components in enum declaration order
     * into this layout component and return the built object.
     *
     * @param eachComponent Action to do for building each component.
     */
    protected T build(BiConsumer<T, ComponentV2> eachComponent) {
        if(provider == null) throw new IllegalArgumentException("No provider to build for component.");
        if(builders.isEmpty()) throw new IllegalArgumentException("No registered component in this provider.");

        T provided = this.provider.build();

        for (V component : this.values) {
            Builder<ComponentV2> builder = builders.get(component);
            if (builder != null) {
                eachComponent.accept(provided, builder.build(this.pack(component)));
            }
        }

        return provided;
    }

    /**
     * Rebuild each subcomponent of this layout component.
     *
     * @param components The subcomponent array to rebuild.
     * @param eachComponent Action to do when rebuilding each subcomponent.
     */
    protected void rebuild(@NotNull DataArray components, BiConsumer<Integer, DataObject> eachComponent) throws ParsingException, IllegalArgumentException {
        for (int i = 0; i < components.length(); i++) {
            DataObject component = components.getObject(i);

            int packedID = component.getInt("id");

            eachComponent.accept(packedID, component);
        }
    }

    /**
     * Build this layout component to component data
     * @return The component data
     */
    public abstract T build();

    /**
     * Provide rebuild functionality for this component
     * when receiving as raw data from the API.
     *
     * @param packedID The received ID
     * @param component The received raw data object
     * @throws ParsingException If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    protected abstract void rebuildComponent(int packedID, DataObject component) throws ParsingException, IllegalArgumentException;
}

