package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotFetch;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * Slash command events for the command {@code /plot fetch}
 *
 * @see #onConfirmOverride(InteractionHook, OnPlotFetch)
 * @see #onCreateRegister(InteractionHook, OnPlotFetch)
 * @see #onCreateUntracked(InteractionHook, OnPlotFetch)
 * @see #onConfirmFetch(InteractionHook, OnPlotFetch)
 */
public interface PlotFetchEvent {

    /**
     * Interacted when user confirm the prompt to override plot data of an existing thread.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onConfirmOverride(InteractionHook hook, OnPlotFetch interaction);

    /**
     * Interacted when user confirm to fetch the plot with their slash command inputs.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onConfirmFetch(InteractionHook hook, OnPlotFetch interaction);

    /**
     * If the fetch process is creating a new plot thread,
     * invoked when user decided to create and register the new thread.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onCreateRegister(InteractionHook hook, OnPlotFetch interaction);

    /**
     * If the fetch process is creating a new plot thread,
     * invoked when user decided to create the new thread as un-tracked.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotFetch} interaction payload of this event.
     */
    void onCreateUntracked(InteractionHook hook, OnPlotFetch interaction);

    /**
     * Special message to send when fetch status is picked more than one
     *
     * @param hook The interaction hook to reply to
     * @param pickedStatus The first picked status that will be applied to this command
     */
    static void onManyStatusPicked(@NotNull InteractionHook hook, @NotNull String pickedStatus) {
        hook.sendMessageEmbeds(new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setTitle("Picked more than one status!")
                .setDescription("The plot will be create only with the first picked status: `" + pickedStatus + "`")
                .build())
            .setEphemeral(true)
            .queue();
    }
}
