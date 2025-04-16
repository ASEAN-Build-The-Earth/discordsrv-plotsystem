package github.tintinkung.discordps.commands.interactions;

/**
 * Interaction handle for all active interactions.
 * @see #getInteractions()
 */
public interface InteractionEvent {

    void removeInteraction(Long id);

    void clearInteractions();

    InteractionPayload getPayload(Long id);

    <T extends InteractionPayload> void putPayload(Long id, T payload);

    <T extends InteractionPayload> T getAs(Class<T> interaction, Long id);

    InteractionEvent getInteractions();
}
