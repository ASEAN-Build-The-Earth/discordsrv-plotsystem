package asia.buildtheearth.asean.discord.plotsystem.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;

import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.NotificationProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.NotificationLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PlotNotification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.ErrorNotification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.PluginNotification;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.Config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Static notification manager class.
 *
 * <p>This statically parse configured notification channel on first use,
 * and is available for the entire system after.</p>
 *
 * @see #notify(NotificationLang, String...)
 */
public abstract class Notification extends NotificationProvider {

    /**
     * Notify a notification message dynamically with defined language information.
     *
     * <p>Supported types are: {@linkplain PluginNotification PluginNotification}, {@linkplain PlotNotification PlotNotification},
     * {@linkplain ErrorNotification ErrorNotification}, and {@linkplain NotificationLang NotificationLang}</p>
     *
     * @param message The message definition to retrieve language information from.
     * @param args Supported placeholder argument(s) defined by each notification message.
     */
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
                Config config = parseConfig(plot.getConfig(), Config.DISABLED);
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
     *
     * @param content Message string content.
     * @param embeds The {@link MessageEmbed}
     */
    public static void sendMessageEmbeds(String content, @NotNull MessageEmbed... embeds) {
        if(METADATA.plugin() == Config.DISABLED) return;

        getOpt().ifPresent((channel -> channel.sendMessage(content).setEmbeds(embeds).queue()));
    }

    /**
     * Send message to the notification channel.
     *
     * @param message The message as String of content.
     */
    public static void sendMessage(CharSequence message) {
        if(METADATA.plugin() == Config.DISABLED) return;

        getOpt().ifPresent((channel -> channel.sendMessage(message).queue()));
    }

    /**
     * Send red error embed.
     *
     * @param message The error message language definition.
     * @param error The error stacktrace message.
     * @param args Supported placeholder argument(s) of the message.
     */
    public static void sendErrorEmbed(ErrorNotification message, String error, String... args) {
        if(METADATA.errors() == Config.DISABLED) return;

        sendEmbed(METADATA.errors(), prepareErrorEmbed(message, args)
            .addField(METADATA.errorLabel(), "```" + error + "```", false)
            .build()
        );
    }

    /**
     * Send error embed with a specific color.
     *
     * @param color The color to be used.
     * @param description The error description message.
     * @param error The error stacktrace message.
     */
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

    /**
     * Send error embed with a specific color.
     *
     * @param color The color to be used.
     * @param description The error description message.
     */
    public static void sendErrorEmbed(java.awt.Color color, String description) {
        if(METADATA.errors() == Config.DISABLED) return;

        sendEmbed(METADATA.errors(), new EmbedBuilder()
                .setTitle(METADATA.errorTitle())
                .setDescription(description)
                .setColor(color)
                .build()
        );
    }

    /**
     * Send red error embed.
     *
     * @param description The error description message.
     */
    public static void sendErrorEmbed(String description) {
        sendErrorEmbed(Constants.RED, description);
    }

    /**
     * Send red error embed.
     *
     * @param description The error description message.
     * @param error The error stacktrace message.
     */
    public static void sendErrorEmbed(String description, String error) {
        sendErrorEmbed(Constants.RED, description, error);
    }

    /**
     * Prepare an error embed as an {@linkplain EmbedBuilder}.
     *
     * @param message The error message language definition.
     * @param args Supported placeholder argument(s) of the message.
     * @return New embed builder instance built with the error title, description, and its accent color.
     */
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
