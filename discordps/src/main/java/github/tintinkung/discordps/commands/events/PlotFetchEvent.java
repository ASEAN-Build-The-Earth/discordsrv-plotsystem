package github.tintinkung.discordps.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.tintinkung.discordps.commands.interactions.OnPlotFetch;
import github.tintinkung.discordps.core.database.ThreadStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface PlotFetchEvent {


    void onConfirmOverride(InteractionHook hook, OnPlotFetch interaction);

    void onConfirmCreation(InteractionHook hook, OnPlotFetch interaction);

    void onCreateRegister(InteractionHook hook, OnPlotFetch interaction);

    void onCreateUntracked(InteractionHook hook, OnPlotFetch interaction);


    /**
     * Special message to send when fetch status is picked more than one
     *
     * @param hook The interaction hook to reply to
     * @param pickedStatus The first picked status that will be applied to this command
     */
    static void onManyStatusPicked(@NotNull InteractionHook hook, @NotNull String pickedStatus) {
        hook.sendMessageEmbeds(new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setTitle("Picked more than one status!")
                .setDescription("The plot will be create only with the first picked status: `" + pickedStatus + "`")
                .build())
            .setEphemeral(true)
            .queue();
    }
}
