package asia.buildtheearth.asean.discord.plotsystem.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;

import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnSetupWebhook;
import org.jetbrains.annotations.NotNull;

/**
 * Slash command events for the command {@code /setup webhook} and {@code /setup showcase}
 *
 * @see #onSetupWebhook(InteractionHook, OnSetupWebhook)
 * @see #onConfirmAvatarProvided(MessageChannel, OnSetupWebhook)
 * @see #onConfirmAvatar(Message, OnSetupWebhook)
 * @see #onConfirmConfig(Message, OnSetupWebhook)
 */
public interface SetupWebhookEvent {

    /**
     * 1st interaction when user first activated the command.
     *
     * @param hook The event's hook
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onSetupWebhook(@NotNull InteractionHook hook, @NotNull OnSetupWebhook interaction);

    /**
     * 2nd interaction when user clicked the "Already Provided" button
     *
     * @param channel The channel where this event originate at.
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onConfirmAvatarProvided(@NotNull MessageChannel channel, @NotNull OnSetupWebhook interaction);

    /**
     * 2nd interaction when user clicked the "Attach Image" button
     *
     * @param message The latest message where an attachment is provided by the user.
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onConfirmAvatar(@NotNull Message message, @NotNull OnSetupWebhook interaction);

    /**
     * Final confirmation and the end of interaction.
     *
     * @param message The message where the final message will be displayed (embed).
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onConfirmConfig(@NotNull Message message, @NotNull OnSetupWebhook interaction);
}
