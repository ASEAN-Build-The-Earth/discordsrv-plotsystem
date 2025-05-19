package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.WebhookMessageAction;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnPlotShowcase;
import github.tintinkung.discordps.commands.providers.AbstractPlotShowcaseCommand;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.Notification;
import github.tintinkung.discordps.core.system.PlotData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PlotShowcaseCommand extends AbstractPlotShowcaseCommand {

    public PlotShowcaseCommand(@NotNull String name, @NotNull String plotID) {
        super(name, "Showcase a completed plot by ID");

        this.addOption(
            OptionType.INTEGER,
            plotID,
            "The plot ID in integer to showcase",
            true);
    }

    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnPlotShowcase interaction) {

        WebhookEntry.ifPlotExisted(interaction.getPlotID()).ifPresentOrElse(entry -> {
//            if(entry.status() != ThreadStatus.archived) {
//                this.queueEmbed(hook, ENTRY_NOT_ARCHIVED);
//                return;
//            }

            // Fetch plot information from plot-system database
            PlotEntry plot = PlotEntry.getByID(interaction.getPlotID());

            if(plot == null) {
                this.queueEmbed(hook, PLOT_DATA_RETURNED_NULL);
                return;
            } else if (plot.ownerUUID() == null) {
                this.queueEmbed(hook, PLOT_DATA_NO_OWNER);
                return;
            }

            PlotData plotData = new PlotData(plot);

            interaction.setPlotInfo(plotData, entry);

            ActionRow interactions = ActionRow.of(
                    SHOWCASE_CONFIRM_BUTTON.apply(interaction),
                    SHOWCASE_CANCEL_BUTTON.apply(interaction)
            );

            final WebhookMessageAction<Message> action = hook
                .sendMessageEmbeds(this.formatShowcaseInfo(plotData))
                .setEphemeral(true)
                .addActionRows(interactions);

            if(plotData.getAvatarFile().isPresent())
                action.addFile(plotData.getAvatarFile().get()).queue();
            else action.queue();

        }, () -> this.queueEmbed(hook, NOTHING_TO_SHOWCASE));
    }

    @Override
    public void onShowcaseConfirm(@NotNull InteractionHook hook, @NotNull OnPlotShowcase payload) {
        DiscordPS.getPlugin().exitSlashCommand(payload.eventID);

        if(payload.getPlotData() == null
        || payload.getPlotEntry() == null
        || DiscordPS.getPlugin().getShowcase() == null) {
            this.queueEmbed(hook, PLOT_DATA_RETURNED_NULL);
            return;
        }

        CompletableFuture<Optional<MessageReference>> showcaseAction = DiscordPS.getPlugin()
                .getShowcase()
                .showcasePlot(payload.getPlotData(), payload.getPlotEntry());

        Consumer<Message> onShowcase = defer -> showcaseAction.whenComplete((optMessage, error) -> {
            if(error != null) defer.editMessageEmbeds(ON_SHOWCASE_ERROR.apply(error.toString())).queue();
            else optMessage.ifPresentOrElse(
                message -> handleSuccessful(hook, defer, message),
                () -> defer.editMessageEmbeds(ON_SHOWCASE_ERROR.apply("Requested API call returned empty")).queue()
            );
        });

        hook.sendMessageEmbeds(ON_SHOWCASE_EMBED).setEphemeral(true).queue(onShowcase);
    }

    /**
     * Response to successful showcase event and send info to notification channel.
     *
     * @param hook The interaction hook
     * @param defer Defer processing message to be updated as successful
     * @param message The showcased message
     */
    private void handleSuccessful(@NotNull InteractionHook hook,
                                  @NotNull Message defer,
                                  MessageReference message) {

        defer.editMessageEmbeds(this.formatSuccessfulEmbed(message)).queue();

        Notification.sendMessageEmbeds(ON_SHOWCASE_COMPLETED.apply(
            hook.getInteraction().getUser().getId(),
            message.getMessageId())
        );
    }
}
