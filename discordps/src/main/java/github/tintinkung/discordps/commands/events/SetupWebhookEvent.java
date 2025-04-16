package github.tintinkung.discordps.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;

import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;

/**
 * Slash command events of the command <code>/setup webhook</code>
 * @see #onSetupWebhook(ActionRow, InteractionHook, OnSetupWebhook)
 * @see #onConfirmAvatarProvided(MessageChannel, OnSetupWebhook)
 * @see #onConfirmAvatar(Message, OnSetupWebhook)
 * @see #onConfirmConfig(Message, OnSetupWebhook)
 */
public interface SetupWebhookEvent {

    /**
     * 1st interaction when user first activated the command.
     * @param options Provided as 2 buttons: "Attach Image" and "Already Provided"
     * @param hook The message hook
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     * @see github.tintinkung.discordps.core.listeners.DiscordCommandListener#onSetupWebhook(SlashCommandEvent)
     */
    void onSetupWebhook(ActionRow options, InteractionHook hook, OnSetupWebhook interaction);

    /**
     * 2nd interaction when user clicked the "Already Provided" button
     * @param channel The channel where this event originate at.
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onConfirmAvatarProvided(MessageChannel channel, OnSetupWebhook interaction);

    /**
     * 2nd interaction when user clicked the "Attach Image" button
     * @param message The latest message where an attachment is provided by the user.
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onConfirmAvatar(Message message, OnSetupWebhook interaction);

    /**
     * Final confirmation and the end of interaction.
     * @param message The message where the final message will be displayed (embed).
     * @param interaction The {@link OnSetupWebhook} interaction payload of this event.
     */
    void onConfirmConfig(Message message, OnSetupWebhook interaction);
}
