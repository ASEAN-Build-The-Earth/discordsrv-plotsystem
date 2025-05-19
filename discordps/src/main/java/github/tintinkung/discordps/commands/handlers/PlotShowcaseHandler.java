package github.tintinkung.discordps.commands.handlers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.tintinkung.discordps.commands.PlotCommand;
import github.tintinkung.discordps.commands.events.PlotShowcaseEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotShowcase;
import github.tintinkung.discordps.core.system.components.buttons.AvailableButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.InteractiveButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.PluginButtonHandler;
import github.tintinkung.discordps.core.system.components.buttons.SimpleButtonHandler;

public enum PlotShowcaseHandler implements AvailableButtonHandler {
    ON_SHOWCASE_CONFIRM(new EventForwarder(event -> event::onShowcaseConfirm)),
    ON_SHOWCASE_DISMISS(new ExitButton());

    private static class EventForwarder extends ForwardEventButton<PlotCommand, OnPlotShowcase, InteractionHook> {
        private EventForwarder(Handler<PlotShowcaseEvent, OnPlotShowcase, InteractionHook> handler) {
            super(PlotCommand.class, OnPlotShowcase.class, Interaction::getHook, command -> handler.apply(command.getShowcaseEvent()));
        }
    }

    private final PluginButtonHandler handler;

    PlotShowcaseHandler(InteractiveButtonHandler handler) {
        this.handler = handler;
    }

    PlotShowcaseHandler(SimpleButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public PluginButtonHandler getHandler() {
        return handler;
    }
}
