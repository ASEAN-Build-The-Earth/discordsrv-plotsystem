package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.commands.interactions.InteractionEvent;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.components.buttons.InteractiveButtonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions.EMBED_ATTACH_IMAGE_FAILED;

class AttachmentListenerButton implements InteractiveButtonHandler {
    private final Function<Message, InteractiveButtonHandler> sender;

    /**
     * Provide this plugin button with attachment message.
     *
     * @param sender The interaction sender that will be triggered when the provider successfully resolved message data.
     */
    public AttachmentListenerButton(Function<Message, InteractiveButtonHandler> sender) {
        this.sender = sender;
    }

    @Override
    public void onInteracted(PluginButton button, ButtonClickEvent event, InteractionEvent interactions) {
        TextChannel channel = event.getInteraction().getTextChannel();
        MessageReference lastMsg = getInteractionLastMessage(channel);

        MessageEmbed errorEmbed = DiscordPS.getSystemLang()
            .getEmbedBuilder(EMBED_ATTACH_IMAGE_FAILED)
            .setColor(Constants.RED)
            .build();

        // Last message is not sent (it is the button message)
        if(lastMsg == null || lastMsg.getMessageIdLong() == event.getMessage().getIdLong()) {
            channel.sendMessageEmbeds(errorEmbed).queue();
            event.editButton(button.get().asEnabled()).queue();
            return;
        }

        lastMsg.resolve().queue((message -> {

            if(message.getAttachments().isEmpty()) {
                channel.sendMessageEmbeds(errorEmbed).queue();
                event.editButton(button.get().asEnabled()).queue();
                return;
            }

            event.editComponents(ActionRow.of(button.get().asDisabled())).queue();
            this.sender.apply(message).onInteracted(button, event, interactions);
        }));
    }

    public @Nullable MessageReference getInteractionLastMessage(@NotNull TextChannel channel) {
        try {
            long lastMsgID = channel.getLatestMessageIdLong();
            long channelID = channel.getIdLong();
            long guildID = channel.getGuild().getIdLong();

            return new MessageReference(
                    lastMsgID,
                    channelID,
                    guildID,
                    null,
                    channel.getJDA()
            );
        } catch (IllegalStateException ex) {
            DiscordPS.error("[Internal] Interaction only support in a text channel, "
                + "A user should not be able to trigger this");
            return null;
        }
    }
}
