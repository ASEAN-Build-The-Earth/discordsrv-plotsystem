package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnPlotDelete;
import github.tintinkung.discordps.commands.providers.AbstractPlotDeleteCommand;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.Notification;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import static github.tintinkung.discordps.core.system.io.lang.PlotDeleteCommand.*;
import static github.tintinkung.discordps.core.system.io.lang.Notification.CommandMessage;

class PlotDeleteCommand extends AbstractPlotDeleteCommand {


    public PlotDeleteCommand(@NotNull String name, @NotNull String plotID) {
        super(name);

        this.setDescription(getLang(DESC));

        this.addOption(OptionType.INTEGER, plotID, getLang(DESC_PLOT_ID), true);
    }

    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotDelete interaction) {
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(interaction.getPlotID());

            // Existing entries detected
            if(!entries.isEmpty()) {
                // Plot already exist
                EntryMenuBuilder menu = new EntryMenuBuilder(DELETE_SELECTION_MENU.apply(interaction), interaction::setFetchOptions);

                entries.forEach(entry -> this.formatEntryOption(entry, menu::registerMenuOption));

                hook.sendMessageEmbeds(getEmbed(Constants.ORANGE, EMBED_DELETE_INFO))
                    .addActionRow(menu.build())
                    .addActionRow(
                        this.getConfirmButton(interaction),
                        this.getCancelButton(interaction))
                    .setEphemeral(true)
                    .queue();
            } // Entry does not exist, nothing to delete
            else this.queueEmbed(hook, getEmbed(Constants.ORANGE, EMBED_NOTHING_TO_DELETE));
        }
        catch (SQLException ex) {
            this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_GET_ERROR, ex.toString()));
        }
    }

    @Override
    public void onDeleteConfirm(@NotNull InteractionHook hook, @NotNull OnPlotDelete interaction) {
        DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);

        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        for(SelectOption option : interaction.getFetchOptions()) {
            String selected = option.getValue();

            DiscordPS.info("Deleting by slash command event (entry ID: " + selected + ")");

            Checks.isSnowflake(selected);

            try {
                WebhookEntry entry = WebhookEntry.getByMessageID(Long.parseUnsignedLong(selected));

                if(entry == null) throw new SQLException("Entry does not exist for selected id: " + selected);

                String threadID = Long.toUnsignedString(entry.threadID());

                WebhookEntry.deleteEntry(entry.messageID());
                Notification.notify(
                    CommandMessage.PLOT_DELETE,
                    selected,
                    threadID,
                    hook.getInteraction().getUser().getId()
                );

                this.queueEmbed(hook, getEmbed(
                    Constants.RED,
                    EMBED_DELETED_INFO,
                    selected,
                    threadID)
                );
            }
            catch (SQLException ex) {
                this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_DELETE_ERROR, ex.toString()));
            }
        }
    }
}
