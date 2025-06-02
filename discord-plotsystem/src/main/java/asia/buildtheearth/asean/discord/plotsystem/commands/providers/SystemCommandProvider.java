package asia.buildtheearth.asean.discord.plotsystem.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LangManager;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import org.jetbrains.annotations.NotNull;

/**
 * Provider for all system-related commands.
 * Force {@link SystemLang} as the language manager.
 *
 * <p>Note: System languages has special error embed
 * methods that can be used in each command</p>
 *
 * @param <I> Interaction type of this command data.
 * @param <V> Values for the language messages for this command (Conventionally as an enum)
 *
 * @see SubcommandProvider
 */
public class SystemCommandProvider<
        I extends Interaction, V extends SystemLang>
        extends SubcommandProvider<I, SystemLang, V> {

    /**
     * Initialize a command data with name and description
     *
     * @param name The name of this command
     * @param description The description of the command
     */
    public SystemCommandProvider(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    /**
     * Get the lang manager of this command.
     *
     * @return Language manager of type {@link SystemLang}
     */
    @Override
    protected LangManager<SystemLang> getLangManager() {
        return DiscordPS.getSystemLang();
    }

    /**
     * Create a red-blocking error embed stacktrace.
     *
     * @param error The error message.
     * @return A built message embed with the error message in the stacktrace section.
     */
    protected MessageEmbed errorEmbed(String error) {
        return errorEmbed(
            getLangManager().get(CommandInteractions.LABEL_ERROR_OCCURRED),
            getLangManager().get(CommandInteractions.LABEL_ERROR),
            error
        );
    }

    /**
     * Create a red-blocking error embed.
     *
     * @param description The error description as a lang signature to get.
     * @return A built message embed with the description as the embed {@code description} field
     */
    protected MessageEmbed errorEmbed(V description) {
        return errorEmbed(
            getLangManager().get(CommandInteractions.LABEL_ERROR_OCCURRED),
            getLang(description)
        );
    }

    /**
     * Create a red-blocking error embed with SQL error title.
     *
     * @param description The error description as a lang signature to get.
     * @param error The occurred error stacktrace as string.
     * @return A built message embed with the description as the embed {@code description} field
     *         and a stacktrace section as a code block
     */
    protected MessageEmbed sqlErrorEmbed(V description, String error) {
        return sqlErrorEmbed(
            getLangManager().get(CommandInteractions.LABEL_SQL_ERROR),
            getLang(description),
            error
        );
    }

    /**
     * Create a red-blocking error embed.
     *
     * @param description The error description as a lang signature to get.
     * @param error The occurred error stacktrace as string.
     * @return A built message embed with the description as the embed {@code description} field
     *         and a stacktrace section as a code block
     */
    protected MessageEmbed errorEmbed(V description, String error) {
        return errorEmbed(
            getLangManager().get(CommandInteractions.LABEL_ERROR_OCCURRED),
            getLang(description),
            error
        );
    }

    /**
     * Parse an error embed from embed lang data.
     *
     * @param info The language signature to be parsed as an error embed.
     * @return Red-blocking error embed with the language's title and description.
     */
    protected MessageEmbed errorEmbed(@NotNull LanguageFile.EmbedLang info) {
        return errorEmbed(info.title(), info.description());
    }

    /**
     * Parse an error embed from embed lang data with an additional stacktrace.
     *
     * @param info The language signature to be parsed as an error embed.
     * @param error The error stacktrace as string.
     * @return Red-blocking error embed with the language's title and description.
     */
    protected MessageEmbed errorEmbed(@NotNull LanguageFile.EmbedLang info, String error) {
        return errorEmbed(info.title(), info.description(), error);
    }

    /**
     * Create a custom SQL error embed with full information.
     *
     * @param title The title of this error embed.
     * @param description The description of this error.
     * @param error The error stacktrace as string.
     * @return Red-blocking error embed all the specified parameters.
     */
    protected MessageEmbed sqlErrorEmbed(String title, String description, String error) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .addField(getLangManager().get(CommandInteractions.LABEL_SQL_ERROR),
                "```" + error + "```", false)
            .setColor(Constants.RED)
            .build();
    }

    /**
     * Create a custom error embed with full information.
     *
     * @param title The title of this error embed.
     * @param description The description of this error.
     * @param error The error stacktrace as string.
     * @return Red-blocking error embed all the specified parameters.
     */
    protected MessageEmbed errorEmbed(String title, String description, String error) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .addField(getLangManager().get(CommandInteractions.LABEL_ERROR),
                "```" + error + "```", false)
            .setColor(Constants.RED)
            .build();
    }

    /**
     * Create a custom error embed with no stacktrace information.
     *
     * @param title The title of this error embed.
     * @param description The description of this error.
     * @return Red-blocking error embed all the specified parameters.
     */
    protected MessageEmbed errorEmbed(String title, String description) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(Constants.RED)
            .build();
    }
}
