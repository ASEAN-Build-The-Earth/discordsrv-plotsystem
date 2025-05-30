package asia.buildtheearth.asean.discord.plotsystem.commands;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotEvent;
import asia.buildtheearth.asean.discord.plotsystem.api.events.PlotUndoReviewEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotFetch;
import asia.buildtheearth.asean.discord.plotsystem.commands.providers.AbstractPlotFetchCommand;
import asia.buildtheearth.asean.discord.plotsystem.core.database.PlotEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.*;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.CommandMessage;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotFetchCommand.*;
import static asia.buildtheearth.asean.discord.plotsystem.Constants.RED;
import static asia.buildtheearth.asean.discord.plotsystem.Constants.GREEN;
import static asia.buildtheearth.asean.discord.plotsystem.Constants.ORANGE;

class PlotFetchCommand extends AbstractPlotFetchCommand {

    public PlotFetchCommand(@NotNull String name, @NotNull String plotID, @NotNull String override) {
        super(name);

        this.setDescription(getLang(DESC));

        this.addOption(OptionType.INTEGER, plotID, getLang(DESC_PLOT_ID), true);

        this.addOption(OptionType.BOOLEAN, override, getLang(DESC_OVERRIDE), true);

        for(ThreadStatus status : ThreadStatus.VALUES) {
            String description = getLang(DESC_TAG).replace(Format.LABEL, status.name());
            this.addOption(OptionType.BOOLEAN, status.name(), description, false);
        }
    }

    @Override
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotFetch payload) {
        if(payload.getPrimaryStatus() == null) this.queueEmbed(hook, getEmbed(ORANGE, EMBED_STATUS_NOT_SET));
        else if(payload.getPrimaryStatus() == ThreadStatus.archived) {
            this.queueEmbed(hook, getEmbed(RED, EMBED_CANNOT_MAKE_ARCHIVE));
            return;
        }

        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(payload.getPlotID());

            // Existing entries detected
            if(!entries.isEmpty()) {
                this.queueEmbed(hook, formatOverrideWarning(
                        String.valueOf(entries.size()),
                        payload.override)
                );
                // Exit if we can't override the data
                if(!payload.override) return;
            }
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_GET_ERROR, ex.toString()));
        }

        ActionRow options = ActionRow.of(
            Button.FETCH_CONFIRM.get(payload),
            Button.FETCH_DISMISS.get(payload)
        );

        MessageEmbed embed = this.formatConfirmationEmbed(
            payload.getPlotID(),
            payload.override,
            payload.getPrimaryStatus()
        );

        this.queueEmbed(hook, options, embed);
    }

    public void onConfirmCreation(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(interaction.getPlotID());

            if(!entries.isEmpty()) {
                // Plot already exist -> override existing required -> send override options
                EntryMenuBuilder menu = new EntryMenuBuilder(FETCH_SELECTION_MENU.apply(interaction), interaction::setFetchOptions);

                entries.forEach(entry -> this.formatEntryOption(entry, menu::registerMenuOption));

                hook.sendMessageEmbeds(getEmbed(ORANGE, EMBED_OVERRIDE_OPTIONS))
                    .addActionRow(menu.build())
                    .addActionRow(
                        Button.OVERRIDE_CONFIRM.get(interaction),
                        Button.OVERRIDE_CANCEL.get(interaction))
                    .setEphemeral(true)
                    .queue();
                return;
            }
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_PLOT_ERROR, ex.toString()));
        }

        // Plot does not exist in the database
        // Send creation options
        ActionRow interactions = ActionRow.of(
            Button.CREATE_REGISTER.get(interaction),
            Button.CREATE_UNTRACKED.get(interaction),
            Button.CREATE_CANCEL.get(interaction)
        );

        this.queueEmbed(hook, interactions, getEmbed(ORANGE, EMBED_UNTRACKED_OPTIONS));
    }

    public void onConfirmOverride(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);

        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        for(SelectOption option : interaction.getFetchOptions()) {
            String selected = option.getValue();

            DiscordPS.debug("Overriding entry: " + selected);

            Checks.isSnowflake(selected);

            try {
                WebhookEntry entry = WebhookEntry.getByMessageID(Long.parseUnsignedLong(selected));

                if(entry == null) throw new SQLException("Entry does not exist for selected id: " + selected);

                PlotSystemThread.UpdateAction action = new PlotSystemThread.UpdateAction(
                    interaction.getPlotID(),
                    entry,
                    Long.toUnsignedString(entry.messageID()),
                    Long.toUnsignedString(entry.threadID())
                );

                ThreadStatus updateStatus = interaction.getPrimaryStatus() == null
                        ? entry.status()
                        : interaction.getPrimaryStatus();

                PlotEvent event = null;

                // Detect if this fetch even is an undo from reviewed plot to finished
                if(entry.status() != updateStatus) {
                    if(entry.status() == ThreadStatus.approved || entry.status() == ThreadStatus.rejected)
                        if(updateStatus == ThreadStatus.finished)
                            event = new PlotUndoReviewEvent(interaction.getPlotID());
                }

                BiConsumer<PlotSystemThread.UpdateAction, ? super Throwable> handler = (ok, error) -> {
                    if(error != null) this.queueEmbed(hook, errorEmbed(error.toString()));
                    else {
                        this.queueEmbed(hook, formatSuccessEmbed(interaction.getPlotID(), entry));
                        Notification.notify(
                            CommandMessage.PLOT_FETCH,
                            updateStatus.name(),
                            hook.getInteraction().getUser().getId(),
                            String.valueOf(interaction.plotID)
                        );
                    }
                };

                DiscordPS.getPlugin()
                    .getWebhook()
                    .updatePlot(action, event, updateStatus)
                    .whenComplete(handler);
            }
            catch (SQLException ex) {
                this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_GET_ERROR, ex.toString()));
            }
        }
    }

    public void onCreateRegister(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        this.createPlot(
            DiscordPS.getPlugin().getWebhook()::createAndRegisterNewPlot,
            hook.getInteraction().getUser().getId(),
            interaction,
            hook.sendMessageEmbeds(getEmbed(ORANGE, EMBED_ON_CREATE))
                .setEphemeral(true)
                .submit()
        );
    }

    public void onCreateUntracked(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        this.createPlot(
            DiscordPS.getPlugin().getWebhook()::createNewUntrackedPlot,
            hook.getInteraction().getUser().getId(),
            interaction,
            hook.sendMessageEmbeds(getEmbed(ORANGE, EMBED_ON_CREATE))
                .setEphemeral(true)
                .submit()
        );
    }

    /**
     * Create a new plot by defined creator implementation
     * returning as an action consumer for embed message.
     *
     * @param creator The plot creator reference to call for
     * @param userID The user that triggered this interaction for notification message
     * @param interaction The interaction data
     * @param receiver An action consumer to edit message as the creation status
     */
    @Contract(pure = true)
    private void createPlot(@NotNull Function<PlotSystemThread, CompletableFuture<Void>> creator,
                            @NotNull String userID,
                            @NotNull OnPlotFetch interaction,
                            @NotNull CompletableFuture<Message> receiver) {
        Consumer<PlotEntry> onSuccess = plot -> {
            // Edit defer embed as successful
            receiver.thenAccept(this::acceptSuccessful);

            // Notify the system
            Notification.notify(
                CommandMessage.PLOT_FETCH,
                interaction.getPrimaryStatus() != null
                    ? interaction.getPrimaryStatus().name()
                    : ThreadStatus.fromPlotStatus(plot.status()).name(),
                userID,
                String.valueOf(interaction.plotID)
            );
        };

        Consumer<Throwable> onFailure = failed -> receiver.thenAccept(
            defer -> defer.editMessageEmbeds(errorEmbed(failed.toString())).queue()
        );

        this.createPlot(creator, interaction.getPlotID(), interaction.getPrimaryStatus(), onSuccess, onFailure);
    }

    private void acceptSuccessful(@NotNull Message defer) {
        defer.editMessageEmbeds(getEmbed(GREEN, EMBED_ON_CREATE_SUCCESS)).queue();
    }

    /**
     * Create a new plot thread from plot ID,
     * practically the same as {@link PlotSystemWebhook#createAndRegisterNewPlot(int)}
     *
     * @param creator The plot creator reference to call for
     * @param plotID The plot ID to be created
     * @param status The primary status of said creating plot
     * @param onSuccess Action to do after plot successfully created, invoked with the initial message
     * @param onFailure Invoked if an error occurred
     */
    private void createPlot(@NotNull Function<PlotSystemThread, CompletableFuture<Void>> creator,
                            int plotID,
                            @Nullable ThreadStatus status,
                            @NotNull Consumer<PlotEntry> onSuccess,
                            @NotNull Consumer<Throwable> onFailure) {
        PlotEntry plot = PlotEntry.getByID(plotID);

        if(plot == null) {
            onFailure.accept(Error.PLOT_FETCH_RETURNED_NULL.get());
            return;
        } else if (plot.ownerUUID() == null) {
            onFailure.accept(Error.PLOT_FETCH_UNKNOWN_OWNER.get());
            return;
        }

        PlotSystemThread thread = new PlotSystemThread(plotID);

        thread.setProvider(data -> {
            PlotData plotData = new PlotData(plot);
            if(status != null)
                plotData.setPrimaryStatus(status, true);
            return plotData;
        });

        CompletableFuture<Void> createAction =  creator.apply(thread);

        if(createAction == null)
            onFailure.accept(Error.PLOT_CREATE_RETURNED_NULL.get());
        else
            createAction.whenComplete((ok, error) -> {
            if(error != null) onFailure.accept(error);
            else onSuccess.accept(plot);
        });
    }
}
