package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.plotsystem.commands.providers.SystemCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.SetupWebhookEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnSetupWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableButton;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.SetupWebhookCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.nio.file.Path;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.WEBHOOK_AVATAR_FILE;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.SetupWebhookCommand.*;

abstract sealed class AbstractSetupWebhookCommand
        extends SystemCommandProvider<OnSetupWebhook, SetupWebhookCommand>
        implements SetupWebhookEvent
        permits asia.buildtheearth.asean.discord.plotsystem.commands.SetupWebhookCommand {

    protected String outputFile;

    public AbstractSetupWebhookCommand(@NotNull String name, @NotNull String outputFile) {
        super(name, LanguageFile.NULL_LANG);

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

    @Override
    protected String[] getLangArgs() {
        return new String[] { Format.PATH, Format.FILENAME };
    }

    protected MessageEmbed formatInfoEmbed(String name, String channel) {
        return this.getLangManager().getEmbedBuilder(EMBED_CONFIRM_SETTINGS)
                .addField(getLang(MESSAGE_WEBHOOK_NAME), "```" + name + "```\n-# " + DESC_NAME, false)
                .addField(getLang(MESSAGE_WEBHOOK_CHANNEL), "```" + channel + "```\n-# " + DESC_CHANNEL, false)
                .setColor(Constants.ORANGE)
                .build();
    }

    protected MessageEmbed formatImageEmbed() {
        LanguageFile.EmbedLang attachInfo = getLangManager().getEmbed(CommandInteractions.EMBED_ATTACH_IMAGE);
        LanguageFile.EmbedLang provideInfo = getLangManager().getEmbed(CommandInteractions.EMBED_PROVIDE_IMAGE);
        String location = DiscordPS.getPlugin().getDataFolder().getAbsolutePath();

        return this.getLangManager().getEmbedBuilder(EMBED_SETUP_AVATAR_IMAGE)
                .addField(attachInfo.title(), attachInfo.description(), false)
                .addField(provideInfo.title(), provideInfo.description()
                    .replace(Format.FILENAME, WEBHOOK_AVATAR_FILE)
                    .replace(Format.PATH, location),
                    false)
                .setColor(Constants.ORANGE)
                .build();
    }

    protected EmbedBuilder formatWebhookInformation(Color color,
                                                    SetupWebhookCommand message,
                                                    String outputFile,
                                                    String yamlData,
                                                    @Nullable String createdURL) {
        LanguageFile.EmbedLang lang = getEmbed(message);

        return new EmbedBuilder()
            .setTitle(lang.title(), createdURL)
            .setDescription(lang.description())
            .addField(outputFile,"```yml\n" + yamlData + "\n```", false)
            .setColor(color);
    }


    protected enum Button {
        SUBMIT_AVATAR_BUTTON(ButtonStyle.SUCCESS, AvailableButton.WEBHOOK_SUBMIT_AVATAR, CommandInteractions.BUTTON_ATTACH_IMAGE),
        PROVIDED_AVATAR_BUTTON(ButtonStyle.DANGER, AvailableButton.WEBHOOK_PROVIDED_AVATAR, CommandInteractions.BUTTON_PROVIDE_IMAGE),
        CONFIRM_CONFIG_BUTTON(ButtonStyle.SUCCESS, AvailableButton.WEBHOOK_CONFIRM_CONFIG, CommandInteractions.BUTTON_CREATE_WEBHOOK),
        CANCEL_CONFIG_BUTTON(ButtonStyle.DANGER, AvailableButton.WEBHOOK_CANCEL_CONFIG, CommandInteractions.BUTTON_CANCEL);

        private final CommandButton button;

        Button(ButtonStyle style, AvailableButton type, CommandInteractions label) {
            this.button = payload -> github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button.of(
                    style, type.resolve(payload.eventID, payload.userID), DiscordPS.getSystemLang().get(label)
            );
        }

        public Component get(OnSetupWebhook interaction) {
            return button.apply(interaction);
        }
    }
}
