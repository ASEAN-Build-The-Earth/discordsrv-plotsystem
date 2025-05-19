package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.core.system.AvailableButton;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static github.tintinkung.discordps.Constants.WEBHOOK_AVATAR_FILE;

public abstract class AbstractSetupWebhookCommand extends SubcommandProvider<OnSetupWebhook> implements SetupWebhookEvent {
    protected String outputFile;

    public AbstractSetupWebhookCommand(@NotNull String name, @NotNull String outputFile, @NotNull String description) {
        super(name, description);

        this.outputFile = outputFile;
    }

    /**
     * Configuration output file to be saved in plugin root folder.
     *
     * @return yml file name eg. {@code webhook.yml}
     */
    public final String getOutputFilename() {
        return this.outputFile;
    }

    /**
     * Get the output file as an absolute path.
     *
     * @return {@link Path} object representing the absolute path of the output file
     */
    public Path getOutputPath() {
        return DiscordPS.getPlugin()
            .getDataFolder()
            .toPath()
            .resolve(this.getOutputFilename())
            .toAbsolutePath();
    }

    @Override
    public abstract void onCommandTriggered(InteractionHook hook, OnSetupWebhook payload);

    protected static final CommandButton SUBMIT_AVATAR_BUTTON = payload -> Button.success(
        AvailableButton.WEBHOOK_SUBMIT_AVATAR.resolve(payload.eventID, payload.userID), "Attach Image"
    );

    protected static final CommandButton PROVIDED_AVATAR_BUTTON = payload -> Button.danger(
        AvailableButton.WEBHOOK_PROVIDED_AVATAR.resolve(payload.eventID, payload.userID), "Already Provided"
    );

    protected static final CommandButton CONFIRM_CONFIG_BUTTON = payload -> Button.success(
        AvailableButton.WEBHOOK_CONFIRM_CONFIG.resolve(payload.eventID, payload.userID), "Create Webhook"
    );

    protected static final CommandButton CANCEL_CONFIG_BUTTON = payload -> Button.danger(
        AvailableButton.WEBHOOK_CANCEL_CONFIG.resolve(payload.eventID, payload.userID), "Cancel"
    );

    protected static final Function<String, MessageEmbed> ALREADY_CONFIGURED_ERROR = path -> new EmbedBuilder()
        .setTitle("Already configured!")
        .setDescription("A webhook is already configured in the plugin's config file.\n"
            + "If you want to create a new webhook, please delete (and back up) the file: ```"
            + path + "```\n"
            + "Then, restart the plugin and use this command again.")
        .setColor(Color.ORANGE.darker())
        .build();


    protected static final MessageEmbed BAD_USERNAME_ERROR = new EmbedBuilder()
        .setTitle("Bad user name!")
        .setDescription("User cannot contain the substring of discord api "
            + "banned/blocked words (eg. Clyde and Discord) "
            + "and DiscordSRV owned webhook name (DSRV, DiscordSRV)")
        .setColor(Color.RED)
        .build();

    protected static final MessageEmbed BAD_CHANNEL_ERROR =  new EmbedBuilder()
        .setTitle("Bad channel!")
        .setDescription("Only Forum channel is supported by this plugin.")
        .setColor(Color.RED)
        .build();

    protected static final MessageEmbed CANT_VERIFY_CHANNEL_ERROR = new EmbedBuilder()
        .setTitle("Error Occurred!")
        .setDescription(
            "Failed to try verify channel type, "
            + "please make sure the given channel is a forum channel. "
            + "You may proceed with cautions")
        .setColor(Color.RED)
        .build();

    protected static final Function<String, MessageEmbed> INTERNAL_EXCEPTION = msg -> new EmbedBuilder()
        .setTitle("Internal Exception Occurred! `" + msg + "`")
        .setDescription("Please see the server console or send help from the developer.")
        .setColor(Color.RED)
        .build();

    protected static final BiFunction<String, Long, MessageEmbed> CONFIRM_SETTINGS_EMBED = (name, channel) -> new EmbedBuilder()
        .setTitle("Please verify your webhook settings")
        .setColor(Color.ORANGE)
        .addField(
            "Webhook Name:",
            "```" + name + "```"
            + "\n-# The display name that will be used when creating embed in webhook forum channel.",
            false)
        .addField(
            "Webhook Channel:",
            "```" + channel + "```"
            + "\n-# The Forum channel where Discord-PlotSystem will manage.",
            false)
        .build();

    protected static final BiFunction<String, String, MessageEmbed> CREATED_NO_PERMISSION = (error, path) -> new EmbedBuilder()
        .setTitle("Webhook Created BUT no permission to write in config file")
        .setDescription(
            "Please replace the field \"webhook\" in the plugin's data folder at `" + path
            + "` with all fields created in the embed above. "
            + "Run the `/setup` command to check the checklist.")
        .addField("Stack Trace", "```" + error + "```", false)
        .setColor(Color.RED)
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

    protected static final BiFunction<String, String, MessageEmbed> AVATAR_IMAGE_SETUP_EMBED = (name, location) -> new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("Webhook avatar image is not set!")
        .setDescription("Please choose a method to apply webhook avatar image.")
        .addField(
            ":blue_heart: Attach an image",
            "Please send a message with one image attachment below, and click the confirm button to apply.",
            false)
        .addField(
            ":grey_heart: Provide an image",
            "Please upload an image named `" + name + "` in your plugin data folder (`" + location + "`)",
            false)
        .build();

    protected static final MessageEmbed UPLOADING_EMBED = new EmbedBuilder()
        .setTitle("Uploading Attachment . . .")
        .setDescription("Please wait . . .")
        .setColor(Color.ORANGE)
        .build();

    protected static final MessageEmbed AVATAR_FILE_NOT_FOUND = new EmbedBuilder()
        .setTitle("File not found!")
        .setDescription("Make sure the file is named `"
            + WEBHOOK_AVATAR_FILE + "` and provided at the path: ```"
            + DiscordPS.getPlugin().getDataFolder().getAbsolutePath()
            + "```")
        .setColor(Color.RED)
        .build();

    protected static final Function<String, MessageEmbed> WEBHOOK_CREATED_SUCCESSFUL = path -> new EmbedBuilder()
        .setTitle("Discord-PlotSystem Webhook Created")
        .setDescription(
            "Webhook config has been saved to `" + path + "`, "
            + "Run `/setup help` command to check the checklist")
        .setColor(Color.GREEN)
        .build();

    protected EmbedBuilder formatCreatedInformation(String outputFile, String yamlData, String createdURL) {
        return new EmbedBuilder()
            .setTitle("Webhook created", createdURL)
            .setDescription("Consider pinning this message or archive it somewhere.")
            .addField(outputFile,"```yml\n" + yamlData + "\n```", false)
            .setColor(Color.GREEN);
    }

    protected EmbedBuilder formatCreatedConfirmation(String outputFile,String yamlData) {
        return new EmbedBuilder()
            .setTitle("Confirm your final config")
            .setDescription("Make sure the channel is a Forum channel, otherwise the plugin will not be functional.")
            .addField(outputFile,"```yml\n" + yamlData + "\n```", false)
            .setColor(Color.ORANGE);
    }

}
