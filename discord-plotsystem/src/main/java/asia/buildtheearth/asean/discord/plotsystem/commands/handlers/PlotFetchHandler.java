package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotFetchEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotFetch;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.AvailableButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.InteractiveButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.components.buttons.SimpleButtonHandler;


public enum PlotFetchHandler implements AvailableButtonHandler {
    ON_FETCH_SELECTION((SimpleButtonHandler) null),
    ON_CONFIRM_FETCH(new EventForwarder(event -> event::onConfirmCreation)),
    ON_OVERRIDE_CONFIRM(new EventForwarder(new MenuButton(), event -> event::onConfirmOverride)),
    ON_OVERRIDE_CANCEL(new MenuExitButton()),
    ON_DISMISS_FETCH(new ExitButton()),
    ON_CREATE_REGISTER(new EventForwarder(event -> event::onCreateRegister)),
    ON_CREATE_UNTRACKED(new EventForwarder(event -> event::onCreateUntracked)),
    ON_CREATE_CANCEL(new ExitButton());

    private final PluginButtonHandler handler;

    private static class EventForwarder extends ForwardEventButton<PlotCommand, OnPlotFetch, InteractionHook> {
        private EventForwarder(Handler<PlotFetchEvent, OnPlotFetch, InteractionHook> handler) {
            super(PlotCommand.class, OnPlotFetch.class, Interaction::getHook, command -> handler.apply(command.getFetchEvent()));
        }

        private EventForwarder(SimpleButtonHandler response, Handler<PlotFetchEvent, OnPlotFetch, InteractionHook> handler) {
            super(PlotCommand.class, OnPlotFetch.class, Interaction::getHook, command -> handler.apply(command.getFetchEvent()), response);
        }
    }

    PlotFetchHandler(InteractiveButtonHandler handler) {
        this.handler = handler;
    }

    PlotFetchHandler(SimpleButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public PluginButtonHandler getHandler() {
        return handler;
    }
}
