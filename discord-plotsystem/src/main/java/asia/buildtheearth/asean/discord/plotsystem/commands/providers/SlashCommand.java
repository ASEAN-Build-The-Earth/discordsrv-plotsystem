package asia.buildtheearth.asean.discord.plotsystem.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.Interaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SlashCommand<T extends Interaction>  {
    /**
     * Trigger a command with pre-defined follow-up interactions and payload
     *
     * @param hook The triggered command interaction hook
     * @param interactions The follow-up interactions as an {@link ActionRow}
     * @param payload The payload to start this command events
     */
    void trigger(@NotNull InteractionHook hook, @Nullable T payload);
}
