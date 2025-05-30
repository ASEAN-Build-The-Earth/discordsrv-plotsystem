package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.ChannelType;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.CommandInteraction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.util.SchedulerUtil;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.CommandEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.InteractionEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.commands.providers.SlashCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provider/Manager for discord slash command interactions.
 * Managed externally by DiscordSRV API {@link SlashCommandProvider}.
 *
 * @see #register(String, CommandData...) Register slash commands
 * @see #fromClass(Class) Get the registered slash command
 * @see #putPayload(Long, Interaction) Track a slash command event w/ payload
 * @see #getAs(Class, Long) Get the tracked slash command interaction
 */
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
     * Provide {@link Interaction} for each retrospective interaction.
     */
    private final Map<Long, Interaction> interactions;

    /**
     * Construct plugin base slash command provider
     *
     * @param plugin Plugin to register commands in
     */
    public DiscordCommandProvider(DiscordPS plugin) {
        super(plugin);
        this.commandSet = new HashSet<>();
        this.registered = new HashMap<>();
        this.interactions = new HashMap<>();
    }

    /**
     * Register a new guild-based slash command
     *
     * @param commands list {@link CommandData} to register
     * @param guildID The guild to register slash command in
     */
    public void register(@NotNull String guildID, CommandData... commands) {
        for(CommandData command : commands) {
            PluginSlashCommand slashCommand = new PluginSlashCommand(this.plugin, command, guildID);
            commandSet.add(slashCommand);
            registered.put(command.getClass(), command);
        }
    }

    /**
     * Clear all registered slash command data.
     * Required an API update for changes to take effects.
     */
    public void clearCommands() {
        commandSet.clear();
        registered.clear();
    }

    /**
     *
     * {@inheritDoc}
     */
    public <T extends CommandData> T fromClass(Class<T> command) {
        CommandData slashCommand = registered.get(command);
        if (slashCommand == null)
            throw new IllegalStateException("[Internal Exception] "
                    + "No command instance available for class: "
                    + command.getName());
        return command.cast(slashCommand);
    }

    /**
     * If guard to check on every command because our plugin
     * will not support thread channel and will output it as {@link ChannelType#UNKNOWN}
     *
     * @param event The command that is triggered
     * @return Whether the triggered channel is valid
     */
    private boolean isUnknownChannel(@NotNull CommandInteraction event) {
        if(event.getChannelType() == ChannelType.UNKNOWN) {

            event.deferReply(true).addEmbeds(new EmbedBuilder()
                .setTitle("Cannot Interact on a Thread Channel")
                .setDescription("Our discord API (JDA) is outdated. please contact our developer to request this functionality.")
                .setColor(Color.RED)
                .build())
            .queue();

            return true;
        }
        else return false;
    }

    /**
     * {@inheritDoc}
     */
    public <T extends Interaction, V extends CommandData>
        void onSlashCommand(@NotNull SlashCommandEvent event,
                            boolean ephemeral,
                            @NotNull Class<V> type,
                            @NotNull Function<V, SlashCommand<T>> resolver,
                            @NotNull Supplier<@Nullable T> payload) {

        if(isUnknownChannel(event)) return;
        else event.deferReply(ephemeral).queue();

        T interaction = payload.get();

        if(interaction != null) this.putPayload(event.getIdLong(), interaction);

        resolver.apply(this.fromClass(type)).trigger(event.getHook(), interaction);
    }

    /**
     * {@inheritDoc}
     */
    public <T extends Interaction, V extends CommandData>
        void onSlashCommand(@NotNull SlashCommandEvent event,
                            boolean ephemeral,
                            @NotNull Class<V> type,
                            @NotNull Function<V, SlashCommand<T>> resolver) {
        onSlashCommand(event, ephemeral, type, resolver, () -> null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Interaction> @Nullable T getAs(@NotNull Class<T> interaction, Long eventID) {
        Interaction payload = getPayload(eventID);

        if(payload == null) return null;

        if (interaction.isInstance(payload)) return interaction.cast(payload);
        else throw new ClassCastException("Payload with event id <" + Long.toUnsignedString(eventID) + "> is not of type " + interaction.getSimpleName());
    }

    /**
     * Get all registered slash command provider to be process by DiscordSRV API
     *
     * @return Set of registered provider
     */
    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return commandSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearInteractions() {
        interactions.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeInteraction(Long eventID) {
        interactions.remove(eventID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Interaction getPayload(Long eventID) {
        return interactions.get(eventID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Interaction> void putPayload(Long eventID, T payload) {
        interactions.put(eventID, payload);

        // Payload is guarantee to be expired in 15 minutes (15min * 60000ms / 50tick = 18000)
        SchedulerUtil.runTaskLaterAsynchronously(DiscordPS.getPlugin(), () -> {
            DiscordPS.warning("Plugin interaction ends on a timeout (event: " + payload.getClass().getSimpleName() + ")");
            this.removeInteraction(eventID);
        }, 18000);
    }
}