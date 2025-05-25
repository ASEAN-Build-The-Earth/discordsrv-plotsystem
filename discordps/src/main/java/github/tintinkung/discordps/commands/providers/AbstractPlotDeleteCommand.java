package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.commands.events.PlotDeleteEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;
import github.tintinkung.discordps.core.system.AvailableButton;
import github.tintinkung.discordps.core.system.io.lang.CommandInteractions;
import github.tintinkung.discordps.core.system.io.lang.Format;
import github.tintinkung.discordps.core.system.io.lang.PlotDeleteCommand;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPlotDeleteCommand extends AbstractPlotCommand<OnPlotDelete, PlotDeleteCommand> implements PlotDeleteEvent {
    public AbstractPlotDeleteCommand(@NotNull String name) {
        super(name);
    }

    @Override
    protected abstract void onCommandTriggered(InteractionHook hook, OnPlotDelete payload);

    @Override
    protected String[] getLangArgs() {
        return new String[] { Format.MESSAGE_ID, Format.THREAD_ID };
    }

    protected static final CommandSelectionMenu DELETE_SELECTION_MENU = payload -> SelectionMenu.create(
            AvailableButton.PLOT_DELETE_SELECTION.resolve(payload.eventID, payload.userID)
    );

    protected Button getConfirmButton(@NotNull OnPlotDelete payload) {
        return Button.danger(
            AvailableButton.PLOT_DELETE_CONFIRM.resolve(payload.eventID, payload.userID),
            getLangManager().get(CommandInteractions.BUTTON_CONFIRM_DELETE)
        );
    }

    protected Button getCancelButton(@NotNull OnPlotDelete payload) {
        return Button.secondary(
            AvailableButton.PLOT_DELETE_CANCEL.resolve(payload.eventID, payload.userID),
            getLangManager().get(CommandInteractions.BUTTON_CANCEL)
        );
    }

}