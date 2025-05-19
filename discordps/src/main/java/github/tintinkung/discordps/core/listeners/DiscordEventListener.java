package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.DisconnectEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.ShutdownEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SelectionMenuEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.requests.CloseCode;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.CommandEvent;
import github.tintinkung.discordps.commands.interactions.*;
import github.tintinkung.discordps.core.system.AvailableButton;
import github.tintinkung.discordps.core.system.components.PluginComponent;
import github.tintinkung.discordps.core.system.components.buttons.PluginButton;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * JDA instance listener
 * DiscordSRV on disconnect does not call any API event, so we have to handle it our own too.
 * @see <a href="https://github.com/DiscordSRV/DiscordSRV/blob/9d4734818ab27069d76f264a4cda74a699806770/src/main/java/github/scarsz/discordsrv/listeners/DiscordDisconnectListener.java#L33">
 *     github.scarsz.discordsrv.listeners.DiscordDisconnectListener
 * </a>
 */
final public class DiscordEventListener extends ListenerAdapter {

    public static CloseCode mostRecentCloseCode = null;
    private final DiscordSRVListener listener;

    public DiscordEventListener(DiscordSRVListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        handleCode(event.getCloseCode());
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        handleCode(event.getCloseCode());
    }

    private void handleCode(CloseCode closeCode) {
        if (closeCode == null) {
            return;
        }
        mostRecentCloseCode = closeCode;
        if (closeCode == CloseCode.DISALLOWED_INTENTS || !closeCode.isReconnect()) {
            DiscordPS.error("Please check if DiscordSRV plugin is loaded correctly");
            DiscordPS.getPlugin().disablePlugin("Discord Plot System cannot connect to DiscordSRV");
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if(listener.getPluginSlashCommand() == null) return;
        if(event.getButton() == null) return;

        PluginButton
            .getOpt(event.getComponent(), PluginButton::new)
            .ifPresent(button -> AvailableButton.valueOf(button.getType())
                .interact(button, event, listener.getPluginSlashCommand())
            );
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if(listener.getPluginSlashCommand() == null) return;
        if(event.getComponent() == null) return;

        PluginComponent.getOpt(event.getComponent()).ifPresent(menu -> {
            // Exit immediately for un-authorized owner
            if(!menu.getUserID().equals(event.getUser().getId())) return;

            // Update the menu to what is selected
            event.editSelectionMenu(menu.get().createCopy().setDefaultValues(event.getValues()).build()).queue();

            // Fetch our selection menu which is borrowing enum from available button's data
            InteractionEvent interactions = listener.getPluginSlashCommand();
            switch(AvailableButton.valueOf(menu.getType())) {
                case PLOT_FETCH_SELECTION -> onEntryMenuSelect(event, interactions.getAs(OnPlotFetch.class, menu.getIDLong()));
                case PLOT_DELETE_SELECTION -> onEntryMenuSelect(event, interactions.getAs(OnPlotDelete.class, menu.getIDLong()));
            }
        });
    }

    /**
     * Special handler for plot entry selection menu because it cannot be handled by {@link AvailableButton}
     *
     * <p>This will update the plot's interaction data by the given event using {@link PlotFetchInteraction#setFetchOptions(List)} </p>
     *
     * @param event The triggered event
     * @param interaction The plot interaction of this menu
     */
    private void onEntryMenuSelect(SelectionMenuEvent event, PlotFetchInteraction interaction) {
        if(interaction == null) return;

        if(event.getSelectedOptions() == null || event.getSelectedOptions().isEmpty()) return;

        if(interaction.getFetchOptions() == null)
            interaction.setFetchOptions(event.getSelectedOptions());
        else {
            interaction.getFetchOptions().clear();
            interaction.getFetchOptions().addAll(event.getSelectedOptions());
        }
    }
}
