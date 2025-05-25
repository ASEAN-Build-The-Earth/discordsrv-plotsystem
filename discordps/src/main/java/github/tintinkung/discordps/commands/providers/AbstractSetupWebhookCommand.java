package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.core.system.AvailableButton;
import github.tintinkung.discordps.core.system.io.LanguageFile;
import github.tintinkung.discordps.core.system.io.lang.CommandInteractions;
import github.tintinkung.discordps.core.system.io.lang.Format;
import github.tintinkung.discordps.core.system.io.lang.SetupWebhookCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.nio.file.Path;

import static github.tintinkung.discordps.Constants.WEBHOOK_AVATAR_FILE;
import static github.tintinkung.discordps.core.system.io.lang.SetupWebhookCommand.*;

public abstract class AbstractSetupWebhookCommand
    extends SystemCommandProvider<OnSetupWebhook, SetupWebhookCommand>
    implements SetupWebhookEvent {
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
