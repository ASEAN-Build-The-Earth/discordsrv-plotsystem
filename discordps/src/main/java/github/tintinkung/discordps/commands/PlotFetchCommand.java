package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnPlotFetch;
import github.tintinkung.discordps.commands.providers.AbstractPlotFetchCommand;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

class PlotFetchCommand extends AbstractPlotFetchCommand {

    public PlotFetchCommand(@NotNull String name, @NotNull String plotID, @NotNull String override) {
        super(name, "Manually create a plot tracking thread by ID");

        this.addOption(
            OptionType.INTEGER,
            plotID,
            "The plot ID in integer to be fetch",
            true);

        this.addOption(
            OptionType.BOOLEAN,
            override,
            "If false will create new thread for this plot",
            true);

        for(ThreadStatus status : ThreadStatus.VALUES) {
            this.addOption(
                OptionType.BOOLEAN,
                status.name(),
                "Apply the initial status `" + status.name() + "` to this plot on creation.",
                false);
        }
    }

    @Override
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotFetch payload) {
        if(payload.getPrimaryStatus() == null) this.queueEmbed(hook, STATUS_WARNING);
        else if(payload.getPrimaryStatus() == ThreadStatus.archived) {
            this.queueEmbed(hook, STATUS_EXCEPTION);
            return;
        }

        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(payload.getPlotID());

            // Existing entries detected
            if(!entries.isEmpty()) {
                this.queueEmbed(hook, OVERRIDE_WARNING.apply(entries.size(), payload.override));
                // Exit if we can't override the data
                if(!payload.override) return;
            }
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, SQL_ERROR_PLOT.apply(ex.toString()));
        }

        ActionRow options = ActionRow.of(
            FETCH_CONFIRM_BUTTON.apply(payload),
            FETCH_DISMISS_BUTTON.apply(payload)
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

                hook.sendMessageEmbeds(OVERRIDE_INFO)
                    .addActionRow(menu.build())
                    .addActionRow(
                        OVERRIDE_CONFIRM_BUTTON.apply(interaction),
                        OVERRIDE_CANCEL_BUTTON.apply(interaction))
                    .setEphemeral(true)
                    .queue();
                return;
            }
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, SQL_ERROR_PLOT.apply(ex.toString()));
        }

        // Plot does not exist in the database
        // Send creation options
        ActionRow interactions = ActionRow.of(
            CREATE_REGISTER_BUTTON.apply(interaction),
            CREATE_UNTRACKED_BUTTON.apply(interaction),
            CREATE_CANCEL_BUTTON.apply(interaction)
        );

        this.queueEmbed(hook, interactions, CREATE_INFO);
    }

    public void onConfirmOverride(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);

        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, ERROR_SELECTED_OPTION);
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

                DiscordPS.getPlugin().getWebhook().updatePlot(
                    action,
                    null,
                    interaction.getPrimaryStatus() == null? entry.status() : interaction.getPrimaryStatus(),
                    ignored -> this.queueEmbed(hook, FETCH_SUCCESSFUL.apply(interaction.getPlotID(), entry))
                );
            }
            catch (SQLException ex) {
                this.queueEmbed(hook, SQL_ERROR_ENTRY.apply(ex.toString()));
            }
        }
    }

    public void onCreateRegister(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        Consumer<Message> onCreatePlot = createPlot(DiscordPS.getPlugin().getWebhook()::createAndRegisterNewPlot, interaction);

        hook.sendMessageEmbeds(CREATING_MESSAGE).setEphemeral(true).queue(onCreatePlot);
    }

    public void onCreateUntracked(@NotNull InteractionHook hook, @NotNull OnPlotFetch interaction) {
        Consumer<Message> onCreatePlot = createPlot(DiscordPS.getPlugin().getWebhook()::createNewUntrackedPlot, interaction);

        hook.sendMessageEmbeds(CREATING_MESSAGE).setEphemeral(true).queue(onCreatePlot);
    }

    /**
     * Create a new plot by defined creator implementation
     * returning as an action consumer for embed message.
     *
     * @param creator The plot creator reference to call for
     * @param interaction The interaction data
     * @return An action consumer to edit message as the creation status
     */
    @Contract(pure = true)
    private @NotNull Consumer<Message> createPlot(@NotNull Function<PlotSystemThread, CompletableFuture<Void>> creator,
                                                  @NotNull OnPlotFetch interaction) {
        return  defer -> this.createPlot(
                creator,
                interaction.getPlotID(),
                interaction.getPrimaryStatus(),
                ok -> defer.editMessageEmbeds(SUCCESSFULLY_CREATED).queue(),
                failed -> defer.editMessageEmbeds(ON_CREATE_ERROR.apply(failed.toString())).queue()
        );
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
                            @NotNull Consumer<Void> onSuccess,
                            @NotNull Consumer<Throwable> onFailure) {
        PlotEntry plot = PlotEntry.getByID(plotID);

        if(plot == null) {
            onFailure.accept(PLOT_FETCH_RETURNED_NULL);
            return;
        } else if (plot.ownerUUID() == null) {
            onFailure.accept(PLOT_FETCH_UNKNOWN_OWNER);
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

        if(createAction == null) onFailure.accept(PLOT_CREATE_RETURNED_NULL);
        else createAction.whenComplete((ok, error) -> {
            if(error != null) onFailure.accept(error);
            else onSuccess.accept(ok);
        });
    }
}
