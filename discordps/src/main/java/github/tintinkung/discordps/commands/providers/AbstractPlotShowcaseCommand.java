package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.tintinkung.discordps.commands.events.PlotShowcaseEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotShowcase;
import github.tintinkung.discordps.core.system.AvailableButton;
import github.tintinkung.discordps.core.system.PlotData;
import github.tintinkung.discordps.core.system.layout.InfoComponent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractPlotShowcaseCommand extends AbstractPlotCommand<OnPlotShowcase> implements PlotShowcaseEvent {

    public AbstractPlotShowcaseCommand(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    protected static final CommandButton SHOWCASE_CONFIRM_BUTTON = payload -> Button.success(
            AvailableButton.PLOT_SHOWCASE_CONFIRM.resolve(payload.eventID, payload.userID), "Confirm"
    );

    protected static final CommandButton SHOWCASE_CANCEL_BUTTON = payload -> Button.secondary(
            AvailableButton.PLOT_SHOWCASE_CANCEL.resolve(payload.eventID, payload.userID), "Cancel"
    );

    @Override
    protected abstract void onCommandTriggered(InteractionHook hook, OnPlotShowcase payload);

    protected @NotNull MessageEmbed formatShowcaseInfo(@NotNull PlotData plotData) {

        String mediaFiles = plotData
            .getImageFiles()
            .stream()
            .map(File::getName)
            .collect(Collectors.joining(", "));

        return new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Confirm your showcase data")
            .setThumbnail(plotData.getAvatarAttachmentOrURL())
            .setDescription(":warning: After confirming, the action cannot be undo or edited.")
            .addField("Plot Title", InfoComponent.makeTitle(
                    plotData.getPlot().plotID(),
                    plotData.getPlot().cityName(),
                    plotData.getPlot().countryName()), false)
            .addField("Owner", (plotData.isOwnerHasDiscord()? plotData.getOwnerMentionOrName()
                    + " (" + plotData.getOwner().getName() + ")"
                    : plotData.formatOwnerName()), false)
            .addField("Owner Avatar", plotData.getAvatarAttachmentOrURL(), false)
            .addField("Image Files", mediaFiles, false)
            .addField("Location", plotData.getDisplayCords(), false)
            .addField("Google Map Link", "https://www.google.com/maps/place/" + plotData.getGeoCoordinates(), false)
            .build();
    }

    protected static final MessageEmbed NOTHING_TO_SHOWCASE = new EmbedBuilder()
            .setColor(Color.ORANGE)
            .setTitle("Nothing to Showcase!")
            .setDescription("There are no entries found for this plot ID.")
            .build();

    protected static final MessageEmbed PLOT_DATA_RETURNED_NULL = new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Failed to retrieve plot data")
            .setDescription(PLOT_FETCH_RETURNED_NULL.getMessage())
            .build();

    protected static final MessageEmbed PLOT_DATA_NO_OWNER = new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Plot data to showcase invalid")
            .setDescription(PLOT_FETCH_UNKNOWN_OWNER.getMessage())
            .build();

    protected static final MessageEmbed ENTRY_NOT_ARCHIVED = new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Cannot Showcase!")
            .setDescription("The plot to showcase has to be mark archived first! "
                + "run the command `/plot archive` to proceed the archival steps.")
            .build();

    protected static final MessageEmbed ON_SHOWCASE_EMBED = new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Processing Data . . .")
            .setDescription("Showcase output will be created on showcase forum soon.")
            .build();

    protected static final Function<String, MessageEmbed> ON_SHOWCASE_ERROR = error -> new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Error occurred!")
            .setDescription("Failed to showcase this plot.")
            .addField("Error", "```" + error + "```", false)
            .build();

    protected static final BiFunction<String, String, MessageEmbed> ON_SHOWCASE_COMPLETED = (userID, threadID) -> new EmbedBuilder()
            .setTitle(":sparkles: Plot Showcased!")
            .setDescription("Thread: <#" + threadID + ">\nShowcased by: <@" + userID + ">")
            .addField("Thread ID", "```" +threadID + "```", false)
            .setColor(Color.CYAN)
            .build();

    protected MessageEmbed formatSuccessfulEmbed(@NotNull MessageReference message) {
        return new EmbedBuilder()
            .setTitle("Showcased to <#" + message.getMessageId() + '>')
            .addField("Thread ID", "```" + message.getMessageId() + "```", false)
            .setColor(Color.GREEN)
            .build();
    }
}