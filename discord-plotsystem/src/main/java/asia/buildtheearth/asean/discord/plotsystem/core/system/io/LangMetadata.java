package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LangMetadata {

    private final Map<Class<? extends MetadataInstance>, MetadataInstance> registered;

    public LangMetadata() {
        this.registered = new HashMap<>();
    }

    /**
     * Register a new guild-based slash command
     *
     * @param metadata list {@link Record} to register
     */
    public final void register(MetadataInstance @NotNull... metadata) {
        for(MetadataInstance data : metadata) {
            registered.put(data.getClass(), data);
        }
    }

    /**
     * Clear all registered slash command data.
     * Required an API update for changes to take effects.
     */
    public void reload() {
        Set<Map.Entry<Class<? extends MetadataInstance>, MetadataInstance>> instances = registered.entrySet();

        instances.forEach(metadata -> {
            registered.put(metadata.getKey(), metadata.getValue().load());
        });
    }

    public final <T extends MetadataInstance> T getAs(Class<T> metadata) {
        MetadataInstance data = registered.get(metadata);
        if (data == null)
            throw new IllegalStateException("[Internal Exception] "
            + "No metadata instance available for class: "
            + metadata.getName());
        return metadata.cast(data);
    }
}
