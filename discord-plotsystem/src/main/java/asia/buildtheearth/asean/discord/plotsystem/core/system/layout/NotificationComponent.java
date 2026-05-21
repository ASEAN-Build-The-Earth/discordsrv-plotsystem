package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import asia.buildtheearth.asean.discord.components.api.ComponentV2;
import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.components.api.TextDisplay;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.LayoutComponentProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableComponent.NOTIFICATION;

public class NotificationComponent
    extends LayoutComponentProvider<Container, AvailableComponent.NotificationComponent>
    implements LayoutComponent<Container> {

    private final int packedID;
    private final String notification;
    private AvailableComponent.NotificationComponent type = null;

    @Nullable
    public AvailableComponent.NotificationComponent getNotificationType() {
        return this.type;
    }

    @Override
    protected void register(AvailableComponent.NotificationComponent component,
                            Builder<ComponentV2> builder) {
        this.type = component;
        super.register(component, builder);
    }

    /**
     * Construct a new layout component with a given layout position.
     *
     * @param layout The layout position of this component.
     * @param id The layout ID used internally as identity.
     * @param message The displaying message of this notification
     */
    protected NotificationComponent(int layout, int id, @NotNull String message) {
        super(layout, NOTIFICATION, AvailableComponent.NotificationComponent.VALUES);
        this.packedID = id;
        this.notification = message;
        this.setProvider(() -> new Container(this.packedID));
    }

    public NotificationComponent(int layout,
                                 @NotNull AvailableComponent.NotificationComponent type,
                                 @NotNull String message) {
        this(layout, NOTIFICATION.pack(layout), message);
        this.register(type, id -> new TextDisplay(id, this.notification));
    }

    /**
     * Restore a notification component from raw data
     *
     * @param id         The component ID to restore
     * @param layout     The layout position to restore
     * @param component    The raw data to rebuild
     * @throws ParsingException         If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    private NotificationComponent(int layout,
                                  int id,
                                  @NotNull DataObject component) throws ParsingException, IllegalArgumentException {
        this(layout, id, component.getString("content"));
        this.rebuildComponent(id, component);
    }

    /**
     * Rebuild a new status component from raw data
     *
     * @param rawData The raw {@link DataObject} to be rebuilt
     * @return A new notification component instance with all the data restored
     * @throws ParsingException         If an unexpected data keys is received
     * @throws IllegalArgumentException If the given raw data is invalid
     */
    @Contract("_ -> new")
    public static @NotNull NotificationComponent from(@NotNull DataObject rawData) throws ParsingException, IllegalArgumentException {
        int id = rawData.getInt("id");
        int layout = AvailableComponent.unpackPosition(id);
        DataArray components = rawData.getArray("components");

        return new NotificationComponent(layout, id, components.getObject(0));
    }

        @Override
    public Container build() {
        return super.build(Container::addComponent);
    }

    @Override
    protected void rebuildComponent(int packedID, @NotNull DataObject component) throws ParsingException, IllegalArgumentException {
        int typeID = component.getInt("id");
        int ordinal = AvailableComponent.unpackSubComponent(typeID);
        AvailableComponent.NotificationComponent type = AvailableComponent.NotificationComponent.get(ordinal);

        if(type == null)
            throw new IllegalArgumentException("Unknown type parsing NotificationComponent (ID: " + packedID + ")");

        this.register(type, id -> new TextDisplay(id, this.notification));
    }
}
