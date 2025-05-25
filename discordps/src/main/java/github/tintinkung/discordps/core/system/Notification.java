package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;

import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.providers.NotificationProvider;
import github.tintinkung.discordps.core.system.io.LanguageFile;
import github.tintinkung.discordps.core.system.io.NotificationLang;
import github.tintinkung.discordps.core.system.io.lang.Notification.PlotNotification;
import github.tintinkung.discordps.core.system.io.lang.Notification.ErrorNotification;
import github.tintinkung.discordps.core.system.io.lang.Notification.PluginNotification;
import github.tintinkung.discordps.core.system.io.lang.Notification.Config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class Notification extends NotificationProvider {

    public static void notify(@NotNull NotificationLang message, @NotNull String... args) {
        switch (message) {
            case PluginNotification plugin -> {
                if(METADATA.plugin() == Config.DISABLED) break;

                EmbedBuilder embed = DiscordPS.getSystemLang()
                    .getEmbedBuilder(plugin)
                    .setColor(plugin.getAccentColor());

                if(args.length > 0) embed.setDescription(args[0]);

                sendEmbed(METADATA.plugin(), embed.build());
            }
            case PlotNotification plot -> {
                Config config = parseConfig(plot.getKey(), Config.DISABLED);
                if(config == Config.DISABLED) break;

                Function<String, String> applier = argsApplier(plot.getLangArgs(), args);
                MessageEmbed embed = DiscordPS.getSystemLang()
                        .getEmbedBuilder(plot, applier, applier)
                        .setColor(plot.getAccentColor())
                        .build();
                sendEmbed(config, embed);
            }
            case ErrorNotification error -> {
                if(METADATA.errors() == Config.DISABLED) break;
                sendEmbed(METADATA.errors(), prepareErrorEmbed(error, args).build());
            }
            // un-categorized notification, will be sent as plugin related
            case NotificationLang notification -> {
                if(METADATA.plugin() == Config.DISABLED) break;

                sendEmbed(
                    METADATA.plugin(),
                    DiscordPS.getSystemLang()
                        .getEmbedBuilder(notification)
                        .setColor(Constants.GRAY)
                        .build()
                );
            }
        }
    }

    /**
     * Send message embed(s) to the notification channel.
     *
     * @param config The notification config
     * @param embed The {@link MessageEmbed}
     */
    public static void sendEmbed(Config config, @NotNull MessageEmbed embed) {
        if(config == Config.WITH_CONTENT) METADATA.content().ifPresentOrElse(
            content -> getOpt().ifPresent((channel -> channel.sendMessage(content).setEmbeds(embed).queue())),
            () -> getOpt().ifPresent((channel -> channel.sendMessageEmbeds(embed).queue())
        ));
        else if(config == Config.ENABLED)
            getOpt().ifPresent((channel -> channel.sendMessageEmbeds(embed).queue()));
    }

    /**
     * Send message content and embed(s) to the notification channel.
     * @param content Message string content.
     * @param embeds The {@link MessageEmbed}
     */
    public static void sendMessageEmbeds(String content, @NotNull MessageEmbed... embeds) {
        if(METADATA.plugin() == Config.DISABLED) return;

        getOpt().ifPresent((channel -> channel.sendMessage(content).setEmbeds(embeds).queue()));
    }

    /**
     * Send message to the notification channel.
     * @param message The message as String of content
     */
    public static void sendMessage(CharSequence message) {
        if(METADATA.plugin() == Config.DISABLED) return;

        getOpt().ifPresent((channel -> channel.sendMessage(message).queue()));
    }

    public static void sendErrorEmbed(ErrorNotification message, String error, String... args) {
        if(METADATA.errors() == Config.DISABLED) return;

        sendEmbed(METADATA.errors(), prepareErrorEmbed(message, args)
            .addField(METADATA.errorLabel(), "```" + error + "```", false)
            .build()
        );
    }

    public static void sendErrorEmbed(java.awt.Color color, String description, String error) {
        if(METADATA.errors() == Config.DISABLED) return;

        sendEmbed(METADATA.errors(), new EmbedBuilder()
                .setTitle(METADATA.errorTitle())
                .addField(METADATA.errorLabel(), "```" + error + "```", false)
                .setDescription(description)
                .setColor(color)
                .build()
        );
    }

    public static void sendErrorEmbed(java.awt.Color color, String description) {
        if(METADATA.errors() == Config.DISABLED) return;

        sendEmbed(METADATA.errors(), new EmbedBuilder()
                .setTitle(METADATA.errorTitle())
                .setDescription(description)
                .setColor(color)
                .build()
        );
    }

    public static void sendErrorEmbed(String description) {
        sendErrorEmbed(Constants.RED, description);
    }

    public static void sendErrorEmbed(String description, String error) {
        sendErrorEmbed(Constants.RED, description, error);
    }

    private static @NotNull EmbedBuilder prepareErrorEmbed(@NotNull ErrorNotification message,
                                                           String @NotNull ... args) {
        LanguageFile.EmbedLang lang = DiscordPS.getSystemLang().getEmbed(message);
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(METADATA.errorTitle())
                .setColor(message.getAccentColor());

        if(args.length > 0)
            message.getOptLangArgs().ifPresentOrElse(
                format -> embed.setDescription(argsApplier(format, args).apply(lang.description())),
                    () -> embed.setDescription(lang.description())
            );
        else embed.setDescription(lang.description());

        return embed;
    }


    /**
     * Message arguments applier.
     * Apply all the placeholder defined from langArgs to each provided arguments.
     *
     * @param langArgs formatter placeholder to apply arguments to
     * @param args The provided argument to apply, must match the order of langArgs placeholders
     * @return An applier function to be used as the message provider
     */
    @Contract(pure = true)
    private static @NotNull Function<String, String> argsApplier(String[] langArgs, String... args) {
        return description -> {
            int limit = Math.min(args.length, langArgs.length);
            for (int i = 0; i < limit; i++)
                description = description.replace(langArgs[i], args[i]);
            return description;
        };
    }
}
