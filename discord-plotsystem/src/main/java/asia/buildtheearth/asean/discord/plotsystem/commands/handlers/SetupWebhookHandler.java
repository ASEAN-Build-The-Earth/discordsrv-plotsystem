package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ComponentInteraction;
import asia.buildtheearth.asean.discord.plotsystem.commands.SetupCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.SetupWebhookEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnSetupWebhook;
import asia.buildtheearth.asean.discord.components.buttons.AvailableButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.InteractiveButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.ForwardEventButton;

import java.util.function.Function;

/**
 * Interaction handler for the command {@code /setup webhook} and {@code /setup showcase}
 *
 * @see SetupWebhookEvent
 */
public enum SetupWebhookHandler implements AvailableButtonHandler {
    ON_SUBMIT_AVATAR_IMAGE(new EventListener(message -> new EventForwarder<>(message, event -> event::onConfirmAvatar))),
    ON_PROVIDED_AVATAR(new EventForwarder<MessageChannel>(ComponentInteraction::getChannel, event -> event::onConfirmAvatarProvided)),
    ON_CONFIRM_CONFIG(new EventForwarder<Message>(ComponentInteraction::getMessage, event -> event::onConfirmConfig)),
    ON_CANCEL_CONFIG(new ExitButton());

    private final PluginButtonHandler handler;

    private static class EventListener extends MessageListenerButton {
        public EventListener(Function<Message, InteractiveButtonHandler> sender) {
            super(message -> message.getAttachments().isEmpty(), sender);
        }
    }

    private static class EventForwarder<E> extends ForwardEventButton<SetupCommand, OnSetupWebhook, E> {
        private EventForwarder(Forwarder<E> forwarder, Handler<SetupWebhookEvent, OnSetupWebhook, E> handler) {
            super(SetupCommand.class, OnSetupWebhook.class, forwarder, command -> handler.apply(command.getWebhookEvent()));
        }

        private EventForwarder(E forwarder, Handler<SetupWebhookEvent, OnSetupWebhook, E> handler) {
            super(SetupCommand.class, OnSetupWebhook.class, ignored -> forwarder, command -> handler.apply(command.getWebhookEvent()), null);
        }
    }

    SetupWebhookHandler(InteractiveButtonHandler handler) {
        this.handler = handler;
    }

    SetupWebhookHandler(SimpleButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public PluginButtonHandler getHandler() {
        return handler;
    }
}
