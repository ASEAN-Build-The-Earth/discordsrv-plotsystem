package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;
import github.tintinkung.discordps.commands.providers.AbstractPlotDeleteCommand;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.Notification;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public class PlotDeleteCommand extends AbstractPlotDeleteCommand {


    public PlotDeleteCommand(@NotNull String name, @NotNull String plotID) {
        super(name, "Manually create a plot tracking thread by ID");

        this.addOption(
            OptionType.INTEGER,
            plotID,
            "The plot ID in integer to be fetch",
            true);
    }

    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotDelete interaction) {
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(interaction.getPlotID());

            // Existing entries detected
            if(!entries.isEmpty()) {
                // Plot already exist
                EntryMenuBuilder menu = new EntryMenuBuilder(DELETE_SELECTION_MENU.apply(interaction), interaction::setFetchOptions);

                entries.forEach(entry -> this.formatEntryOption(entry, menu::registerMenuOption));

                hook.sendMessageEmbeds(DELETE_INFO)
                    .addActionRow(menu.build())
                    .addActionRow(
                        DELETE_CONFIRM_BUTTON.apply(interaction),
                        DELETE_CANCEL_BUTTON.apply(interaction))
                    .setEphemeral(true)
                    .queue();
            } // Entry does not exist, nothing to delete
            else this.queueEmbed(hook, NOTHING_TO_DELETE);
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, SQL_ERROR_PLOT.apply(ex.toString()));
        }
    }

    @Override
    public void onDeleteConfirm(@NotNull InteractionHook hook, @NotNull OnPlotDelete interaction) {
        DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);

        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, ERROR_SELECTED_OPTION);
            return;
        }

        for(SelectOption option : interaction.getFetchOptions()) {
            String selected = option.getValue();

            DiscordPS.info("Deleting by slash command event (entry ID: " + selected + ")");

            Checks.isSnowflake(selected);

            try {
                WebhookEntry entry = WebhookEntry.getByMessageID(Long.parseUnsignedLong(selected));

                if(entry == null) throw new SQLException("Entry does not exist for selected id: " + selected);

                WebhookEntry.deleteEntry(entry.messageID());
                Notification.sendMessageEmbeds(DELETED_NOTIFICATION.apply(entry, hook.getInteraction().getUser().getId()));
                this.queueEmbed(hook, DELETED_INFO.apply(selected, Long.toUnsignedString(entry.threadID())));
            }
            catch (SQLException ex) {
                this.queueEmbed(hook, SQL_ERROR_ENTRY.apply(ex.toString()));
            }
        }
    }
}
