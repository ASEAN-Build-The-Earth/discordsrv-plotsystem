package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.InteractionEvent;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import github.tintinkung.discordps.core.system.components.buttons.InteractiveButtonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.function.Function;

public class AttachmentProviderButton implements InteractiveButtonHandler {
    private final Function<Message, InteractiveButtonHandler> sender;

    /**
     * Provide this plugin button with attachment message.
     *
     * @param sender The interaction sender that will be triggered when the provider successfully resolved message data.
     */
    public AttachmentProviderButton(Function<Message, InteractiveButtonHandler> sender) {
        this.sender = sender;
    }

    @Override
    public void onInteracted(PluginButton button, ButtonClickEvent event, InteractionEvent interactions) {
        TextChannel channel = event.getInteraction().getTextChannel();
        MessageReference lastMsg = getInteractionLastMessage(channel);

        MessageEmbed errorEmbed = new EmbedBuilder()
                .setTitle("Please send a message with attachment")
                .setDescription("And make sure it is the latest message before confirming.")
                .setColor(Color.RED)
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
            DiscordPS.error("Interaction only support in a text channel");
            return null;
        }
    }
}
