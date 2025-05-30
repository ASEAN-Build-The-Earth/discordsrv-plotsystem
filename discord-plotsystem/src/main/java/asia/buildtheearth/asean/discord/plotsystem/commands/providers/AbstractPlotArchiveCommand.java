package asia.buildtheearth.asean.discord.plotsystem.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotArchiveEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotArchive;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableButton;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotArchiveCommand;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotArchiveCommand.*;

public abstract class AbstractPlotArchiveCommand
    extends AbstractPlotCommand<OnPlotArchive, PlotArchiveCommand>
    implements PlotArchiveEvent {

    public AbstractPlotArchiveCommand(@NotNull String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void onCommandTriggered(InteractionHook hook, OnPlotArchive payload);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).setEphemeral(false).queue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull ActionRow interactions, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).addActionRows(interactions).setEphemeral(false).queue();
    }

    protected void queueImagesOption(@NotNull InteractionHook hook,
                                     @NotNull OnPlotArchive payload) {
        ActionRow options = ActionRow.of(
            Button.ATTACH_IMAGES.get(payload),
            Button.PROVIDE_IMAGES.get(payload),
            Button.ARCHIVE_DISMISS.get(payload)
        );
        this.queueImagesOption(hook, options, formatImageEmbed(PlotArchiveCommand.EMBED_IMAGE_NOT_SET, payload.plotID));
    }

    protected void queueImagesOption(@NotNull InteractionHook hook,
                                     @NotNull ActionRow interactions,
                                     @NotNull MessageEmbed embed) {
        this.queueEmbed(hook, interactions, embed);
    }

    protected MessageEmbed formatImageEmbed(PlotArchiveCommand message, Long plotID) {
        LanguageFile.EmbedLang attachInfo = getLangManager().getEmbed(CommandInteractions.EMBED_ATTACH_IMAGE);
        LanguageFile.EmbedLang provideInfo = getLangManager().getEmbed(CommandInteractions.EMBED_PROVIDE_IMAGE);
        String location = DiscordPS.getPlugin().getDataFolder().getAbsolutePath();

        return this.getLangManager().getEmbedBuilder(message)
            .addField(attachInfo.title(), attachInfo.description(), false)
            .addField(provideInfo.title(), provideInfo.description()
                .replace(Format.FILENAME, Constants.PLOT_IMAGE_FILE + "-[i]")
                .replace(Format.PATH, location + "/media/plot-" + plotID),
            false)
            .setColor(Constants.ORANGE)
            .build();
    }

    protected MessageEmbed formatInfoEmbed(String threadID, long plotID) {
        EmbedBuilder embed = this.getLangManager().getEmbedBuilder(PlotArchiveCommand.EMBED_FOUND_THREAD,
            title -> title.replace(Format.THREAD_ID, threadID),
            description -> description.replace(Format.PLOT_ID, String.valueOf(plotID))
        );

        return embed.setColor(Constants.GREEN).build();
    }

    protected void onImageDownloadFailed(@NotNull Message defer, @NotNull  String error) {
        MessageEmbed embed = errorEmbed(getEmbed(EMBED_ON_IMAGE_DOWNLOAD_FAILED), error);

        defer.editMessageEmbeds(embed).queue();
    }

    protected Consumer<File> getCacheDeleter(MessageChannel channel) {
        return file -> {
            if (file.delete()) channel.sendMessage(
                getLang(MESSAGE_MEDIA_CACHE_DELETED)
                .replace(Format.FILENAME, file.getName()))
                .queue();
        };
    }

    protected MessageEmbed formatPendingImageWarning(String files) {
        return getLangManager().getEmbedBuilder(EMBED_MEDIA_PENDING_CACHE)
            .addField(getLang(MESSAGE_DETECTED_FILES), "```" + files + "```", false)
            .setColor(Constants.ORANGE)
            .build();
    }

    protected MessageEmbed onMediaSaved(int count, String log) {
        String lang = getLang(MESSAGE_MEDIA_SAVED);

        return new EmbedBuilder()
            .setTitle(lang.replace(Format.COUNT, String.valueOf(count)))
            .setDescription(log)
            .setColor(Constants.GREEN)
            .build();
    }

    protected MessageEmbed formatConfirmEmbed(boolean override) {
        String title = getLang(MESSAGE_CONFIRM_ARCHIVE);
        return new EmbedBuilder()
            .setColor(Constants.GREEN)
            .setTitle(title)
            .setDescription(override
                ? getLang(MESSAGE_OVERRIDE_ENABLED)
                : getLang(MESSAGE_OVERRIDE_DISABLED))
            .build();
    }

    protected enum Button {
        ATTACH_IMAGES(ButtonStyle.PRIMARY, AvailableButton.PLOT_IMAGES_ATTACHED, CommandInteractions.BUTTON_ATTACH_IMAGE),
        PROVIDE_IMAGES(ButtonStyle.SECONDARY, AvailableButton.PLOT_IMAGES_PROVIDED, CommandInteractions.BUTTON_PROVIDE_IMAGE),
        ARCHIVE_DISMISS(ButtonStyle.DANGER, AvailableButton.PLOT_ARCHIVE_DISMISS, CommandInteractions.BUTTON_DISMISS),
        ARCHIVE_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.PLOT_ARCHIVE_CONFIRM, CommandInteractions.BUTTON_CONFIRM),
        ARCHIVE_NOW(ButtonStyle.SUCCESS, AvailableButton.PLOT_ARCHIVE_CONFIRM, CommandInteractions.BUTTON_ARCHIVE_NOW),
        ARCHIVE_CANCEL(ButtonStyle.DANGER, AvailableButton.PLOT_ARCHIVE_CANCEL, CommandInteractions.BUTTON_CANCEL),
        SHOWCASE_PLOT(ButtonStyle.SUCCESS, AvailableButton.PLOT_ARCHIVE_SHOWCASE_CONFIRM, CommandInteractions.BUTTON_SHOWCASE_NOW),
        SHOWCASE_DISMISS(ButtonStyle.DANGER, AvailableButton.PLOT_ARCHIVE_SHOWCASE_DISMISS, CommandInteractions.BUTTON_DISMISS);

        private final CommandButton button;

        Button(ButtonStyle style, AvailableButton type, CommandInteractions label) {
            this.button = payload -> github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button.of(
                    style, type.resolve(payload.eventID, payload.userID), DiscordPS.getSystemLang().get(label)
            );
        }

        public Component get(OnPlotArchive interaction) {
            return button.apply(interaction);
        }
    }
}
