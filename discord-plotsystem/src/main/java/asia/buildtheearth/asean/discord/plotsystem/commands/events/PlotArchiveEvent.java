package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import org.jetbrains.annotations.NotNull;

/**
 * Slash command events for the command {@code /plotctl archive} and {@code /review archive}
 *
 * @see #onImagesAttached(Message, OnPlotArchive)
 * @see #onImagesProvided(InteractionHook, OnPlotArchive)
 * @see #onConfirmArchive(InteractionHook, OnPlotArchive)
 * @see #onShowcaseConfirmed(InteractionHook, OnPlotArchive)
 */
public interface PlotArchiveEvent {

    /**
     * 1st interaction when user clicked the {@code Attach Image} button
     *
     * @param message The latest message where an attachment is provided by the user.
     * @param interaction The {@link OnPlotArchive} interaction payload of this event.
     */
    void onImagesAttached(@NotNull Message message, @NotNull OnPlotArchive interaction);

    /**
     * 1st interaction when user clicked the {@code Already Provided} button
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotArchive} interaction payload of this event.
     */
    void onImagesProvided(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction);

    /**
     * Interacted when user confirmed to archive the plot.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotArchive} interaction payload of this event.
     */
    void onConfirmArchive(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction);

    /**
     * Interacted after the archival process is completed and a prompted to showcase this plot pop up.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnPlotArchive} interaction payload of this event.
     */
    void onShowcaseConfirmed(@NotNull InteractionHook hook, @NotNull OnPlotArchive interaction);
}
