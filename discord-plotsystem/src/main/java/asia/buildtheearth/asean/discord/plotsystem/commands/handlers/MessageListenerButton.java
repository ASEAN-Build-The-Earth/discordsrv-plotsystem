package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.components.buttons.InteractiveButtonHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

class MessageListenerButton extends asia.buildtheearth.asean.discord.components.buttons.MessageListenerButton {

    private final Predicate<Message> retrievalCheck;

    /**
     * Provide this plugin button with attachment message.
     *
     * @param retrievalCheck Required check after retrieving the message to determine whether to forward the event to sender or not.
     * @param sender The interaction sender that will be triggered when the provider successfully resolved message data.
     */
    public MessageListenerButton(Predicate<Message> retrievalCheck, Function<Message, InteractiveButtonHandler> sender) {
        super(sender);
        this.retrievalCheck = retrievalCheck;
    }

    /**
     * General error embed if the listening action fails.
     *
     * @return Built message embed to be sent
     */
    protected @NotNull MessageEmbed errorEmbed() {
        return DiscordPS.getSystemLang()
            .getEmbedBuilder(asia.buildtheearth.asean.discord.plotsystem.core
                .system.io.lang.CommandInteractions.EMBED_ATTACH_IMAGE_FAILED)
            .setColor(Constants.RED)
            .build();
    }

    /**
     * If message is not sent, send an error embed to notify the error.
     *
     * @param interaction The interaction data of this listener including all info like {@link InteractiveButtonHandler}
     * @return The button as enabled again to listen to more message.
     */
    @Override
    protected boolean onMessageNotSent(@NotNull InteractionData interaction) {
        interaction.event().replyEmbeds(errorEmbed()).setEphemeral(true).queue();
        return super.onMessageNotSent(interaction);
    }

    /**
     * If message is received, check if there exist attachment in the message.
     *
     * @param interaction The interaction data of this listener including all info like {@link InteractiveButtonHandler}
     * @param message      The received message instance.
     * @return The event forwarded if attachment is not empty.
     */
    @Override
    protected boolean onMessageReceived(@NotNull InteractionData interaction, @NotNull Message message) {
        // No attachment provided
        if(retrievalCheck.test(message)) {
            interaction.event().replyEmbeds(errorEmbed()).setEphemeral(true).queue();
            interaction.event().editButton(interaction.button().get().asEnabled()).queue();
            return false;
        }

        // Message provided by wrong user
        if(interaction.event().getUser().getIdLong() != message.getAuthor().getIdLong()) {
            interaction.event().replyEmbeds(errorEmbed()).setEphemeral(true).queue();
            interaction.event().editButton(interaction.button().get().asEnabled()).queue();
            return false;
        }

        return super.onMessageReceived(interaction, message);
    }

    /**
     * If the retrieving failed, send an error message
     *
     * @param interaction The interaction data of this listener including all info like {@link InteractiveButtonHandler}
     * @param error        The {@link Throwable} error that cause the retrieval to fails.
     */
    @Override
    protected void onMessageRetrievingFailed(@NotNull InteractionData interaction, @NotNull Throwable error) {
        interaction.event().replyEmbeds(errorEmbed()).setEphemeral(true).queue();
        DiscordPS.error(getClass().getSimpleName() + " Failed to retrieve message data: " + error.toString());
    }
}
