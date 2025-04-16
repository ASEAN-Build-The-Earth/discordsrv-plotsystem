package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.ChannelType;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.CommandInteraction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.internal.interactions.CommandInteractionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.interactions.InteractionImpl;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.SetupHelpEvent;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.core.providers.DiscordCommandProvider;
import github.tintinkung.discordps.commands.SetupCommand;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;

import static github.tintinkung.discordps.Constants.NEW_CONFIRM_AVATAR_BUTTON;
import static github.tintinkung.discordps.Constants.NEW_PROVIDED_IMAGE_BUTTON;

/**
 * Discord slash command listener
 *
 */
@SuppressWarnings("unused")
final public class DiscordCommandListener extends DiscordCommandProvider implements EventListener {
    private static final String SLASH_SETUP_WEBHOOK =  SetupCommand.SETUP + "/" + SetupCommand.WEBHOOK;
    private static final String SLASH_SETUP_CHECKLIST =  SetupCommand.SETUP + "/" + SetupCommand.HELP;

    public DiscordCommandListener(DiscordPS plugin) {
        super(plugin);
    }

    /**
     * Remove command data from getting listen to
     * including its interactions.
     * {@inheritDoc}
     */
    @Override
    public void clearCommands() {
        super.clearCommands();
        super.clearInteractions();
    }

    /**
     * Entry point for /setup webhook command
     * @param event Slash command event activated by JDA
     * @see SetupWebhookEvent#onSetupWebhook(ActionRow, InteractionHook, OnSetupWebhook) Event handler
     */
    @SlashCommand(path = SLASH_SETUP_WEBHOOK)
    public void onSetupWebhook(@NotNull SlashCommandEvent event) {
        DiscordPS.debug("Got slash command: " + event.getType());

        // Send instant response
        sendDeferReply(event, false);

        OnSetupWebhook interaction = new OnSetupWebhook(
            event.getUser().getIdLong(),
            event.getIdLong(),
            Objects.requireNonNull(event.getOption(SetupCommand.WEBHOOK_NAME)).getAsString(),
            Objects.requireNonNull(event.getOption(SetupCommand.WEBHOOK_CHANNEL)).getAsLong()
        );

        ActionRow actionRow = ActionRow.of(
            Button.primary(
                NEW_CONFIRM_AVATAR_BUTTON.apply(
                    event.getIdLong(),
                    event.getUser().getIdLong()),
        "Attach Image"),
            Button.secondary(
                NEW_PROVIDED_IMAGE_BUTTON.apply(
                    event.getIdLong(),
                    event.getUser().getIdLong()),
        "Already Provided")
        );

        // Send the command event to its instance
        this.getCommands()
            .fromClass(SetupCommand.class)
            .getWebhookCommand()
            .onSetupWebhook(actionRow, event.getHook(), interaction);

        // Update our interactions
        this.getInteractions()
            .putPayload(event.getIdLong(), interaction);
    }

    // 1361283234855391262

    /**
     * Entry point for setup checklist command
     * @see SetupHelpEvent#onSetupHelp(InteractionHook) Event Handler
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_SETUP_CHECKLIST)
    public void onSetupHelp(@NotNull SlashCommandEvent event) {
        DiscordPS.info("Got slash command: " + event.getType());

        sendDeferReply(event, true);

        this.getCommands()
            .fromClass(SetupCommand.class)
            .getHelpCommand()
            .onSetupHelp(event.getHook());
    }

    private void sendDeferReply(CommandInteraction event, boolean ephemeral) {

        if(event.getChannelType() == ChannelType.UNKNOWN) {

            event.deferReply(true).addEmbeds(new EmbedBuilder()
                .setTitle("Cannot Interact on a Thread Channel")
                .setDescription("Our discord API (JDA) is outdated. please contact our developer to request this functionality.")
                .setColor(Color.RED)
                .build())
                .queue();

            return;
        }

        event.deferReply(ephemeral).queue();
    }

}
