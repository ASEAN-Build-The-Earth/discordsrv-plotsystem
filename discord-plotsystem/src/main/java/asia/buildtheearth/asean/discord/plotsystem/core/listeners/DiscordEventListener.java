package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.DisconnectEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.ShutdownEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SelectionMenuEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.requests.CloseCode;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.*;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableButton;
import asia.buildtheearth.asean.discord.components.PluginComponent;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.commands.interactions.InteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main JDA events listener.
 *
 * <p>Listen for JDA interactions event like {@link ButtonClickEvent} and {@link SelectionMenuEvent}</p>
 * <p>Note: Also listen for disconnection and shutdown event to further shutdown this plugin gracefully.
 * (DiscordSRV on disconnect does not call any API event so we have to handle it our own)</p>
 *
 * @see ListenerAdapter
 * @see ButtonClickEvent
 * @see SelectionMenuEvent
 * @see DisconnectEvent
 * @see ShutdownEvent
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
            .getOpt(listener.getPlugin(), event.getComponent(), PluginButton::new)
            .ifPresent(button -> AvailableButton.valueOf(button.getType())
                .interact(button, event, listener.getPluginSlashCommand())
            );
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if(listener.getPluginSlashCommand() == null) return;
        if(event.getComponent() == null) return;

        PluginComponent.getOpt(listener.getPlugin(), event.getComponent()).ifPresent(menu -> {
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
