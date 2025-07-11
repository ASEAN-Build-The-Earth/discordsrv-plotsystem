package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import asia.buildtheearth.asean.discord.components.buttons.ForwardEventButton;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotDeleteEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotDelete;
import asia.buildtheearth.asean.discord.components.buttons.AvailableButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.InteractiveButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;

/**
 * Interaction handler for the command {@code /plotctl delete}
 *
 * @see PlotDeleteEvent
 */
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
