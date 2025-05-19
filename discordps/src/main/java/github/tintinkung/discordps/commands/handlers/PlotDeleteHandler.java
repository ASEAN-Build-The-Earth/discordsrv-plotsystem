package github.tintinkung.discordps.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.tintinkung.discordps.commands.PlotCommand;
import github.tintinkung.discordps.commands.events.PlotDeleteEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;
import github.tintinkung.discordps.core.system.components.buttons.AvailableButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.InteractiveButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.PluginButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;

public enum PlotDeleteHandler implements AvailableButtonHandler {
    ON_DELETE_SELECTION((SimpleButtonHandler) null),
    ON_DELETE_CONFIRM(new EventForwarder(new MenuButton(), event -> event::onDeleteConfirm)),
    ON_DELETE_CANCEL(new MenuExitButton());

    private static class EventForwarder extends ForwardEventButton<PlotCommand, OnPlotDelete, InteractionHook> {
        private EventForwarder(SimpleButtonHandler response, Handler<PlotDeleteEvent, OnPlotDelete, InteractionHook> handler) {
            super(PlotCommand.class, OnPlotDelete.class, Interaction::getHook, command -> handler.apply(command.getDeleteEvent()), response);
        }
    }

    private final PluginButtonHandler handler;

    PlotDeleteHandler(InteractiveButtonHandler handler) {
        this.handler = handler;
    }

    PlotDeleteHandler(SimpleButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public PluginButtonHandler getHandler() {
        return handler;
    }
}
