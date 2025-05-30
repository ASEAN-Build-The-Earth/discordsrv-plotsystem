package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.commands.providers.SlashCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Contract for handling Discord slash command interactions.
 */
public interface CommandEvent {

    /**
     * Handles a Discord slash command interaction by deferring the reply, resolving the appropriate command handler,
     * and invoking it with a payload supplier.
     *
     * @see #onSlashCommand(SlashCommandEvent, boolean, Class, Function) For triggering slash command without payload
     *
     * @param event     The {@link SlashCommandEvent} received from JDA.
     * @param ephemeral Whether the reply should be ephemeral (visible only to the user).
     * @param type      The {@link CommandData} class representing the type of command to resolve.
     * @param resolver  A function that maps the given {@link CommandData} instance to its corresponding
     *                  {@link SlashCommand} handler.
     * @param payload   A supplier that provides the interaction payload to be passed to the handler.
     *                  This may return {@code null} if the command does not require payload data.
     * @param <T>       The type of {@link Interaction} payload expected by the command.
     * @param <V>       The type of {@link CommandData} used to resolve the command handler.
     */
    <T extends Interaction, V extends CommandData>
    void onSlashCommand(@NotNull SlashCommandEvent event,
                        boolean ephemeral,
                        @NotNull Class<V> type,
                        @NotNull Function<V, SlashCommand<T>> resolver,
                        @NotNull Supplier<@Nullable T> payload);

    /**
     * Handles a Discord slash command interaction without a payload.
     *
     * @param event     The {@link SlashCommandEvent} received from JDA.
     * @param ephemeral Whether the reply should be ephemeral (visible only to the user).
     * @param type      The {@link CommandData} class representing the type of command to resolve.
     * @param resolver  A function that maps the given {@code CommandData} instance to its corresponding
     *                  {@link SlashCommand} handler.
     * @param <T>       The type of {@link Interaction} payload expected by the command.
     * @param <V>       The type of {@link CommandData} used to resolve the command handler.
     */
    <T extends Interaction, V extends CommandData>
    void onSlashCommand(@NotNull SlashCommandEvent event,
                        boolean ephemeral,
                        @NotNull Class<V> type,
                        @NotNull Function<V, SlashCommand<T>> resolver);
}
