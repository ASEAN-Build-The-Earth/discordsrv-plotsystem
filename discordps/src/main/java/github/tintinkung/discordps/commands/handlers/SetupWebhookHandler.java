package github.tintinkung.discordps.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ComponentInteraction;
import github.tintinkung.discordps.commands.SetupCommand;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.core.system.components.buttons.AvailableButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.PluginButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.InteractiveButtonHandler;
import github.tintinkung.discordps.commands.providers.AttachmentProviderButton;

/**
 * Provides handler for setup webhook command events
 */
public enum SetupWebhookHandler implements AvailableButtonHandler {
    ON_SUBMIT_AVATAR_IMAGE(new AttachmentProviderButton(message -> new EventForwarder<>(message, event -> event::onConfirmAvatar))),
    ON_PROVIDED_AVATAR(new EventForwarder<MessageChannel>(ComponentInteraction::getChannel, event -> event::onConfirmAvatarProvided)),
    ON_CONFIRM_CONFIG(new EventForwarder<Message>(ComponentInteraction::getMessage, event -> event::onConfirmConfig)),
    ON_CANCEL_CONFIG(new ExitButton());

    private final PluginButtonHandler handler;

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
