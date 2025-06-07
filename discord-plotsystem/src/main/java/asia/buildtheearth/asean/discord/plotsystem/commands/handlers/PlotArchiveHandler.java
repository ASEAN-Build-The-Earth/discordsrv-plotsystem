package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotArchiveEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import asia.buildtheearth.asean.discord.components.buttons.ForwardEventButton;
import asia.buildtheearth.asean.discord.components.buttons.AvailableButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.InteractiveButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;

import java.util.function.Function;

/**
 * Interaction handler for the command {@code /plotctl archive} and {@code /review archive}
 *
 * @see PlotArchiveEvent
 */
public enum PlotArchiveHandler implements AvailableButtonHandler {
    // Plot archive command
    ON_IMAGES_ATTACHED(new EventListener(message -> new EventForwarder<>(message, event -> event::onImagesAttached))),
    ON_IMAGES_PROVIDED(new EventForwarder<InteractionHook>(Interaction::getHook, event -> event::onImagesProvided)),
    ON_ARCHIVE_CONFIRM(new EventForwarder<InteractionHook>(Interaction::getHook, event -> event::onConfirmArchive)),
    ON_ARCHIVE_SHOWCASE_CONFIRM(new EventForwarder<InteractionHook>(Interaction::getHook, event -> event::onShowcaseConfirmed)),
    ON_ARCHIVE_SHOWCASE_DISMISS(new ExitButton()),
    ON_ARCHIVE_DISMISS(new ExitButton()),
    ON_ARCHIVE_CANCEL(new ExitButton());

    private final PluginButtonHandler handler;

    private static class EventListener extends MessageListenerButton {
        public EventListener(Function<Message, InteractiveButtonHandler> sender) {
            super(message -> message.getAttachments().isEmpty(), sender);
        }
    }

    private static class EventForwarder<E> extends ForwardEventButton<PlotCommand, OnPlotArchive, E> {
        private EventForwarder(Forwarder<E> forwarder, Handler<PlotArchiveEvent, OnPlotArchive, E> handler) {
            super(PlotCommand.class, OnPlotArchive.class, forwarder, command -> handler.apply(command.getArchiveEvent()));
        }

        private EventForwarder(E forwarder, Handler<PlotArchiveEvent, OnPlotArchive, E> handler) {
            super(PlotCommand.class, OnPlotArchive.class, ignored -> forwarder, command -> handler.apply(command.getArchiveEvent()), null);
        }
    }

    PlotArchiveHandler(InteractiveButtonHandler handler) {
        this.handler = handler;
    }

    PlotArchiveHandler(SimpleButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public PluginButtonHandler getHandler() {
        return handler;
    }
}
