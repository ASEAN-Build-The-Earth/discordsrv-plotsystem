package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.DiscordPS;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ComponentUtil {

    public enum IDPattern {
        PLUGIN("plugin"),
        TYPE("type"),
        ID("id"),
        USER("user"),
        PAYLOAD("payload");

        private final String group;

        IDPattern(@NotNull String group) { this.group = group; }
    }

    /**
     * Plugin interaction button {@code custom_id} convention.
     * <p>
     * Matches component IDs in the format: {@code <plugin>/<type>/<id>/<user>} or {@code <plugin>/<type>/<id>/<user>/<payload>}.
     * <p>
     * Regex pattern used: {@code "^(?<plugin>\\w+)/(?<type>\\w+)/(?<id>\\w+)/(?<user>\\w+)(?:/(?<payload>.*?))?/?$"}
     */
    public static final Pattern COMPONENT_PATTERN = Pattern.compile(
        String.format("^" +
            "(?<%s>\\w+)/" +
            "(?<%s>\\w+)/" +
            "(?<%s>\\w+)/" +
            "(?<%s>\\w+)" +
            "(?:/(?<%s>.*?))?/?$",
            IDPattern.PLUGIN.group,
            IDPattern.TYPE.group,
            IDPattern.ID.group,
            IDPattern.USER.group,
            IDPattern.PAYLOAD.group
        )
    );

    /**
     * A class to parse plugin registered button.
     * Will throw {@link IllegalArgumentException} on creation
     * if the given button is not registered by this plugin.
     */
    public static class PluginButton {
        private final EnumMap<ComponentUtil.IDPattern, String> component;

        public PluginButton(@NotNull Button button) {
            Checks.notNull(button.getId(), "Button ID");

            this.component = ComponentUtil.parseCustomID(button.getId());

            // Invalid button ID (possibly from other bots)
            if(component == null)
                throw new IllegalArgumentException("Button is not created from this plugin");

            // If for whatever reason the button is not from this plugin
            if(!component.get(ComponentUtil.IDPattern.PLUGIN).equals(DiscordPS.getPlugin().getName()))
                throw new IllegalArgumentException("Button ID is invalid");

            Checks.isSnowflake(component.get(ComponentUtil.IDPattern.ID), "Internal Error: Button ID");
            Checks.isSnowflake(component.get(ComponentUtil.IDPattern.USER), "Internal Error: Button's USER ID");
        }

        public @NotNull String getID() {
            return component.get(ComponentUtil.IDPattern.ID);
        }

        public long getIDLong() {
            return Long.parseUnsignedLong(component.get(ComponentUtil.IDPattern.ID));
        }

        public @Nullable String getPayload() {
            return component.get(ComponentUtil.IDPattern.PAYLOAD);
        }

        public @Nullable Integer getIntPayload() {
            if(getPayload() == null) return null;
            else return Integer.valueOf(getPayload());
        }

        public @NotNull String getType() {
            return component.get(ComponentUtil.IDPattern.TYPE);
        }

        public @NotNull String getUserID() {
            return component.get(ComponentUtil.IDPattern.USER);
        }

        public long getUserIDLong() {
            return Long.parseUnsignedLong(component.get(ComponentUtil.IDPattern.USER));
        }
    }

    /**
     * Parses a {@code custom_id} string to extract the components based on the {@link IDPattern} convention.
     * The returned {@link EnumMap} maps {@link IDPattern} values to their corresponding extracted string values.
     *
     * @param customId the {@code custom_id} string to be parsed
     * @return an {@link EnumMap} with the extracted components, or {@code null} if the {@code custom_id} doesn't match the pattern
     */
    @Nullable
    public static EnumMap<IDPattern, String> parseCustomID(String customId) {
        Matcher matcher = COMPONENT_PATTERN.matcher(customId);
        if (!matcher.matches()) return null;

        EnumMap<IDPattern, String> result = new EnumMap<>(IDPattern.class);
        for (IDPattern component : IDPattern.values()) {

            String value = matcher.group(component.group);
            if (value != null) result.put(component, value);
        }
        return result;
    }

    /**
     * Resolve function for button ID with the patten {@link ComponentUtil#COMPONENT_PATTERN}
     * @param type The component signature
     * @param id The snowflake signature of this component
     * @return A new component custom_id as String
     */
    public static String newComponentID(String type, Long id, Long user) {
        if (id == null || user == null) throw new IllegalArgumentException("ID and user must not be null");

        String componentID = String.join("/",
                DiscordPS.getPlugin().getName(), // Plugin name
                type,                            // Unique type String
                Long.toUnsignedString(id),       // The event ID of this component
                Long.toUnsignedString(user)      // The only allowed user to use this component
        );

        if(componentID.length() > 80)
            throw new IllegalArgumentException("[Internal] Created component's custom_id has length greater than 80 characters!");

        return componentID;
    }

    /**
     * Resolve function for button ID with the patten {@link ComponentUtil#COMPONENT_PATTERN}
     * @param type The component signature
     * @param id The snowflake signature of this component
     * @param user The only allowed user to use this component
     * @param payload The optional payload
     * @return A new component custom_id as String
     */
    public static <T> String newComponentID(String type, Long id, Long user, T payload) {
        if (id == null || user == null) throw new IllegalArgumentException("ID and user must not be null");

        String payloadValue = null;
        if (payload != null) {
            if (payload instanceof Long)
                payloadValue = Long.toUnsignedString((Long) payload);
            else if (payload instanceof Number)
                payloadValue = payload.toString(); // e.g., Integer, Double
            else
                payloadValue = payload.toString(); // default to toString
        }

        if(payloadValue == null)
            throw new IllegalArgumentException("[Internal] Created component's custom_id has payload but the payload is Null!");

        String componentID = String.join("/",
                DiscordPS.getPlugin().getName(), // Plugin name
                type,                            // Unique type String
                Long.toUnsignedString(id),       // The event ID of this component
                Long.toUnsignedString(user),     // The only allowed user to use this component
                payloadValue                     // Optional payloads
        );

        if(componentID.length() > 80)
            throw new IllegalArgumentException("[Internal] Created component's custom_id has length greater than 80 characters!");

        return componentID;
    }

    /**
     * Functional interface for generating a Discord component ID without a payload.
     * The generated ID follows the convention: {@code <plugin>/<type>/<id>/<user>}.
     * <p>
     * The {@code id} and {@code user} parameters must be Discord snowflakes
     * represented as {@link Long} values and will be encoded using {@link Long#toUnsignedString(long)}.
     */
    @FunctionalInterface
    public interface ComponentID extends java.util.function.BiFunction<Long, Long, String> {

        /**
         * Resolves a Discord {@code custom_id} string that conforms to the plugin's component ID convention.
         *
         * @param uniqueID the unique Discord snowflake ID of this component
         * @param userID the Discord snowflake ID of the user allowed to interact with this component
         * @return a string in the format {@code <plugin>/<type>/<id>/<user>}
         * @throws IllegalArgumentException if either parameter is {@code null} or if the resulting string exceeds Discord's length limits
         */
        @Override
        String apply(@NotNull Long uniqueID, @NotNull Long userID);
    }

    /**
     * Functional interface for generating a Discord component ID with an additional payload.
     * The generated ID follows the convention: {@code <plugin>/<type>/<id>/<user>/<payload>}.
     *
     * @param <T> the type of the payload to be included in the component ID.
     *            Must be an instance of {@link Number}, or any object with a meaningful {@code toString()} representation.
     */
    @FunctionalInterface
    public interface ComponentIDWithPayload<T> extends TriFunction<Long, Long, T, String> {

        /**
         * Resolves a Discord {@code custom_id} string that conforms to the plugin's component ID convention,
         * including an optional payload.
         *
         * @param uniqueID the unique Discord snowflake ID of this component
         * @param userID the Discord snowflake ID of the user allowed to interact with this component
         * @param payload the payload to be appended to the component ID; must not be {@code null}
         * @return a string in the format {@code <plugin>/<type>/<id>/<user>/<payload>}
         * @throws IllegalArgumentException if any parameter is {@code null} or if the resulting string exceeds Discord's length limits
         */
        @Override
        String apply(@NotNull Long uniqueID, @NotNull Long userID, @NotNull T payload);
    }

    @FunctionalInterface
    interface TriFunction<A,B,C,R> {

        R apply(A a, B b, C c);

        @NotNull
        @Contract(pure = true)
        default <V> TriFunction<A, B, C, V> andThen(java.util.function.Function<? super R, ? extends V> after) {
            java.util.Objects.requireNonNull(after);
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }
    }

}
