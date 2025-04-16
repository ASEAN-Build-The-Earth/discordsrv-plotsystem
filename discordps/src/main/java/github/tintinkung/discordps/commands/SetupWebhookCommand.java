package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.ConfigPaths;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.utils.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static github.tintinkung.discordps.Constants.WEBHOOK_AVATAR_FILE;

final class SetupWebhookCommand extends SubcommandData implements SetupWebhookEvent {

    public SetupWebhookCommand(@NotNull String name, @NotNull String webhookChannel, @NotNull String webhookName) {
        super(name, "Setup Plot-System webhook integration");

        this.addOption(
            OptionType.CHANNEL,
            webhookChannel,
            "The webhook forum channel that will be created in",
            true);

        this.addOption(
            OptionType.STRING,
            webhookName,
            "The webhook name that will be displayed",
            true);

    }

    @Override
    public void onSetupWebhook(ActionRow options, @NotNull InteractionHook hook, @NotNull OnSetupWebhook interaction) {

        // Exit if webhook already has been configured
        if(DiscordPS.getPlugin().getWebhook() != null) {
            MessageEmbed errorAlreadyConfigured = new EmbedBuilder()
                    .setTitle("Already configured!")
                    .setDescription("A webhook is already configured in the plugin's config file.\n"
                            + "If you want to create a new webhook, please delete (and back up) the file: ```"
                            + DiscordPS.getPlugin().getDataFolder().getAbsolutePath() + "/webhook.yml```\n"
                            + "Then, restart the plugin and use this command again.")
                    .setColor(Color.ORANGE.darker())
                    .build();
            hook.sendMessageEmbeds(errorAlreadyConfigured).queue();
            DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
            return;
        }

        // Exit early if provided name is invalid
        String username = interaction.webhookName;
        username = username
                .replaceAll("(?i)(cly)d(e)", "$1*$2")
                .replaceAll("(?i)(d)i(scord)", "$1*$2")
                .replaceAll(Constants.DISCORD_SRV_WEBHOOK_PREFIX, "$1*$2")
                .replaceAll(Constants.DISCORD_SRV_WEBHOOK_PREFIX_LEGACY, "$1*$2");

        if (!username.equals(interaction.webhookName)) {
            MessageEmbed errorUsername = new EmbedBuilder()
                    .setTitle("Bad user name!")
                    .setDescription("User cannot contain the substring of discord api "
                            + "banned/blocked words (eg. Clyde and Discord) "
                            + "and DiscordSRV owned webhook name (DSRV, DiscordSRV)")
                    .setColor(Color.RED)
                    .build();
            hook.sendMessageEmbeds(errorUsername).queue();
            DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
            return;
        }

        // Exit early if provided channel is invalid (Must be form channel)
        try {
            int channelType = getChannelType(interaction.webhookChannel, true).complete().orElseThrow();

            DiscordPS.debug("Got channel: " + channelType);

            // GUILD_FORUM	15	Channel that can only contain threads
            if(channelType != 15) {
                MessageEmbed errorChannel = new EmbedBuilder()
                        .setTitle("Bad channel!")
                        .setDescription("Only Forum channel is supported by this plugin.")
                        .setColor(Color.RED)
                        .build();

                hook.sendMessageEmbeds(errorChannel).queue();
                DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
                return;
            }
        }
        catch (NoSuchElementException ex) { // Skip if we cant verify it, handle it later
            DiscordPS.warning("Failed to try verify channel type on slash command /setup webhook, skipping this check.");
        }

        MessageEmbed verifyEmbed = new EmbedBuilder()
                .setTitle("Please verify your webhook settings")
                .setColor(Color.ORANGE)
                .addField(
                        "Webhook Name:",
                        "```" + interaction.webhookName + "```"
                                + "\n-# The display name that will be used when creating embed in webhook forum channel.",
                        false)
                .addField(
                        "Webhook Channel:",
                        "```" + interaction.webhookChannel + "```"
                                + "\n-# The Forum channel where Discord-PlotSystem will manage.",
                        false)
                .build();

        MessageEmbed imageNotifyEmbed = new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Webhook avatar image is not set!")
                .setDescription("Please choose a method to apply webhook avatar image.")
                .addField(
                        ":blue_heart: Attach an image",
                        "Please send a message with one image attachment below, and click the confirm button to apply.",
                        false)
                .addField(
                        ":grey_heart: Provide an image",
                        "Please upload an image named `" + WEBHOOK_AVATAR_FILE + "` in your plugin data folder (`"
                                + DiscordPS.getPlugin().getDataFolder().getAbsolutePath() + "`)",
                        false)
                .build();

        hook.sendMessageEmbeds(verifyEmbed, imageNotifyEmbed)
                .addActionRows(options)
                .queue();
    }

    @Override
    public void onConfirmConfig(Message message, @NotNull OnSetupWebhook interaction) {

        String name = interaction.webhookName;
        String channel = Long.toUnsignedString(interaction.webhookChannel);
        File imageFile;
        String avatarURI;

        try {
            imageFile = FileUtil.findImageFileByPrefix(WEBHOOK_AVATAR_FILE);
            Checks.notNull(imageFile, "Unexpected error of webhook image file");

            avatarURI = FileUtil.convertImageFileToDataURI(imageFile);
            Checks.notNull(avatarURI, "Unexpected error of webhook image URI");

        } catch (IOException | IllegalArgumentException ex) {
            DiscordPS.error("Unknown error when trying to find webhook image file from resource.");
            throw new RuntimeException(ex);
        }

        DiscordPS.initWebhook(channel, name, avatarURI, true)
                .queue((webhook) -> {
                    if(webhook.isPresent())
                        onWebhookCreated(webhook.get(), message, imageFile.getName());
                    else {
                        DiscordPS.error("Failed to request for webhook creation.");
                    }
                });
    }

    @Override
    public void onConfirmAvatar(@NotNull Message message, OnSetupWebhook interaction) {

        Message.Attachment attachment = message.getAttachments().getFirst();
        MessageChannel channel = message.getChannel();

        try {
            String filePath = String.join("/",
                    DiscordPS.getPlugin().getDataFolder().getAbsolutePath(),
                    WEBHOOK_AVATAR_FILE + "." + attachment.getFileExtension());
            File file = attachment.downloadToFile(filePath).get();

            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Uploading Attachment . . .")
                            .setDescription("Please wait . . .")
                            .setColor(Color.ORANGE)
                            .build())
                    .queue((finalMsg) -> onConfirmAvatarImage(finalMsg, interaction, file), this::onMessageSendingFailed);

        } catch (InterruptedException | IllegalArgumentException | ExecutionException ex) {
            DiscordPS.error("cannot download file to resource", ex);
            DiscordPS.error("please download the file: " + attachment.getFileName());
            DiscordPS.error("and upload it into plugin data folder: " + DiscordPS.getPlugin().getDataFolder().getAbsolutePath());
        }

    }

    @Override
    public void onConfirmAvatarProvided(MessageChannel channel, OnSetupWebhook interaction) {
        try {
            File file = FileUtil.findImageFileByPrefix(WEBHOOK_AVATAR_FILE);

            // File not found
            if(file == null) {
                channel.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("File not found!")
                        .setDescription("Make sure the file is named `"
                                + WEBHOOK_AVATAR_FILE + "` and provided at the path `"
                                + DiscordPS.getPlugin().getDataFolder().getAbsolutePath()
                                + "`")
                        .setColor(Color.RED)
                        .build())
                        .queue();
                DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
                return;
            }

            channel.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle("Uploading Attachment . . .")
                            .setDescription("Please wait . . .")
                            .setColor(Color.ORANGE)
                            .build())
                    .queue((finalMsg) -> onConfirmAvatarImage(finalMsg, interaction, file), this::onMessageSendingFailed);

        } catch (IOException | IllegalArgumentException ex) {
            DiscordPS.error("Cannot download file to resource", ex);

            channel.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("Internal Exception Occurred! `" + ex.getMessage() + "`")
                    .setDescription("Please see the server console or send help from the developer.")
                    .setColor(Color.RED)
                    .build())
                    .queue();
            DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
        }
    }

    private void onWebhookCreated(@NotNull DataObject webhook, Message message, String image) {
        // Webhook response object into config
        YamlConfiguration config = makeWebhookConfig(
                webhook.getString("name"),
                webhook.getString("id"),
                webhook.getString("token"),
                webhook.getString("channel_id"),
                webhook.getString("guild_id"),
                webhook.getString("url"));

        String filePath = String.join("/",
                DiscordPS.getPlugin().getDataFolder().getAbsolutePath(),
                "webhook.yml");

        MessageEmbed editedEmbed = new EmbedBuilder()
                .setTitle("Webhook created", webhook.getString("url"))
                .setDescription("Consider pinning this message or archive it somewhere.")
                .addField("webhook.yml","```yml\n" + config.saveToString() + "\n```", false)
                .setThumbnail("attachment://" + image)
                .setColor(Color.GREEN)
                .build();

        // Try save payload into file
        try { config.save(filePath); }
        catch (IOException | IllegalArgumentException ex) {

            MessageEmbed noPermsEmbed = new EmbedBuilder()
                .setTitle("Webhook Created BUT no permission to write in config file")
                .setDescription(
                    "Please replace the field \"webhook\" in the plugin's webhook.yml "
                    + "with all fields created in the embed above. "
                    + "Run the `/setup` command to check the checklist.")
                .addField("Stack Trace", "```" + ex + "```", false)
                .setColor(Color.RED)
                .build();

            message.editMessageEmbeds(editedEmbed).queue((msg) -> {
                msg.replyEmbeds(noPermsEmbed).queue();
            });

            return;
        }

        MessageEmbed completeEmbed = new EmbedBuilder()
                .setTitle("Discord-PlotSystem Webhook Created")
                .setDescription(
                    "Webhook config has been saved to `webhook.yml`, "
                    + "Run `/setup help` command to check the checklist")
                .setColor(Color.GREEN)
                .build();

        message.editMessageEmbeds(editedEmbed).queue((msg) -> {
            msg.replyEmbeds(completeEmbed).queue();
        });
    }

    private void onConfirmAvatarImage(@NotNull Message message, @NotNull OnSetupWebhook interaction, @NotNull File image) {
        YamlConfiguration draftConfig = makeWebhookConfig(
                interaction.webhookName,
                null,
                null,
                Long.toUnsignedString(interaction.webhookChannel),
                message.getGuild().getId(),
                null);

        MessageEmbed finalConfirm = new EmbedBuilder()
                .setTitle("Confirm your final config")
                .setDescription("Make sure the channel is a Forum channel, otherwise the plugin will not be functional.")
                .addField("webhook.yml","```yml\n" + draftConfig.saveToString() + "\n```", false)
                .setThumbnail("attachment://" + image.getName())
                .setColor(Color.ORANGE)
                .build();

        Button confirmButton = Button.success(
            Constants.NEW_CONFIRM_CONFIG_BUTTON.apply(
                interaction.eventID,
                interaction.userID),
            "Create Webhook"
        );

        Button cancelButton = Button.danger(
            Constants.NEW_CANCEL_CONFIG_BUTTON.apply(
                interaction.eventID,
                interaction.userID),
            "Cancel"
        );

        message.editMessageEmbeds(finalConfirm).setActionRows(ActionRow.of(confirmButton, cancelButton)).addFile(image).queue();
    }

    private void onMessageSendingFailed(Throwable reason) {
        DiscordPS.error(reason);
        DiscordPS.warning("Cannot send message to an activated interaction, maybe it does not have permission to access that channel");
        DiscordPS.warning("Cannot send message to an interaction: " + reason.getMessage());
    }


    private static RestAction<Optional<Integer>> getChannelType(long channelID, boolean allowSecondAttempt) {

        Route.CompiledRoute route = Route
                .get(Route.Channels.MODIFY_CHANNEL.getRoute())
                .compile(Long.toUnsignedString(channelID));

        return new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, (response, request) -> {
            try {
                if (response.code == 404) {
                    // 404 = Invalid Webhook (most likely to have been deleted)
                    DiscordPS.error("Channel GET returned 404" + (allowSecondAttempt? " ... retrying in 5 seconds" : ""));
                    if(allowSecondAttempt)
                        return getChannelType(channelID, false).completeAfter(5, TimeUnit.SECONDS);
                    request.cancel();

                    return Optional.empty();
                }

                Optional<DataObject> body = response.optObject();

                if (body.isPresent()) {
                    if(body.get().hasKey("type"))
                        return Optional.of(body.get().getInt("type"));

                    return Optional.empty();
                }
            }
            catch (Throwable ex) {
                DiscordPS.warning("Failed to receive API response: " + ex.toString());
                request.cancel();
            }
            return Optional.empty();
        });
    }

    private static @NotNull YamlConfiguration makeWebhookConfig(
            @Nullable String name,
            @Nullable String id,
            @Nullable String token,
            @Nullable String channelID,
            @Nullable String guildID,
            @Nullable String url) {

        YamlConfiguration config = new YamlConfiguration();
        Function<String, String> draft = (value) -> value == null? "N/A" : value;

        config.set(ConfigPaths.WEBHOOK_NAME,       draft.apply(name));
        config.set(ConfigPaths.WEBHOOK_ID,         draft.apply(id));
        config.set(ConfigPaths.WEBHOOK_TOKEN,      draft.apply(token));
        config.set(ConfigPaths.WEBHOOK_CHANNEL_ID, draft.apply(channelID));
        config.set(ConfigPaths.WEBHOOK_GUILD_ID,   draft.apply(guildID));
        config.set(ConfigPaths.WEBHOOK_URL,        draft.apply(url));
        return config;
    }
}

