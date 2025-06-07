package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnReview;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

/**
 * Slash command events for the command {@code /review edit} or {@code /review send}
 *
 * @see #onSelectionConfirm(InteractionHook, OnReview)
 * @see #sendInstruction(InteractionHook, OnReview)
 * @see #onNewMessageConfirm(Message, OnReview)
 * @see #clearPreviousReview(InteractionHook, OnReview)
 * @see #onReviewConfirm(InteractionHook, OnReview)
 */
public interface ReviewEvent {
    /**
     * Interacted when user clicked on the select confirmation button
     * after delete entry is selected.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnReview} interaction payload of this event.
     */
    void onSelectionConfirm(@NotNull InteractionHook hook, @NotNull OnReview interaction);

    /**
     * Interacted after user confirmed the selected entry to review,
     * sending the instruction to review the selected plot.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnReview} interaction payload of this event.
     */
    void sendInstruction(@NotNull InteractionHook hook, @NotNull OnReview interaction);

    /**
     * Interacted when user clicked on the delete confirmation button
     * after delete entry is selected.
     *
     * @param message The event's retrieved message
     * @param interaction The {@link OnReview} interaction payload of this event.
     */
    void onNewMessageConfirm(@NotNull Message message, @NotNull OnReview interaction);

    /**
     * Additional interaction if the selection entry has previous media data attachment(s).
     *
     * <p>This interaction will delete all previous media of the selected review entry.</p>
     *
     * @param hook The event's hook
     * @param interaction The {@link OnReview} interaction payload of this event.
     */
    void clearPreviousReview(@NotNull InteractionHook hook, @NotNull OnReview interaction);

    /**
     * Final interaction that finalize all selected data and forward the information to plot's owner.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnReview} interaction payload of this event.
     */
    void onReviewConfirm(@NotNull InteractionHook hook, @NotNull OnReview interaction);
}
