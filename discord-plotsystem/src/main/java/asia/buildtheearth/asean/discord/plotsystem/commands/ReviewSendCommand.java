package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnReview;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewCommand.DESC_COMMAND_SEND;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewEditCommand.*;

final class ReviewSendCommand extends ReviewEditCommand {

    public ReviewSendCommand(@NotNull String name, @NotNull String plotID) {
        super(name, plotID);
        this.setDescription(getLangManager().get(DESC_COMMAND_SEND));
    }

    @Override
    public void onSelectionConfirm(@NotNull InteractionHook hook, @NotNull OnReview interaction) {
        if(interaction.getFetchOptions() == null || interaction.getFetchOptions().isEmpty()) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        String selected = interaction.getFetchOptions().getFirst().getValue();

        try {
            Checks.isSnowflake(selected);

            WebhookEntry entry = WebhookEntry.getByMessageID(Long.parseUnsignedLong(selected));

            if(entry == null) throw new SQLException("Entry does not exist for selected id: " + selected);

            interaction.setSelectedEntry(entry);

            sendInstruction(hook, interaction);
        }
        catch (SQLException | IllegalArgumentException ex) {
            this.queueEmbed(hook, sqlErrorEmbed(MESSAGE_SQL_GET_ERROR, ex.toString()));
        }
    }

    @Override
    public void onNewMessageConfirm(@NotNull Message message, @NotNull OnReview interaction) {
        WebhookEntry entry = interaction.getSelectedEntry();
        MessageChannel channel = message.getChannel();

        if(entry == null) {
            channel.sendMessageEmbeds(errorEmbed(MESSAGE_VALIDATION_ERROR)).queue();
            return;
        }

        // Parse the new content and send it as preview
        String feedbackRaw = message.getContentRaw();

        WebhookDataBuilder.WebhookData reviewData = parseReviewContent(
            message,
            interaction.eventID,
            entry.plotID(),
            entry.status().toTag().getColor(),
            interaction::setAttachments
        );

        // Send the preview as raw rest-action to bypass outdated checks
        Route.CompiledRoute route = Route.Messages.SEND_MESSAGE.compile(channel.getId());
        MultipartBody requestBody = reviewData.prepareRequestBody();
        RestAction<Object> newReview = new RestActionImpl<>(DiscordPS.getPlugin().getJDA(), route, requestBody);

        // Send preview and handle for error
        this.sendReviewConfirmation(newReview.submit(), channel, interaction)
            .thenAccept(success -> success.setNewReviewMessage(feedbackRaw));
    }

    @Override
    public void onReviewConfirm(@NotNull InteractionHook hook, @NotNull OnReview interaction) {
        if(interaction.getSelectedEntry() == null
            || interaction.getNewReviewMessage() == null
            || StringUtils.isBlank(interaction.getNewReviewMessage())) {
            this.queueEmbed(hook, errorEmbed(MESSAGE_VALIDATION_ERROR));
            return;
        }

        interaction.getOptAttachments().ifPresentOrElse(attachments -> {
            String eventID = Long.toUnsignedString(interaction.eventID);
            this.saveAttachmentsToFile(hook, interaction, eventID, attachments, (uploaded, files) ->
                this.sendReviewInThread(hook, interaction, eventID, uploaded, files));
        }, () -> this.sendReviewInThread(hook, interaction, null, null, null));
    }

    @Override
    protected @NotNull ActionRow getSelectionRow(@NotNull OnReview payload) {
        return ActionRow.of(
                Button.SEND_SELECTION_CONFIRM.get(payload),
                Button.SELECTION_CANCEL.get(payload)
        );
    }

    @Override
    protected @NotNull ActionRow getSubmissionRow(@NotNull OnReview payload) {
        return ActionRow.of(
                Button.SEND_MESSAGE_CONFIRM.get(payload),
                Button.MESSAGE_CANCEL.get(payload)
        );
    }

    @Override
    protected @NotNull ActionRow getConfirmationRow(@NotNull OnReview payload) {
        return ActionRow.of(
                Button.SEND_CONFIRM.get(payload),
                Button.SEND_EDIT.get(payload),
                Button.EDIT_CANCEL.get(payload)
        );
    }
}
