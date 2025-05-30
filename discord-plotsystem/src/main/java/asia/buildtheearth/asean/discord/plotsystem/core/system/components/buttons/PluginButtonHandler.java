package asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.CommandEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.InteractionEvent;

public interface PluginButtonHandler {

    /**
     * Handles interaction logic for non-headless buttons.
     * Override this method to provide specific behavior.
     *
     * @throws IllegalArgumentException if not overridden
     */
    default void onInteracted(PluginButton button, ButtonClickEvent event, InteractionEvent interactions) {
        throw new IllegalArgumentException("Plugin button requires an implementation of interaction handling.");
    }

    /**
     * Handles interaction logic for headless buttons.
     * Override this method to provide specific behavior.
     *
     * @throws IllegalArgumentException if not overridden
     */
    default void onInteracted(PluginButton button, ButtonClickEvent event) {
        throw new IllegalArgumentException("Plugin button requires an implementation of interaction handling.");
    }

    /**
     * Invoked when a user who does not own the button attempts to interact with it.
     * This method is optional to override.
     *
     * @param event The event triggered by the unauthorized user.
     */
    default void onInteractedBadOwner(ButtonClickEvent event) {}
}
