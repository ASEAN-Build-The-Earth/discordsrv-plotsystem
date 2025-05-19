package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.commands.interactions.Interaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class SubcommandProvider<T extends Interaction> extends SubcommandData implements SlashCommand<T> {

    public SubcommandProvider(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    public final void trigger(@NotNull InteractionHook hook, @Nullable T payload) {
        if (payload == null) onCommandTriggered(hook);
        else onCommandTriggered(hook, payload);
    }

    protected interface CommandButton extends Function<Interaction, Button> {};

    protected interface CommandSelectionMenu extends Function<Interaction, SelectionMenu.Builder> {};

    /**
     * Trigger a command with specified payload
     *
     * @param hook The triggered command interaction hook
     * @param payload The payload to start this command events
     */
    protected void onCommandTriggered(InteractionHook hook, T payload) {
        throw new IllegalArgumentException("No implementation found for expected slash command payload.");
    }

    /**
     * Trigger a command without any payload
     *
     * @param hook The triggered command interaction hook
     */
    protected void onCommandTriggered(InteractionHook hook) {
        throw new IllegalArgumentException("No implementation found for expected slash command.");
    }
}
