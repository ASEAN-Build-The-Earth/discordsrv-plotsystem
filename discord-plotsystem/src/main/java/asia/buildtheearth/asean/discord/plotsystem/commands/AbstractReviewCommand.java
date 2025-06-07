package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.ReviewEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnReview;
import asia.buildtheearth.asean.discord.plotsystem.commands.providers.PlotCommandProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableButton;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.ReviewEditCommand;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;

abstract sealed class AbstractReviewCommand
        extends PlotCommandProvider<OnReview, ReviewEditCommand>
        implements ReviewEvent
        permits asia.buildtheearth.asean.discord.plotsystem.commands.ReviewEditCommand {

    public AbstractReviewCommand(@NotNull String name) {
        super(name);
    }

    /**
     * Trigger review command.
     * <p>Currently have 2 possible outcome:<ul>
     *     <li>Trigger {@link asia.buildtheearth.asean.discord.plotsystem.commands.ReviewEditCommand Edit} Command</li>
     *     <li>Trigger {@link asia.buildtheearth.asean.discord.plotsystem.commands.ReviewSendCommand Send} Command</li>
     * </ul></p>
     * <p>{@link ReviewArchiveCommand} is triggered separately using all {@link PlotArchiveCommand} implementations.</p>
     *
     * @param hook The triggered command interaction hook
     * @param payload The payload to start this command events
     */
    @Override
    protected abstract void onCommandTriggered(InteractionHook hook, OnReview payload);

    /**
     * Initial selection menu for selection available entry to review.
     *
     * @see AvailableButton#REVIEW_EDIT_SELECTION
     */
    protected static final CommandSelectionMenu EDIT_SELECTION_MENU = payload -> SelectionMenu.create(
        AvailableButton.REVIEW_EDIT_SELECTION.resolve(payload.eventID, payload.userID)
    );

    /**
     * Selection row to confirm the selected entry to review.
     *
     * @param payload The interaction payload
     * @return An action row
     */
    protected abstract @NotNull ActionRow getSelectionRow(@NotNull OnReview payload);

    /**
     * Confirmation row that listen to new review message.
     *
     * @param payload The interaction payload
     * @return An action row
     */
    protected abstract @NotNull ActionRow getSubmissionRow(@NotNull OnReview payload);

    /**
     * Final confirmation row that will finalize all data.
     *
     * @param payload The interaction payload
     * @return An action row
     */
    protected abstract @NotNull ActionRow getConfirmationRow(@NotNull OnReview payload);

    /**
     * Create a file deletion consumer with an interaction hook, sending info message to that hook.
     *
     * @param hook Hook to send deleted information to
     * @return A consumer to be used in {@link java.util.List#forEach(Consumer)}
     */
    protected @NotNull Consumer<File> deleteMedia(@NotNull InteractionHook hook) {
        return file -> {
            if (file.delete()) hook.sendMessage(
                getLang(ReviewEditCommand.MESSAGE_PREV_MEDIA_DELETED)
                    .replace(Format.FILENAME, file.getName())
            ).setEphemeral(true).queue();
        };
    }

    /**
     * Delete a specify file from the plugin, logging info in debug scope.
     *
     * @param file The file to delete
     */
    protected void deleteMedia(@NotNull File file) {
        if (file.delete()) DiscordPS.debug("Cleared temporary review media: " + file.getName());
    }

    protected enum Button {
        SELECTION_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.REVIEW_SELECT_CONFIRM, CommandInteractions.BUTTON_CONFIRM),
        MESSAGE_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.REVIEW_MESSAGE_CONFIRM, CommandInteractions.BUTTON_CONFIRM),
        EDIT_EDIT(ButtonStyle.PRIMARY, AvailableButton.REVIEW_EDIT_EDIT, CommandInteractions.BUTTON_EDIT),
        EDIT_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.REVIEW_EDIT_CONFIRM, CommandInteractions.BUTTON_CONFIRM),

        SELECTION_CANCEL(ButtonStyle.SECONDARY, AvailableButton.REVIEW_SELECT_CANCEL, CommandInteractions.BUTTON_CANCEL),
        MESSAGE_CANCEL(ButtonStyle.SECONDARY, AvailableButton.REVIEW_MESSAGE_CANCEL, CommandInteractions.BUTTON_CANCEL),
        EDIT_CANCEL(ButtonStyle.SECONDARY, AvailableButton.REVIEW_EDIT_CANCEL, CommandInteractions.BUTTON_CANCEL),

        PREV_IMG_CLEAR(ButtonStyle.DANGER, AvailableButton.REVIEW_PREV_IMG_CLEAR, CommandInteractions.BUTTON_CLEAR),
        PREV_IMG_ADD(ButtonStyle.PRIMARY, AvailableButton.REVIEW_PREV_IMG_ADD, CommandInteractions.BUTTON_ADD),

        PUBLIC_CANCEL(ButtonStyle.SECONDARY, AvailableButton.REVIEW_PUBLIC_CANCEL, CommandInteractions.BUTTON_CANCEL),
        PUBLIC_CONTINUE(ButtonStyle.SUCCESS, AvailableButton.REVIEW_PUBLIC_CONTINUE, CommandInteractions.BUTTON_CONTINUE),

        SEND_SELECTION_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.REVIEW_SEND_SELECTION, CommandInteractions.BUTTON_CONFIRM),
        SEND_MESSAGE_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.REVIEW_MESSAGE_SEND, CommandInteractions.BUTTON_CONFIRM),
        SEND_EDIT(ButtonStyle.PRIMARY, AvailableButton.REVIEW_SEND_EDIT, CommandInteractions.BUTTON_EDIT),
        SEND_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.REVIEW_SEND_CONFIRM, CommandInteractions.BUTTON_CONFIRM),
        ;

        private final CommandButton button;

        Button(ButtonStyle style, AvailableButton type, CommandInteractions label) {
            this.button = payload -> github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button.of(
                    style, type.resolve(payload.eventID, payload.userID), DiscordPS.getSystemLang().get(label)
            );
        }

        public Component get(OnReview interaction) {
            return button.apply(interaction);
        }
    }
}
