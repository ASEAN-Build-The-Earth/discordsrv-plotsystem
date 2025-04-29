package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.ArchiveEvent;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.system.AvailableTags;
import github.tintinkung.discordps.core.system.Notification;
import github.tintinkung.discordps.core.system.PlotData;
import github.tintinkung.discordps.core.system.layout.InfoComponent;
import github.tintinkung.discordps.core.system.layout.StatusComponent;

import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static github.tintinkung.discordps.Constants.PLOT_IMAGE_FILE;

public final class ArchiveCommand extends CommandData implements ArchiveEvent {

    public static final String ARCHIVE = "archive";

    // ::/archive plot <id> <create>
    public static final String PLOT = "plot";
    public static final String CREATE = "create";

    public ArchiveCommand() {
        super(ARCHIVE, "Archive a plot");

        this.addOption(
            OptionType.INTEGER,
            PLOT,
            "The plot ID in integer to be archived",
            true);

        this.addOption(
                OptionType.BOOLEAN,
                CREATE,
                "If true will create new thread for this archival plot",
                true);
    }

    public void onArchivePlot(InteractionHook hook, ActionRow options, long plotID, boolean doCreate) {

        MessageEmbed imageNotifyEmbed = new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Plot images is not set!")
                .setDescription("Please choose a method to apply plot images.")
                .addField(
                        ":blue_heart: Attach an image",
                        "Please send a message with image attachments below, and click the confirm button to apply.",
                        false)
                .addField(
                        ":grey_heart: Provide an image",
                        "Please upload an image named `" + PLOT_IMAGE_FILE + "-XX` in your plugin data folder (`"
                                + DiscordPS.getPlugin().getDataFolder().getAbsolutePath() + "/media/plot-" + plotID + "`)",
                        false)
                .build();

        hook.sendMessageEmbeds(imageNotifyEmbed)
            .addActionRows(options)
            .queue();
    }

    public void onConfirmImagesProvided(InteractionHook hook, int plotID) {
        hook.sendMessageEmbeds(new EmbedBuilder()
            .setTitle("Archiving Plot . . .")
            .setColor(Color.ORANGE)
            .build())
            .queue(sent -> {
                this.archivePlot(plotID, msg -> {
                    sent.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("Successfully archived the plot!")
                        .setColor(Color.GREEN)
                        .build())
                        .queue();
                }, failed -> {
                    sent.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("Error occurred!")
                        .addField("Error:", "```" + failed.toString() + "```", false)
                        .setColor(Color.RED)
                        .build())
                        .queue();
                });
            });
    }

    public void onConfirmImageAttached(InteractionHook hook, Message message, int plotID) {
        List<Message.Attachment> attachment = message.getAttachments();

        StringBuilder uploaded = new StringBuilder();

        for (int i = 0; i < attachment.size(); i++) {
            Message.Attachment file = attachment.get(i);

            String fileName =  PLOT_IMAGE_FILE + "-" + i + "." + file.getFileExtension();

            String filePath = String.join("/",
                    DiscordPS.getPlugin().getDataFolder().getAbsolutePath(),
                    "media", "plot-" + plotID,
                    fileName);

            try {
                File downloaded = file.downloadToFile(filePath).get();
                uploaded.append("Uploaded ").append(downloaded.getName()).append("\n");
            } catch (InterruptedException | IllegalArgumentException | ExecutionException ex) {
                DiscordPS.error("cannot download file to resource", ex);
                DiscordPS.error("please download the file: " + file.getFileName());
                DiscordPS.error("and upload it into plugin data folder: " + DiscordPS.getPlugin().getDataFolder().getAbsolutePath());
            }
        }

        hook.sendMessageEmbeds(new EmbedBuilder()
            .setTitle("Uploading Attachment . . .")
            .setDescription(uploaded.toString())
            .setColor(Color.ORANGE)
            .build())
            .queue(sent -> {
                this.archivePlot(plotID, msg -> {
                    sent.editMessageEmbeds(new EmbedBuilder()
                            .setTitle("Successfully archived the plot!")
                            .setDescription(uploaded.toString())
                            .setColor(Color.GREEN)
                            .build())
                        .queue();
                }, failed -> {
                    sent.editMessageEmbeds(new EmbedBuilder()
                            .setTitle("Error occurred!")
                            .addField("Error:", "```" + failed.toString() + "```", false)
                            .setColor(Color.RED)
                            .build())
                        .queue();
                });
            });
    }

    private void archivePlot(int plotID, Consumer<MessageReference> onSuccess, Consumer<Throwable> onFailure) {
        PlotEntry plot = PlotEntry.getByID(plotID);

        if(plot == null) {
            DiscordPS.warning("Failed to fetch plot data from plot-system database!");
            Notification.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle(":red_circle: Discord Plot-System Error")
                    .setDescription("Runtime exception creating new plot thread, "
                            + "The plot ID #`" + plotID + "` will not be tracked by the system.")
                    .setColor(Color.RED)
                    .build()
            );
            return;
        }

        // Prepare plot data
        PlotData plotData = new PlotData(plot);


        InfoComponent infoComponent = new InfoComponent(0, plotData);
        StatusComponent statusComponent = new StatusComponent(1, plotData);

        // Initial history, more will be added dynamically via event type
        infoComponent.addHistory("<t:" + Instant.now().getEpochSecond() + "> • <@481786697860775937> Archived the plot");
        infoComponent.setAccentColor(AvailableTags.ARCHIVED.getColor());
        statusComponent.setAccentColor(AvailableTags.ARCHIVED.getColor());
        statusComponent.setStatusMessage("""
            ## Built by {owner}\

            this plot is **archived**.\

            Builder can still visit this plot or continue improving it on our master server.""");

        // Create Webhook Data
        String threadName = "Plot #" + plot.plotID();

        DiscordPS.getPlugin().getWebhook()
                .createNewPlotThread(threadName, plotData, List.of(infoComponent.build(), statusComponent.build()),
                (opt) -> opt.ifPresent(onSuccess), onFailure);
    }
}
