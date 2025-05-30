package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotArchiveEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.AvailableButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.InteractiveButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.commands.providers.AttachmentProviderButton;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.SimpleButtonHandler;

public enum PlotArchiveHandler implements AvailableButtonHandler {
    // Plot archive command
    ON_IMAGES_ATTACHED(new AttachmentProviderButton(message -> new EventForwarder<>(message, event -> event::onImagesAttached))),
    ON_IMAGES_PROVIDED(new EventForwarder<InteractionHook>(Interaction::getHook, event -> event::onImagesProvided)),
    ON_ARCHIVE_CONFIRM(new EventForwarder<InteractionHook>(Interaction::getHook, event -> event::onConfirmArchive)),
    ON_ARCHIVE_SHOWCASE_CONFIRM(new EventForwarder<InteractionHook>(Interaction::getHook, event -> event::onShowcaseConfirmed)),
    ON_ARCHIVE_SHOWCASE_DISMISS(new ExitButton()),
    ON_ARCHIVE_DISMISS(new ExitButton()),
    ON_ARCHIVE_CANCEL(new ExitButton());

    private final PluginButtonHandler handler;

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
