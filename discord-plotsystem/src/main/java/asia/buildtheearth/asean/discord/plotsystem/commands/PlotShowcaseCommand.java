package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.api.events.NotificationType;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.WebhookMessageAction;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotShowcase;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.CommandMessage;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotShowcaseCommand.*;
import static asia.buildtheearth.asean.discord.plotsystem.Constants.ORANGE;

final class PlotShowcaseCommand extends AbstractPlotShowcaseCommand {

    public PlotShowcaseCommand(@NotNull String name, @NotNull String plotID) {
        super(name);

        this.setDescription(getLang(DESC));

        this.addOption(OptionType.INTEGER, plotID, getLang(DESC_PLOT_ID), true);
    }

    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotShowcase interaction) {

        WebhookEntry.ifPlotExisted(interaction.getPlotID()).ifPresentOrElse(entry -> {
            if(entry.status() != ThreadStatus.archived) {
                this.queueEmbed(hook, errorEmbed(getEmbed(EMBED_PLOT_NOT_ARCHIVED)));
                return;
            }

            // Fetch plot information from plot-system provider
            PlotCreateData plot;

            try {
                plot = DiscordPlotSystemAPI.getDataProvider().getData(interaction.getPlotID());

                if(plot == null) {
                    this.queueEmbed(hook, errorEmbed(
                        MESSAGE_DATA_RETURNED_NULL,
                        Error.PLOT_FETCH_RETURNED_NULL.getMessage())
                    );
                    return;
                } else if (plot.ownerUUID() == null) {
                    this.queueEmbed(hook, errorEmbed(
                        MESSAGE_DATA_RETURNED_NULL,
                        Error.PLOT_FETCH_UNKNOWN_OWNER.getMessage())
                    );
                    return;
                }
            }
            catch (IllegalArgumentException ex) {
                this.queueEmbed(hook, errorEmbed(
                    MESSAGE_DATA_RETURNED_NULL,
                    Error.PLOT_FETCH_RETURNED_NULL.getMessage())
                );
                return;
            }

            PlotData plotData = new PlotData(plot);

            interaction.setPlotInfo(plotData, entry);

            ActionRow interactions = ActionRow.of(
                this.getConfirmButton(interaction),
                this.getCancelButton(interaction)
            );

            final WebhookMessageAction<Message> action = hook
                .sendMessageEmbeds(this.formatShowcaseInfo(plotData))
                .setEphemeral(true)
                .addActionRows(interactions);

            if(plotData.getAvatarFile().isPresent())
                action.addFile(plotData.getAvatarFile().get()).queue();
            else action.queue();

        }, () -> this.queueEmbed(hook, getEmbed(ORANGE, EMBED_NOTHING_TO_SHOWCASE)));
    }

    @Override
    public void onShowcaseConfirm(@NotNull InteractionHook hook, @NotNull OnPlotShowcase payload) {
        DiscordPS.getPlugin().exitSlashCommand(payload.eventID);

        if(payload.getPlotData() == null
        || payload.getPlotEntry() == null
        || DiscordPS.getPlugin().getShowcase() == null) {
            this.queueEmbed(hook, errorEmbed(
                MESSAGE_DATA_RETURNED_NULL,
                Error.PLOT_FETCH_RETURNED_NULL.getMessage())
            );
            return;
        }

        CompletableFuture<Optional<MessageReference>> showcaseAction = DiscordPS.getPlugin()
                .getShowcase()
                .showcasePlot(payload.getPlotData(), payload.getPlotEntry());

        Consumer<Message> onShowcase = defer -> showcaseAction.whenComplete((optMessage, error) -> {
            if(error != null) defer.editMessageEmbeds(errorEmbed(MESSAGE_SHOWCASE_FAILED, error.toString())).queue();
            else optMessage.ifPresentOrElse(
                message -> {
                    // Response to the user and notify the plot owner
                    defer.editMessageEmbeds(this.formatSuccessfulEmbed(message)).queue();
                    DiscordPS.getPlugin().getWebhook().onNotification(NotificationType.ON_SHOWCASED, payload.getPlotID(), notification -> {
                        final String ownerMention = payload.getPlotData().getOwnerMentionOrName();
                        String threadID = Long.toUnsignedString(payload.getPlotEntry().threadID());

                        DiscordPS.getPlugin().getWebhook().sendNotification(notification, message.getMessageId(), threadID, ownerMention);

                        Notification.notify(CommandMessage.PLOT_SHOWCASE, hook.getInteraction().getUser().getId(), message.getMessageId());
                    });
                },
                () -> defer.editMessageEmbeds(errorEmbed(MESSAGE_SHOWCASE_FAILED,
                    "Requested API call returned empty")).queue()
            );
        });

        hook.sendMessageEmbeds(getEmbed(ORANGE, EMBED_ON_SHOWCASE)).setEphemeral(true).queue(onShowcase);
    }
}
