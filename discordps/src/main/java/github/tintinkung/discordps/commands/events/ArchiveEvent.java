package github.tintinkung.discordps.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;

public interface ArchiveEvent {

    /**
     * Utility command for marking plot as archived.
     *
     * @param hook The interaction event
     * @param plotID The plot ID to archive
     * @param doCreate IF true will create a new thread specifically for archiving this plot
     */
    void onArchivePlot(InteractionHook hook, ActionRow options, long plotID, boolean doCreate);

    void onConfirmImagesProvided(InteractionHook hook, int plotID);

    void onConfirmImageAttached(InteractionHook hook, Message message, int plotID);
}
