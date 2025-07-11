package asia.buildtheearth.asean.discord.plotsystem.commands.handlers;

import asia.buildtheearth.asean.discord.components.buttons.ForwardEventButton;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.Interaction;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotShowcaseEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotShowcase;
import asia.buildtheearth.asean.discord.components.buttons.AvailableButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.InteractiveButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;

/**
 * Interaction handler for the command {@code /plotctl showcase}
 *
 * @see PlotShowcaseEvent
 */
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
