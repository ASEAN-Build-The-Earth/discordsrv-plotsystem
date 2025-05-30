package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.IDPattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ComponentProvider {

    /**
     * Resolve this component ID returning a new component ID string
     *
     * @param id The unique snowflake ID for this component.
     * @param user The only allowed user to use this component.
     * @return Finalized component ID that can be parse by {@link IDPattern}
     */
    String resolve(Long id, Long user);

    /**
     * Resolve this specific component ID by itself with payload.
     *
     * @param id The unique snowflake ID for this component.
     * @param user The only allowed user to use this component.
     * @param payload Optional payload for this component.
     * @return Finalized component ID that can be parse by {@link IDPattern}
     * @param <T> The payload type for this component. Accept Numbers and String
     */
    <T> String resolve(Long id, Long user, T payload);

    /**
     * Resolve function for button ID with the patten {@link IDPattern#COMPONENT_PATTERN}
     *
     * @param type The component signature
     * @param id The snowflake signature of this component
     * @param user The only allowed user to use this component
     * @return A new component custom_id as String
     */
    @Contract("_, null, _ -> fail; _, !null, null -> fail")
    default @NotNull String newComponentID(String type, Long id, Long user) {
        if (id == null || user == null) throw new IllegalArgumentException("ID and user must not be null");

        String componentID = String.join("/",
                DiscordPS.getPlugin().getName(), // Plugin name
                type,                            // Unique type String
                Long.toUnsignedString(id),       // The event ID of this component
                Long.toUnsignedString(user)      // The only allowed user to use this component
        );

        if(componentID.length() > 100)
            throw new IllegalArgumentException("[Internal] Created component's custom_id has length greater than 100 characters!");

        return componentID;
    }

    /**
     * Resolve function for button ID with the patten {@link IDPattern#COMPONENT_PATTERN}
     *
     * @param type The component signature
     * @param id The snowflake signature of this component
     * @param user The only allowed user to use this component
     * @param payload The optional payload
     * @return A new component custom_id as String
     * @param <T> The payload type for this component
     */
    @Contract("_, null, _, _ -> fail; _, !null, null, _ -> fail")
    default <T> @NotNull String newComponentID(String type, Long id, Long user, T payload) {
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
            throw new IllegalArgumentException("[Internal] Created component's custom_id has length greater than 80 characters! (" + componentID + ")");

        return componentID;
    }
}