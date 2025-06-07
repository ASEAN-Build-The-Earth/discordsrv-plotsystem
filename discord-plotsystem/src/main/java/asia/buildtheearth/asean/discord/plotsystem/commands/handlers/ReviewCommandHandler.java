package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import asia.buildtheearth.asean.discord.components.buttons.*;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.ReviewCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.ReviewEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnReview;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewEditCommand;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Interaction handler for the command {@code /review edit} and {@code /review send}
 *
 * @see ReviewEvent
 */
public enum ReviewCommandHandler implements AvailableButtonHandler {

    // Review editing command
    ON_REVIEW_SELECTION((SimpleButtonHandler) null),
    ON_SELECTION_CONFIRM(new EditForwarder<>(Interaction::getHook, new MenuButton(), event -> event::onSelectionConfirm)),
    ON_SELECTION_CANCEL(new MenuExitButton()),
    ON_CONTINUE_PUBLIC(new EditForwarder<InteractionHook>(Interaction::getHook, event -> event::sendInstruction)),
    ON_PUBLIC_CANCEL(new ExitButton()),
    ON_MESSAGE_CONFIRM(new EventListener(message -> new EditForwarder<>(message, event -> event::onNewMessageConfirm))),
    ON_MESSAGE_CANCEL(new ExitButton()),
    ON_EDIT_CONFIRM(new EditForwarder<InteractionHook>(Interaction::getHook, event -> event::onReviewConfirm)),
    ON_EDIT_CANCEL(new ExitButton()),
    ON_EDIT_EDIT(new EditForwarder<InteractionHook>(Interaction::getHook, event -> event::sendInstruction)),
    ON_PREV_IMG_CLEAR(new EditForwarder<InteractionHook>(Interaction::getHook, event -> event::clearPreviousReview)),
    ON_PREV_IMG_ADD(new EditForwarder<InteractionHook>(Interaction::getHook, event -> event::sendInstruction)),

    // Review sending command
    SEND_SELECTION_CONFIRM(new SendForwarder<>(Interaction::getHook, new MenuButton(), event -> event::onSelectionConfirm)),
    SEND_MESSAGE_CONFIRM(new EventListener(message -> new SendForwarder<>(message, event -> event::onNewMessageConfirm))),
    SEND_CONFIRM(new SendForwarder<InteractionHook>(Interaction::getHook, event -> event::onReviewConfirm)),
    SEND_EDIT(new SendForwarder<InteractionHook>(Interaction::getHook, event -> event::sendInstruction));

    private static class SendForwarder<E> extends ForwardEventButton<ReviewCommand, OnReview, E> {
        private SendForwarder(Forwarder<E> forwarder, SimpleButtonHandler response, Handler<ReviewEvent, OnReview, E> handler) {
            super(ReviewCommand.class, OnReview.class, forwarder, command -> handler.apply(command.getSendEvent()), response);
        }

        private SendForwarder(Forwarder<E> forwarder, Handler<ReviewEvent, OnReview, E> handler) {
            super(ReviewCommand.class, OnReview.class, forwarder, command -> handler.apply(command.getSendEvent()));
        }

        private SendForwarder(E forwarder, Handler<ReviewEvent, OnReview, E> handler) {
            super(ReviewCommand.class, OnReview.class, ignored -> forwarder, command -> handler.apply(command.getSendEvent()), null);
        }
    }

    private static class EditForwarder<E> extends ForwardEventButton<ReviewCommand, OnReview, E> {
        private EditForwarder(Forwarder<E> forwarder, SimpleButtonHandler response, Handler<ReviewEvent, OnReview, E> handler) {
            super(ReviewCommand.class, OnReview.class, forwarder, command -> handler.apply(command.getEditEvent()), response);
        }

        private EditForwarder(Forwarder<E> forwarder, Handler<ReviewEvent, OnReview, E> handler) {
            super(ReviewCommand.class, OnReview.class, forwarder, command -> handler.apply(command.getEditEvent()));
        }

        private EditForwarder(E forwarder, Handler<ReviewEvent, OnReview, E> handler) {
            super(ReviewCommand.class, OnReview.class, ignored -> forwarder, command -> handler.apply(command.getEditEvent()), null);
        }
    }

    private static class EventListener extends MessageListenerButton {
        public EventListener(Function<Message, InteractiveButtonHandler> sender) {
            super(message -> StringUtils.isBlank(message.getContentRaw()), sender);
        }

        @Override
        protected @NotNull MessageEmbed errorEmbed() {
            return DiscordPS.getSystemLang()
                .getEmbedBuilder(ReviewEditCommand.EMBED_PROVIDED_ERROR)
                .setColor(Constants.RED)
                .build();
        }
    }

    private final PluginButtonHandler handler;

    ReviewCommandHandler(InteractiveButtonHandler handler) {
        this.handler = handler;
    }

    ReviewCommandHandler(SimpleButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public PluginButtonHandler getHandler() {
        return handler;
    }
}
