package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.PlotArchiveEvent;
import github.tintinkung.discordps.commands.interactions.OnPlotArchive;
import github.tintinkung.discordps.core.system.AvailableButton;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


public abstract class AbstractPlotArchiveCommand extends AbstractPlotCommand<OnPlotArchive> implements PlotArchiveEvent {

    public AbstractPlotArchiveCommand(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    @Override
    public abstract void onCommandTriggered(InteractionHook hook, OnPlotArchive payload);

    @Override
    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).setEphemeral(false).queue();
    }

    @Override
    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull ActionRow interactions, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).addActionRows(interactions).setEphemeral(false).queue();
    }

    protected void queueImagesOption(@NotNull InteractionHook hook,
                                     @NotNull OnPlotArchive payload) {
        ActionRow options = ActionRow.of(
            ATTACH_IMAGES_BUTTON.apply(payload),
            PROVIDE_IMAGES_BUTTON.apply(payload),
            ARCHIVE_DISMISS_BUTTON.apply(payload)
        );
        this.queueImagesOption(hook, options, payload, IMAGES_OPTION_EMBED);
    }

    protected void queueImagesOption(@NotNull InteractionHook hook,
                                     @NotNull ActionRow interactions,
                                     @NotNull OnPlotArchive payload,
                                     @NotNull BiFunction<String, Long, MessageEmbed> embed) {
        this.queueEmbed(
            hook,
            interactions,
            embed.apply(DiscordPS.getPlugin().getDataFolder().getAbsolutePath(), payload.plotID)
        );
    }

    protected static final CommandButton ATTACH_IMAGES_BUTTON = payload -> Button.primary(
        AvailableButton.PLOT_IMAGES_ATTACHED.resolve(payload.eventID, payload.userID), "Attach Image"
    );

    protected static final CommandButton PROVIDE_IMAGES_BUTTON = payload -> Button.secondary(
        AvailableButton.PLOT_IMAGES_PROVIDED.resolve(payload.eventID, payload.userID), "Already Provided"
    );

    protected static final CommandButton ARCHIVE_DISMISS_BUTTON = payload -> Button.danger(
        AvailableButton.PLOT_ARCHIVE_DISMISS.resolve(payload.eventID, payload.userID), "Dismiss"
    );

    protected static final CommandButton ARCHIVE_CONFIRM_BUTTON = payload -> Button.success(
            AvailableButton.PLOT_ARCHIVE_CONFIRM.resolve(payload.eventID, payload.userID), "Confirm"
    );

    protected static final CommandButton ARCHIVE_NOW_BUTTON = payload -> Button.success(
            AvailableButton.PLOT_ARCHIVE_CONFIRM.resolve(payload.eventID, payload.userID), "Archive Now"
    );

    protected static final CommandButton ARCHIVE_CANCEL_BUTTON = payload -> Button.danger(
            AvailableButton.PLOT_ARCHIVE_CANCEL.resolve(payload.eventID, payload.userID), "Cancel"
    );

    protected static final CommandButton SHOWCASE_PLOT_BUTTON = payload -> Button.success(
            AvailableButton.PLOT_ARCHIVE_SHOWCASE_CONFIRM.resolve(payload.eventID, payload.userID), "Showcase Now"
    );

    protected static final CommandButton SHOWCASE_DISMISS_BUTTON = payload -> Button.danger(
            AvailableButton.PLOT_ARCHIVE_SHOWCASE_DISMISS.resolve(payload.eventID, payload.userID), "Dismiss"
    );

    protected static final BiFunction<String, Long, MessageEmbed> IMAGES_OPTION_EMBED = (location, plotID) -> new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Plot images is not set!")
        .setDescription(
            "For archival we require some completed image to showcase this plot, "
            + "Please choose a method to apply plot images.")
        .addField(
            ":blue_heart: Attach an image",
            "Please send a message with image attachments below, and click the confirm button to apply.",
            false)
        .addField(
            ":grey_heart: Provide an image", "Please upload image(s) named `"
            + Constants.PLOT_IMAGE_FILE + "-XX` in your plugin data folder:```"
            + location + "/media/plot-" + plotID + "```",
        false)
        .build();

    protected static final BiFunction<String, Long, MessageEmbed> IMAGES_EXIST_EMBED = (location, plotID) -> new EmbedBuilder()
            .setColor(Color.ORANGE)
            .setTitle("There are already image attached to this plot")
            .setDescription(
                    "You can archive this plot right away or attach more images into it, "
                    + "archiving now will edit the existing thread status as archived.")
            .addField(
                    ":blue_heart: Attach an image",
                    "Please send a message with image attachments below, and click the confirm button to apply.",
                    false)
            .addField(
                    ":grey_heart: Provide an image", "Please upload image(s) named `"
                            + Constants.PLOT_IMAGE_FILE + "-XX` in your plugin data folder:```"
                            + location + "/media/plot-" + plotID + "```",
                    false)
            .build();

    protected static final BiFunction<String, Long, MessageEmbed> ARCHIVE_SETTING_EMBED = (threadID, plotID) -> new EmbedBuilder()
        .setColor(Color.GREEN)
        .setTitle("Found tracking thread <#" + threadID + "> to archive")
        .setDescription("Please make sure this thread exist and it is the thread for plot ID `" + plotID + "`")
        .build();

    protected static final MessageEmbed NOTHING_TO_ARCHIVE = new EmbedBuilder()
        .setColor(Color.ORANGE)
        .setTitle("Nothing to Archive!")
        .setDescription("There are no entries found for this plot ID.")
        .build();

    protected static final MessageEmbed ARCHIVE_FAILED_OPTION = new EmbedBuilder()
            .setTitle("Error occurred!")
            .setDescription("Internal error occurred while processing archive data, "
                    + "this should never happen, please report a bug.")
            .setColor(Color.ORANGE)
            .build();

    protected static final MessageEmbed UPLOADING_ATTACHMENT = new EmbedBuilder()
        .setTitle("Saving Attachment . . .")
        .setColor(Color.ORANGE)
        .build();

    protected static final MessageEmbed ARCHIVING_PLOT = new EmbedBuilder()
            .setTitle("Archiving Plot . . .")
            .setColor(Color.ORANGE)
            .build();

    protected static final BiConsumer<Message, String> ON_IMAGE_DOWNLOAD_FAILED = (message, error) ->
        message.editMessageEmbeds(new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Failed to save attachment images!")
            .setDescription(
                "The system failed save your image attachments to server resource, "
                + "please try again or provide it manually instead.")
            .addField("Error", "```" + error + "```", false)
            .build())
        .queue();

    protected static final Function<String, MessageEmbed> ON_LAYOUT_FAILED = error -> new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Error Occurred!")
            .setDescription("The system failed to restore this plot's thread layout data, "
                    + "maybe it is created from an outdated version of this plugin. "
                    + "You can try using this command again with **override** set to `true`"
                    + " to create a new thread for archiving this plot.")
            .addField("Error", "```" + error + "```", false)
            .build();

    protected static final MessageEmbed SHOWCASE_OPTION_EMBED = new EmbedBuilder()
            .setTitle("Showcase this plot")
            .setDescription("The plugin has showcase webhook configured! continue to showcase this plot?")
            .setColor(Color.GREEN)
            .build();

    protected static final MessageEmbed MEDIA_FOLDER_CREATED = new EmbedBuilder()
            .setTitle(":open_file_folder: Created media folder to archive plot")
            .setColor(Color.CYAN)
            .build();

    protected static final Function<String, String> DELETED_CACHED_FILE_INFO = filename -> "Deleted pending cached file (\"" + filename + "\") to archive plot images";

    protected static final Function<MessageChannel, Consumer<File>> CACHED_FILE_DELETER = channel -> file -> {
        if (file.delete()) channel.sendMessage(DELETED_CACHED_FILE_INFO.apply(file.getName())).queue();
    };

    protected static final Function<String, MessageEmbed> CACHED_FILE_PENDING = files -> new EmbedBuilder()
            .setTitle(":warning: Detected cached image files that is not attached to the plot layout")
            .setDescription("Attaching new image files will delete this cached files, make sure these file is not important.")
            .addField("Detected Files", "```" + files + "```", false)
            .setColor(Color.ORANGE)
            .build();

    protected static final Function<String, MessageEmbed> MEDIA_FOLDER_FAILED = location -> new EmbedBuilder()
            .setTitle(":warning: Failed to create plot media folder")
            .setDescription("Images may not be save correctly, proceed with cautions.")
            .addField("Please create the folder manually at:", "```" + location + "```", false)
            .setColor(Color.RED)
            .build();

    protected static final MessageEmbed SUCCESSFULLY_CREATED = new EmbedBuilder()
            .setTitle("Successfully archived the plot!")
            .setColor(Color.GREEN)
            .build();

    protected static final MessageEmbed ON_SHOWCASE_ERROR = new EmbedBuilder()
            .setTitle("Error occurred!")
            .setDescription("Internal error occurred triggering showcase command, "
                + "please contact developer for more info.")
            .setColor(Color.RED)
            .build();

    protected static final Function<String, MessageEmbed> ON_CREATE_ERROR = error -> new EmbedBuilder()
            .setTitle("Error occurred!")
            .addField("Error:", "```" + error + "```", false)
            .setColor(Color.RED)
            .build();

    protected static final BiFunction<Integer, String, MessageEmbed> ON_IMAGES_SAVED = (saved, log) -> new EmbedBuilder()
        .setColor(Color.GREEN)
        .setTitle("Saved " + saved + " images")
        .setDescription(log)
        .build();

    protected static final Function<Boolean, MessageEmbed> ARCHIVE_CONFIRMATION_EMBED = override -> new EmbedBuilder()
        .setColor(Color.GREEN)
        .setTitle("Confirm archive")
        .setDescription(override?
            "**Overriding enabled**, the existing plot's thread will be edited to archived status." :
            "Override **disabled**, the system will create a new non-tracked thread to archive this plot.")
        .build();

}
