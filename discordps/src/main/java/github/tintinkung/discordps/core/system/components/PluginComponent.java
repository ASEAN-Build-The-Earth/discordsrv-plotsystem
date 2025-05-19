package github.tintinkung.discordps.core.system.components;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.system.components.buttons.IDPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * A class to parse plugin registered components.
 * Will throw {@link IllegalArgumentException} on creation
 * if the given button is not registered by this plugin.
 */
public class PluginComponent<T extends Component> {
    private final java.util.EnumMap<IDPattern, String> dataID;
    private final String rawID;
    protected final T component;

    public PluginComponent(@NotNull T component) throws IllegalArgumentException {
        Checks.notNull(component.getId(), "Component ID");

        this.component = component;
        this.rawID = component.getId();
        this.dataID = IDPattern.parseCustomID(this.rawID);

        // Invalid button ID (possibly from other bots)
        if (dataID == null)
            throw new IllegalArgumentException("Component is not created from this plugin");

        // If for whatever reason the button is not from this plugin
        if (!dataID.get(IDPattern.PLUGIN).equals(DiscordPS.getPlugin().getName()))
            throw new IllegalArgumentException("Component ID is invalid");

        Checks.isSnowflake(dataID.get(IDPattern.ID), "Internal Error: Component ID");
        Checks.isSnowflake(dataID.get(IDPattern.USER), "Internal Error: Component's USER ID");
    }

    public T get() {
        return component;
    }

    public static <T extends Component, V extends PluginComponent<T>>
    Optional<V> getOpt(T component, @NotNull Function<T, V> constructor) {
        try {
            return Optional.of(constructor.apply(component));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static <T extends Component> Optional<PluginComponent<T>> getOpt(T component) {
        try {
            return Optional.of(new PluginComponent<>(component));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public @NotNull String getID() {
        return dataID.get(IDPattern.ID);
    }

    public long getIDLong() {
        return Long.parseUnsignedLong(dataID.get(IDPattern.ID));
    }

    public @Nullable String getPayload() {
        return dataID.get(IDPattern.PAYLOAD);
    }

    public @Nullable Integer getIntPayload() {
        if (getPayload() == null) return null;
        else return Integer.valueOf(getPayload());
    }

    public @NotNull String getType() {
        return dataID.get(IDPattern.TYPE);
    }

    public @NotNull String getUserID() {
        return dataID.get(IDPattern.USER);
    }

    public long getUserIDLong() {
        return Long.parseUnsignedLong(dataID.get(IDPattern.USER));
    }

    public String getRawID() {
        return rawID;
    }
}

