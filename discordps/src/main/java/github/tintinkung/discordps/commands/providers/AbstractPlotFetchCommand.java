package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.commands.events.PlotFetchEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotFetch;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.AvailableButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractPlotFetchCommand extends AbstractPlotCommand<OnPlotFetch> implements PlotFetchEvent {

    public AbstractPlotFetchCommand(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    @Override
    public abstract void onCommandTriggered(InteractionHook hook, OnPlotFetch payload);

    protected final @NotNull MessageEmbed formatConfirmationEmbed(long plotID, boolean override, @Nullable ThreadStatus status) {
        return new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Confirm Fetch")
            .addField("Plot ID",  "```" + plotID + "```", true)
            .addField("Override Existing",  "```" + override + "```", true)
            .addField("Primary Status",  "```" + (status == null? "null" : status.name()) + "```", true)
            .build();
    }

    // Interactive Components

    protected static final CommandSelectionMenu FETCH_SELECTION_MENU = payload -> SelectionMenu.create(
        AvailableButton.PLOT_FETCH_SELECTION.resolve(payload.eventID, payload.userID)
    );

    protected static final CommandButton FETCH_CONFIRM_BUTTON = payload -> Button.success(
        AvailableButton.PLOT_FETCH_CONFIRM.resolve(payload.eventID, payload.userID), "Confirm"
    );

    protected static final CommandButton FETCH_DISMISS_BUTTON = payload -> Button.secondary(
        AvailableButton.PLOT_FETCH_DISMISS.resolve(payload.eventID, payload.userID), "Dismiss"
    );

    protected static final CommandButton OVERRIDE_CONFIRM_BUTTON = payload -> Button.success(
        AvailableButton.PLOT_OVERRIDE_CONFIRM.resolve(payload.eventID, payload.userID), "Confirm"
    );

    protected static final CommandButton OVERRIDE_CANCEL_BUTTON = payload -> Button.secondary(
        AvailableButton.PLOT_OVERRIDE_CANCEL.resolve(payload.eventID, payload.userID), "Cancel"
    );

    protected static final CommandButton CREATE_REGISTER_BUTTON = payload -> Button.success(
        AvailableButton.PLOT_CREATE_REGISTER.resolve(payload.eventID, payload.userID), "Create & Register"
    );

    protected static final CommandButton CREATE_UNTRACKED_BUTTON = payload -> Button.primary(
        AvailableButton.PLOT_CREATE_UNTRACKED.resolve(payload.eventID, payload.userID), "Create & Make un-tracked"
    );

    protected static final CommandButton CREATE_CANCEL_BUTTON = payload -> Button.secondary(
        AvailableButton.PLOT_CREATE_CANCEL.resolve(payload.eventID, payload.userID), "Cancel"
    );

    // Embed Messages

    protected static final MessageEmbed STATUS_EXCEPTION = new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Cannot fetch plot as archived")
            .setDescription("Use the command `/plot archive` to archive a plot instead.")
            .build();

    protected static final MessageEmbed STATUS_WARNING = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Status not set")
        .setDescription("Confirming this will fetch the current plot-system status of this plot ID as the primary status.")
        .build();

    protected static final BiFunction<Integer, Boolean, MessageEmbed> OVERRIDE_WARNING = (entries, override) -> new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Plot already registered in the database")
        .setDescription("Found " + entries + " entries of this plot ID in the database. "
            + (override? "Confirming this will override the existing thread."
            : "To proceed, please use this command again with override set to `true`.")
        ).build();

    protected static final Function<String, MessageEmbed> SQL_ERROR_PLOT = error -> new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("SQL Exception Occurred!")
        .setDescription("The system failed to verify if the plot ID exist in the database or not, proceed with cautions.")
        .addField("Error", "```" + error + "```", false)
        .build();

    protected static final Function<String, MessageEmbed> SQL_ERROR_ENTRY = error -> new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("SQL Exception Occurred!")
        .setDescription("The system failed to get entry data.")
        .addField("Error", "```" + error + "```", false)
        .build();

    protected static final MessageEmbed ERROR_SELECTED_OPTION = new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("Error Occurred!")
        .setDescription("The system failed to verify selected plot to override.")
        .build();

    protected static final BiFunction<Integer, WebhookEntry, MessageEmbed> FETCH_SUCCESSFUL = (plotID, entry) -> new EmbedBuilder()
        .setColor(Color.GREEN)
        .setTitle("Fetched Plot#" + plotID
            + " at <#" + Long.toUnsignedString(entry.threadID()) + '>'
            + (entry.ownerID() == null? "" : " for <@" + entry.ownerID() + '>'))
        .build();

    protected static final MessageEmbed OVERRIDE_INFO = new EmbedBuilder()
        .setTitle("Override Options")
        .setDescription("This plot has an existing entry which will be overwritten!")
        .setColor(Color.ORANGE)
        .build();

    protected static final MessageEmbed CREATE_INFO = new EmbedBuilder()
        .setTitle("Plot not tracked!")
        .setDescription("This plot is not registered in the database, "
            + "do you want to register it or make it an un-tracked thread?"
        ).setColor(Color.ORANGE)
        .build();

    protected static final MessageEmbed CREATING_MESSAGE = new EmbedBuilder()
        .setTitle("Creating Plot . . .")
        .setColor(Color.ORANGE)
        .build();

    protected static final MessageEmbed SUCCESSFULLY_CREATED = new EmbedBuilder()
        .setTitle("Successfully created the plot!")
        .setColor(Color.GREEN)
        .build();

    protected static final Function<String, MessageEmbed> ON_CREATE_ERROR = error -> new EmbedBuilder()
        .setTitle("Error occurred!")
        .addField("Error:", "```" + error + "```", false)
        .setColor(Color.RED)
        .build();
}
