package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.handlers.*;
import asia.buildtheearth.asean.discord.commands.interactions.InteractionEvent;
import asia.buildtheearth.asean.discord.components.buttons.*;
import asia.buildtheearth.asean.discord.plotsystem.core.system.buttons.PlotFeedback;
import asia.buildtheearth.asean.discord.plotsystem.core.system.buttons.PlotHelp;
import asia.buildtheearth.asean.discord.providers.ComponentProvider;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Available interactive buttons with each enum being the button type
 * saved via button's {@code custom_id} field.
 *
 * <p><strong>Max 42 characters</strong> for the enum name and even less if payload is provided</p>
 */
public enum AvailableButton implements ComponentProvider, AvailableButtonHandler {
    // Setup Webhook
    WEBHOOK_SUBMIT_AVATAR(SetupWebhookHandler.ON_SUBMIT_AVATAR_IMAGE),
    WEBHOOK_PROVIDED_AVATAR(SetupWebhookHandler.ON_PROVIDED_AVATAR),
    WEBHOOK_CONFIRM_CONFIG(SetupWebhookHandler.ON_CONFIRM_CONFIG),
    WEBHOOK_CANCEL_CONFIG(SetupWebhookHandler.ON_CANCEL_CONFIG),

    // Plot Archiving
    PLOT_IMAGES_ATTACHED(PlotArchiveHandler.ON_IMAGES_ATTACHED),
    PLOT_IMAGES_PROVIDED(PlotArchiveHandler.ON_IMAGES_PROVIDED),
    PLOT_ARCHIVE_DISMISS(PlotArchiveHandler.ON_ARCHIVE_DISMISS),
    PLOT_ARCHIVE_CONFIRM(PlotArchiveHandler.ON_ARCHIVE_CONFIRM),
    PLOT_ARCHIVE_CANCEL(PlotArchiveHandler.ON_ARCHIVE_CANCEL),
    PLOT_ARCHIVE_SHOWCASE_CONFIRM(PlotArchiveHandler.ON_ARCHIVE_SHOWCASE_CONFIRM),
    PLOT_ARCHIVE_SHOWCASE_DISMISS(PlotArchiveHandler.ON_ARCHIVE_SHOWCASE_DISMISS),

    // Plot Fetching
    PLOT_FETCH_SELECTION(PlotFetchHandler.ON_FETCH_SELECTION),
    PLOT_FETCH_CONFIRM(PlotFetchHandler.ON_CONFIRM_FETCH),
    PLOT_FETCH_DISMISS(PlotFetchHandler.ON_DISMISS_FETCH),
    PLOT_OVERRIDE_CONFIRM(PlotFetchHandler.ON_OVERRIDE_CONFIRM),
    PLOT_OVERRIDE_CANCEL(PlotFetchHandler.ON_OVERRIDE_CANCEL),
    PLOT_CREATE_REGISTER(PlotFetchHandler.ON_CREATE_REGISTER),
    PLOT_CREATE_UNTRACKED(PlotFetchHandler.ON_CREATE_UNTRACKED),
    PLOT_CREATE_CANCEL(PlotFetchHandler.ON_CREATE_CANCEL),

    // Plot Deletion
    PLOT_DELETE_SELECTION(PlotDeleteHandler.ON_DELETE_SELECTION),
    PLOT_DELETE_CONFIRM(PlotDeleteHandler.ON_DELETE_CONFIRM),
    PLOT_DELETE_CANCEL(PlotDeleteHandler.ON_DELETE_CANCEL),

    // Plot Showcasing
    PLOT_SHOWCASE_CONFIRM(PlotShowcaseHandler.ON_SHOWCASE_CONFIRM),
    PLOT_SHOWCASE_CANCEL(PlotShowcaseHandler.ON_SHOWCASE_DISMISS),

    // Plot-System Forum Buttons
    HELP_BUTTON(new PlotHelp()),
    FEEDBACK_BUTTON(new PlotFeedback());

    private final @Nullable PluginButtonHandler handler;

    AvailableButton(@NotNull AvailableButtonHandler fromHandler) {
        this.handler = fromHandler.getHandler();
    }

    AvailableButton(@Nullable PluginButtonHandler handler) {
        this.handler = handler;
    }

    @Override
    public @Nullable PluginButtonHandler getHandler() {
        return handler;
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return DiscordPS.getPlugin();
    }

    /**
     * Interact with this button by triggered event.
     *
     * @param button       The plugin button instance representing this interaction.
     * @param event        The {@link ButtonClickEvent} triggered by the user.
     * @param interactions Contextual data related to interaction events, if applicable.
     */
    public void interact(PluginButton button, ButtonClickEvent event, InteractionEvent interactions) {
        if(getHandler() == null) return;

        // Exit immediately for un-authorized owner
        if(!button.getUserID().equals(event.getUser().getId())) {
            getHandler().onInteractedBadOwner(event);
            return;
        }

        switch(getHandler()) {
            case InteractiveButtonHandler interactive -> interactive.onInteracted(button, event, interactions);
            case SimpleButtonHandler simple -> simple.onInteracted(button, event);
            case PluginButtonHandler ignored -> throw new IllegalStateException("Button handler has no implementation");
        }
    }

    @Override
    public @NotNull String resolve(Long eventID, Long userID) {
        return this.newComponentID(this.name(), eventID, userID);
    }

    @Override
    public <T> @NotNull String resolve(Long eventID, Long userID, T payload) {
        return this.newComponentID(this.name(), eventID, userID, payload);
    }
}