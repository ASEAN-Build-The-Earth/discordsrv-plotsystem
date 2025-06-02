package asia.buildtheearth.asean.discord.plotsystem.commands.providers;

import asia.buildtheearth.asean.discord.commands.SlashCommand;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LangConfig;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LangManager;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.function.Function;

/**
 * Provider for sub command classes.
 * Note that currently all functional command is a subcommand.
 *
 * @param <I> Interaction type of this command
 * @param <T> Type of the language file for this command
 *           ({@link asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang SystemLang}
 *           or {@link asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang MessageLang})
 * @param <V> Values for the language messages for this command (Conventionally as an enum)
 */
public abstract class SubcommandProvider<I extends Interaction, T extends LangConfig, V extends T>
    extends SubcommandData
    implements SlashCommand<I> {

    /**
     * Initialize a command data with name and description
     *
     * @param name The name of this command
     * @param description The description of the command
     */
    public SubcommandProvider(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    /**
     * {@inheritDoc}
     */
    public final void trigger(@NotNull InteractionHook hook, @Nullable I payload) {
        if (payload == null) onCommandTriggered(hook);
        else onCommandTriggered(hook, payload);
    }

    /**
     * Functional interface for creating a command owned {@link Button}.
     */
    @FunctionalInterface
    protected interface CommandButton extends Function<Interaction, Button> {};

    /**
     * Functional interface for creating a command owned {@link SelectionMenu.Builder}.
     */
    @FunctionalInterface
    protected interface CommandSelectionMenu extends Function<Interaction, SelectionMenu.Builder> {};

    /**
     * Trigger a command with specified payload
     *
     * @param hook The triggered command interaction hook
     * @param payload The payload to start this command events
     */
    protected void onCommandTriggered(InteractionHook hook, I payload) {
        throw new IllegalArgumentException("No implementation found for expected slash command payload.");
    }

    /**
     * Trigger a command without any payload
     *
     * @param hook The triggered command interaction hook
     */
    protected void onCommandTriggered(InteractionHook hook) {
        throw new IllegalArgumentException("No implementation found for expected slash command.");
    }

    /**
     * Provide the main language manager for this command.
     *
     * @return The language manager
     * @see asia.buildtheearth.asean.discord.plotsystem.DiscordPS#getSystemLang()
     * @see asia.buildtheearth.asean.discord.plotsystem.DiscordPS#getMessagesLang()
     */
    protected abstract LangManager<T> getLangManager();

    /**
     * Get lang message from this command's language manager.
     *
     * @param lang The language signature to get
     * @return The lang message as a string
     */
    protected String getLang(V lang) {
        return getLangManager().get(lang);
    }

    /**
     * Get embed lang from this command's language manager.
     *
     * <p>Note that this will fail if the defined signature
     * does not define as an embed field.</p>
     *
     * @param lang The language signature to get
     * @return Embed lang data as a record of {@code title} and {@code description}
     */
    protected LanguageFile.EmbedLang getEmbed(V lang) {
        return getLangManager().getEmbed(lang);
    }

    /**
     * Build an embed from a language signature with a specified color.
     *
     * <p>Note that this will fail if the defined signature
     * does not define as an embed field.</p>
     *
     * @param color The color to apply to this embed
     * @param lang The language signature to get
     * @return A built message embed data
     */
    protected MessageEmbed getEmbed(Color color, V lang) {
        return getLangManager().getEmbedBuilder(lang).setColor(color).build();
    }

    /**
     * Builds a {@link MessageEmbed} with the specified color and language context, replacing language
     * argument placeholders with the provided argument values.
     *
     * @param color the {@link Color} to apply to the embed.
     * @param lang  the language context used to retrieve the localized embed template.
     * @param args  optional arguments that replace placeholders in the embed description.
     *              The order of the arguments must match the order of placeholders returned by {@link #getLangArgs()}.
     * @return a {@link MessageEmbed} with localized and dynamically filled description content.
     */
    protected MessageEmbed getEmbed(Color color, V lang, String... args) {
        EmbedBuilder embed = getLangManager().getEmbedBuilder(lang, argsApplier(args));
        return embed.setColor(color).build();
    }

    /**
     * Intended to be overridden by subclasses to specify the language placeholders
     *
     * @return an array of placeholder strings. The default implementation returns an empty array.
     * @see #getEmbed(Color, V, String...)
     */
    protected String[] getLangArgs() {
        return new String[0];
    }

    /**
     * Message arguments applier.
     * Apply all the placeholder defined from {@link #getLangArgs()} to each provided arguments.
     *
     * @param args The provided argument to apply, must match the order of placeholders returned by {@link #getLangArgs()}
     * @return An applier function to be used as the message provider
     */
    @Contract(pure = true)
    private @NotNull Function<String, String> argsApplier(String... args) {
        return description -> {
            String[] langArgs = this.getLangArgs();
            int limit = Math.min(args.length, langArgs.length);
            for (int i = 0; i < limit; i++)
                description = description.replace(langArgs[i], args[i]);
            return description;
        };
    }
}
