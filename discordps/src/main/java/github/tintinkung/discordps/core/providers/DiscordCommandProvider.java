package github.tintinkung.discordps.core.providers;

import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.util.SchedulerUtil;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.CommandEvent;
import github.tintinkung.discordps.commands.interactions.InteractionEvent;
import github.tintinkung.discordps.commands.interactions.InteractionPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class DiscordCommandProvider
    extends PluginProvider
    implements SlashCommandProvider, InteractionEvent, CommandEvent {

    /**
     * Internal slash command object management.
     * Stores all available {@link PluginSlashCommand}
     * which is the wrapper of all slash command instances.
     */
    private final Set<PluginSlashCommand> commandSet;

    /**
     * Internal commandData instance, map available class into
     * the actual {@link CommandData} instance it is registered in.
     */
    private final Map<Class<? extends CommandData>, CommandData> registered;

    /**
     * The runtime interactions from user activated slash command events.
     * Provide {@link InteractionPayload} for each retrospective interaction.
     */
    private final Map<Long, InteractionPayload> interactions;

    public DiscordCommandProvider(DiscordPS plugin) {
        super(plugin);
        this.commandSet = new HashSet<>();
        this.registered = new HashMap<>();
        this.interactions = new HashMap<>();
    }

    /**
     * Register a new guild-based slash command
     * @param command {@link CommandData} to register
     * @param guildID The guild to register slash command in
     * @param <T> extends CommandData
     */
    public <T extends CommandData> void register(T command, @Nullable String... guildID) {
        PluginSlashCommand slashCommand = new PluginSlashCommand(this.plugin, command, guildID);
        commandSet.add(slashCommand);
        registered.put(command.getClass(), command);
    }

    /**
     * Clear all registered slash command data.
     * Required an API update for changes to take effects.
     */
    public void clearCommands() {
        commandSet.clear();
        registered.clear();
    }

    @Override
    public <T extends CommandData> T fromClass(Class<T> command) {
        CommandData slashCommand = registered.get(command);
        if (slashCommand == null)
            throw new IllegalStateException("[Internal Exception] "
            + "No command instance available for class: "
            + command.getName());
        return command.cast(slashCommand);
    }

    @Override
    public CommandEvent getCommands() {
        return this;
    }

    @Override
    public InteractionEvent getInteractions() {
        return this;
    }


    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return commandSet;
    }


    @Override
    public void clearInteractions() {
        interactions.clear();
    }

    @Override
    public void removeInteraction(Long id) {
        interactions.remove(id);
    }

    @Override
    public InteractionPayload getPayload(Long id) {
        return interactions.get(id);
    }

    @Override
    public <T extends InteractionPayload> void putPayload(Long id, T payload) {
        interactions.put(id, payload);

        // Payload is guarantee to be expired in 15 minutes (15min * 60000ms / 50tick = 18000)
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), () -> this.removeInteraction(id), 18000);
    }

    @Override
    public <T extends InteractionPayload> T getAs(@NotNull Class<T> interaction, Long id) {
        InteractionPayload payload = getPayload(id);

        if (interaction.isInstance(payload))
            return interaction.cast(payload);
        else throw new ClassCastException(
            "Payload with id " + id
            + " is not of type "
            + interaction.getSimpleName()
        );
    }
}