package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.ConfigPaths;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.commands.providers.AbstractSetupWebhookCommand;
import github.tintinkung.discordps.utils.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static github.tintinkung.discordps.Constants.WEBHOOK_AVATAR_FILE;

class SetupWebhookCommand extends AbstractSetupWebhookCommand {

    public SetupWebhookCommand(@NotNull String name, @NotNull String outputFile, @NotNull String webhookChannel, @NotNull String webhookName) {
        this(name, outputFile, "Setup Plot-System webhook integration", webhookChannel, webhookName);
    }

    public SetupWebhookCommand(@NotNull String name,
                               @NotNull String outputFile,
                               @NotNull String description,
                               @NotNull String webhookChannel,
                               @NotNull String webhookName) {
        super(name, outputFile, description);

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
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnSetupWebhook payload)  {
        // Exit if webhook already has been configured
        if(DiscordPS.getPlugin().getWebhook() != null) {
            hook.sendMessageEmbeds(ALREADY_CONFIGURED_ERROR.apply(getOutputPath().toString())).queue();
            DiscordPS.getPlugin().exitSlashCommand(payload.eventID);
            return;
        }

        this.onSetupWebhook(hook, payload);
    }

    @Override
    public void onSetupWebhook(@NotNull InteractionHook hook, @NotNull OnSetupWebhook interaction) {
        // Exit early if provided name is invalid
        String username = interaction.webhookName;
        username = username
                .replaceAll("(?i)(cly)d(e)", "$1*$2")
                .replaceAll("(?i)(d)i(scord)", "$1*$2")
                .replaceAll(Constants.DISCORD_SRV_WEBHOOK_PREFIX, "$1*$2")
                .replaceAll(Constants.DISCORD_SRV_WEBHOOK_PREFIX_LEGACY, "$1*$2");

        if (!username.equals(interaction.webhookName)) {
            hook.sendMessageEmbeds(BAD_USERNAME_ERROR).queue();
            DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
            return;
        }

        // Exit early if provided channel is invalid (Must be form channel)
        try {
            int channelType = getChannelType(interaction.webhookChannel, true).complete().orElseThrow();
            // GUILD_FORUM	15	Channel that can only contain threads
            if(channelType != 15) {
                hook.sendMessageEmbeds(BAD_CHANNEL_ERROR).queue();
                DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
                return;
            }
        }
        catch (NoSuchElementException ex) { // Skip if we cant verify it, handle it later
            hook.sendMessageEmbeds(CANT_VERIFY_CHANNEL_ERROR).queue();
        }

        // Confirmation Message
        hook.sendMessageEmbeds(
                CONFIRM_SETTINGS_EMBED.apply(interaction.webhookName, interaction.webhookChannel),
                AVATAR_IMAGE_SETUP_EMBED.apply(WEBHOOK_AVATAR_FILE, DiscordPS.getPlugin().getDataFolder().getAbsolutePath())
            ).addActionRows(ActionRow.of(
                SUBMIT_AVATAR_BUTTON.apply(interaction),
                PROVIDED_AVATAR_BUTTON.apply(interaction)
            )).queue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfirmConfig(@NotNull Message message, @NotNull OnSetupWebhook interaction) {

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
            DiscordPS.error("Unknown error when trying to find webhook image file from resource: " + ex.getMessage());
            DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
            return;
        }

        DiscordPS.createWebhook(channel, name, avatarURI, true)
            .queue((webhook) -> {
                if(webhook.isPresent())
                    onWebhookCreated(webhook.get(), message, interaction.outputFile, imageFile.getName());
                else DiscordPS.error("Failed to request for webhook creation.");
            });
        DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfirmAvatar(@NotNull Message message, @NotNull OnSetupWebhook interaction) {

        Message.Attachment attachment = message.getAttachments().getFirst();
        MessageChannel channel = message.getChannel();
        File avatarFile = DiscordPS.getPlugin()
            .getDataFolder()
            .toPath()
            .resolve(WEBHOOK_AVATAR_FILE + "." + attachment.getFileExtension())
            .toFile();

        CompletableFuture<Message> queuedMessage = channel.sendMessageEmbeds(UPLOADING_EMBED).submit();
        CompletableFuture<File> queuedDownload = attachment.downloadToFile(avatarFile);

        queuedDownload.whenComplete((file, error) -> {
            if(error != null) queuedMessage.thenAccept(defer -> ON_IMAGE_DOWNLOAD_FAILED.accept(defer, error.toString()));
            else queuedMessage.thenAccept(defer -> onConfirmAvatarImage(defer, interaction, file));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfirmAvatarProvided(@NotNull MessageChannel channel, @NotNull OnSetupWebhook interaction) {
        try {
            File file = FileUtil.findImageFileByPrefix(WEBHOOK_AVATAR_FILE);

            // File not found
            if(file == null) {
                channel.sendMessageEmbeds(AVATAR_FILE_NOT_FOUND).queue();
                DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
                return;
            }

            Consumer<Message> onSuccess = defer -> onConfirmAvatarImage(defer, interaction, file);
            channel.sendMessageEmbeds(UPLOADING_EMBED).queue(onSuccess);

        } catch (IOException | IllegalArgumentException ex) {
            DiscordPS.error("Cannot download file to resource", ex);

            channel.sendMessageEmbeds(INTERNAL_EXCEPTION.apply(ex.getMessage())).queue();
            DiscordPS.getPlugin().exitSlashCommand(interaction.eventID);
        }
    }

    /**
     * Invoked after webhook has been created which returned a raw data object.
     *
     * @param webhook The raw data object representing the created webhook
     * @param defer The message reference to the interaction
     * @param outputFile The output file name to save as
     * @param avatarFile The webhook avatar image as its file name
     */
    private void onWebhookCreated(@NotNull DataObject webhook,
                                  Message defer,
                                  String outputFile,
                                  String avatarFile) {
        // Webhook response object into config
        YamlConfiguration config = makeWebhookConfig(
                webhook.getString("name"),
                webhook.getString("id"),
                webhook.getString("token"),
                webhook.getString("channel_id"),
                webhook.getString("guild_id"),
                webhook.getString("url"));

        File filePath = DiscordPS.getPlugin().getDataFolder().toPath().resolve(outputFile).toFile();

        MessageEmbed editedEmbed = this.formatCreatedInformation(
                outputFile,
                config.saveToString(),
                webhook.getString("url"))
            .setThumbnail("attachment://" + avatarFile)
            .build();

        // Try save payload into file
        try { config.save(filePath); }
        catch(IOException | IllegalArgumentException ex) {
            MessageEmbed noPermsEmbed = CREATED_NO_PERMISSION.apply(ex.toString(), this.getOutputFilename());
            defer.editMessageEmbeds(editedEmbed).queue(msg -> msg.replyEmbeds(noPermsEmbed).queue());
            return;
        }

        defer.editMessageEmbeds(editedEmbed).queue((msg) -> {
            msg.replyEmbeds(WEBHOOK_CREATED_SUCCESSFUL.apply(this.getOutputFilename())).queue();
        });
    }

    /**
     * Invoked after avatar image has been uploaded in a message,
     * where this interaction will edit sent message to be confirmation embed
     *
     * @param message Sent message reference
     * @param interaction The interaction data
     * @param image The uploaded avatar image
     */
    private void onConfirmAvatarImage(@NotNull Message defer, @NotNull OnSetupWebhook interaction, @NotNull File image) {
        YamlConfiguration draftConfig = makeWebhookConfig(
            interaction.webhookName,
            null,
            null,
            Long.toUnsignedString(interaction.webhookChannel),
            defer.getGuild().getId(),
            null
        );

        MessageEmbed finalConfirm = this.formatCreatedConfirmation(interaction.outputFile, draftConfig.saveToString())
            .setThumbnail("attachment://" + image.getName())
            .build();

        ActionRow interactions = ActionRow.of(
            CONFIRM_CONFIG_BUTTON.apply(interaction),
            CANCEL_CONFIG_BUTTON.apply(interaction)
        );

        defer.editMessageEmbeds(finalConfirm).setActionRows(interactions).addFile(image).queue();
    }

    /**
     * Fetch channel ID by raw data
     *
     * @param channelID The channel ID snowflake to get its type
     * @param allowSecondAttempt Re-attempt on 404 error
     * @return The channel type integer as an optional
     */
    private static @NotNull RestAction<Optional<Integer>> getChannelType(long channelID, boolean allowSecondAttempt) {

        Route.CompiledRoute route = Route
            .get(Route.Channels.MODIFY_CHANNEL.getRoute())
            .compile(Long.toUnsignedString(channelID));

        return new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, (response, request) -> {
            try {
                if (response.code == 404) {
                    // 404 = Invalid Webhook (most likely to have been deleted)
                    DiscordPS.error("Channel GET returned 404" + (allowSecondAttempt? " ... retrying in 5 seconds" : ""));
                    if(allowSecondAttempt) return getChannelType(channelID, false).completeAfter(5, TimeUnit.SECONDS);
                    request.cancel();

                    return Optional.empty();
                }

                Optional<DataObject> body = response.optObject();

                if (body.isPresent() && body.get().hasKey("type"))
                    return Optional.of(body.get().getInt("type"));
                else return Optional.empty();
            }
            catch (Throwable ex) {
                DiscordPS.warning("Failed to receive API response: " + ex.toString());
                request.cancel();
                return Optional.empty();
            }
        });
    }

    /**
     * Draft a webhook config data with nullable parameters.
     *
     * @param name The webhook name
     * @param id The webhook application ID
     * @param token The webhook token
     * @param channelID The channel ID to subscribe webhook
     * @param guildID The guild ID to subscribe webhook
     * @param url The api endpoint URL for debugging
     * @return The drafted configuration file with null data displayed as "N/A"
     */
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

