package github.tintinkung.discordps.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.tintinkung.discordps.commands.interactions.Interaction;
import github.tintinkung.discordps.commands.interactions.InteractionEvent;
import github.tintinkung.discordps.core.system.components.buttons.InteractiveButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Button handler that forwards a Discord button interaction to a command handler.
 *
 * <p>Usage: Inherit this class and specify concrete types {@code T} command, {@code V} payload.</p>
 *
 * <p>Then, provide a {@link Handler} that resolves the method to invoke using a {@link java.util.function.BiConsumer},
 * where the first parameter is the forward-able event data {@code E}, and the second is the command payload {@code V}.</p>
 *
 * @param <T> the type of the originating command (extends {@link CommandData})
 * @param <V> the type of the interaction payload to forward (extends {@link Interaction})
 * @param <E> the type of data extracted from the button click event to forward
 */
public class ForwardEventButton<T extends CommandData, V extends Interaction, E> implements InteractiveButtonHandler {

    /**
     * Default response for button interaction will disable the button then forward event to the command.
     */
    private static final SimpleButtonHandler DEFAULT_RESPONSE = (button, event) -> event.editComponents(ActionRow.of(button.get().asDisabled())).queue();

    /**
     * Functional interface to resolve a handler from a command instance.
     *
     * @param <T> the command type
     * @param <V> the interaction type
     * @param <E> the forwarded data type
     */
    @FunctionalInterface
    protected interface Resolver<T extends CommandData, V extends Interaction, E> extends Function<T, BiConsumer<E, V>> {}

    /**
     * Functional interface to extract forward-able data from a button click event.
     *
     * @param <E> the type of data to forward
     */
    @FunctionalInterface
    protected interface Forwarder<E> extends Function<ButtonClickEvent, E> {}

    /**
     * Functional interface defining how the command handles the forwarded data.
     *
     * @param <T> the command type
     * @param <V> the interaction type
     * @param <E> the type of forwarded data
     */
    @FunctionalInterface
    protected interface Handler<T, V extends Interaction, E> extends Function<T, BiConsumer<E, V>> {}

    /**
     * The command class that this button is associated with.
     */
    private final @NotNull  Class<T> command;

    /**
     * The interaction type to which the event payload will be forwarded.
     */
    private final @NotNull  Class<V> interaction;

    /**
     * Function that resolves a handler ({@code BiConsumer<E, V>}) from the command instance.
     */
    private final @NotNull  Resolver<T, V, E> resolver;

    /**
     * Function that extracts the forward-able data of type E from a ButtonClickEvent.
     */
    private final @NotNull  Forwarder<E> forwarder;

    /**
     * Interaction response before forwarding this event
     */
    private final @Nullable SimpleButtonHandler response;

    /**
     * Forward an event to a command with a default response action,
     * the default response will update the button as disabled.
     *
     * @param command The command to forward an event to
     * @param interaction The event to be forwarded to
     * @param forwarder The forward type to what would be accepted
     * @param resolver Resolver function that supply event method
     * @see #DEFAULT_RESPONSE
     */
    protected ForwardEventButton(@NotNull Class<T> command,
                                 @NotNull Class<V> interaction,
                                 @NotNull Forwarder<E> forwarder,
                                 @NotNull Resolver<T, V, E> resolver) {
        this(command, interaction, forwarder, resolver, DEFAULT_RESPONSE);
    }

    /**
     * Forward an event to a command with a defined response action,
     * set response to null to ignore the action.
     *
     * @param command The command to forward an event to
     * @param interaction The event to be forwarded to
     * @param forwarder The forward type to what would be accepted
     * @param resolver Resolver function that supply event method
     * @param response Interaction response before forwarding this event
     */
    protected ForwardEventButton(@NotNull Class<T> command,
                                 @NotNull Class<V> interaction,
                                 @NotNull Forwarder<E> forwarder,
                                 @NotNull Resolver<T, V, E> resolver,
                                 @Nullable SimpleButtonHandler response) {
        this.command = command;
        this.interaction = interaction;
        this.forwarder = forwarder;
        this.resolver = resolver;
        this.response = response;
    }

    @Override
    public void onInteracted(@NotNull PluginButton button,
                             @NotNull ButtonClickEvent event,
                             @NotNull InteractionEvent interactions) {
        if(this.response != null) this.response.onInteracted(button, event);

        this.resolver
            .apply(interactions.fromClass(this.command))
            .accept(forwarder.apply(event), interactions.getAs(this.interaction, button.getIDLong()));
    }
}
