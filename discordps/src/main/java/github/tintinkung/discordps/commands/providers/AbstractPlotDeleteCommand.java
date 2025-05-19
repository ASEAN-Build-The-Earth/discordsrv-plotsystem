package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.commands.events.PlotDeleteEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.AvailableButton;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractPlotDeleteCommand extends AbstractPlotCommand<OnPlotDelete> implements PlotDeleteEvent {
    public AbstractPlotDeleteCommand(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    protected static final CommandSelectionMenu DELETE_SELECTION_MENU = payload -> SelectionMenu.create(
        AvailableButton.PLOT_DELETE_SELECTION.resolve(payload.eventID, payload.userID)
    );

    protected static final CommandButton DELETE_CONFIRM_BUTTON = payload -> Button.danger(
        AvailableButton.PLOT_DELETE_CONFIRM.resolve(payload.eventID, payload.userID), "Confirm Delete"
    );

    protected static final CommandButton DELETE_CANCEL_BUTTON = payload -> Button.secondary(
        AvailableButton.PLOT_DELETE_CANCEL.resolve(payload.eventID, payload.userID), "Cancel"
    );

    @Override
    protected abstract void onCommandTriggered(InteractionHook hook, OnPlotDelete payload);

    protected static final MessageEmbed DELETE_INFO = new EmbedBuilder()
            .setTitle("Delete Options")
            .setDescription("Found plot to delete, please choose what entry to delete.")
            .setColor(Color.ORANGE)
            .build();

    protected static final Function<String, MessageEmbed> SQL_ERROR_PLOT = error -> new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("SQL Exception Occurred!")
        .setDescription("The system failed get data for this plot ID, cannot delete the entry.")
        .addField("Error", "```" + error + "```", false)
        .build();

    protected static final MessageEmbed NOTHING_TO_DELETE = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Nothing to Delete!")
        .setDescription("There are no entries found for this plot ID.")
        .build();

    protected static final MessageEmbed ERROR_SELECTED_OPTION = new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Error Occurred!")
            .setDescription("The system failed to verify selected plot to delete.")
            .build();

    protected static final Function<String, MessageEmbed> SQL_ERROR_ENTRY = error -> new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("SQL Exception Occurred!")
            .setDescription("The system failed delete entry data.")
            .addField("Error", "```" + error + "```", false)
            .build();

    protected static final BiFunction<String, String, MessageEmbed> DELETED_INFO = (messageID, threadID) -> new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Entry Deleted")
            .setDescription("The entry from message <@" + messageID + "> at <#" + threadID + "> will no longer be tracked by the system.")
            .build();

    protected static final BiFunction<WebhookEntry, String, MessageEmbed> DELETED_NOTIFICATION = (entry, userID) -> new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("An Entry has been deleted from the database")
        .setDescription("A Manual deletion event has been triggered by <@" + userID + "> to delete a webhook entry from the database.\n")
        .appendDescription("The entry from message <@" + entry.messageID() + "> at <#" + entry.threadID() + "> will no longer be tracked by the system.")
        .build();
}