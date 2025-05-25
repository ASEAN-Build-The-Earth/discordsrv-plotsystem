package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.commands.interactions.Interaction;
import github.tintinkung.discordps.core.system.io.LangConfig;
import github.tintinkung.discordps.core.system.io.LangManager;
import github.tintinkung.discordps.core.system.io.LanguageFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.function.Function;

public abstract class SubcommandProvider<I extends Interaction, T extends LangConfig, V extends T>
    extends SubcommandData
    implements SlashCommand<I> {

    public SubcommandProvider(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    public final void trigger(@NotNull InteractionHook hook, @Nullable I payload) {
        if (payload == null) onCommandTriggered(hook);
        else onCommandTriggered(hook, payload);
    }

    protected interface CommandButton extends Function<Interaction, Button> {};

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

    protected abstract LangManager<T> getLangManager();

    protected String getLang(V lang) {
        return getLangManager().get(lang);
    }

    protected LanguageFile.EmbedLang getEmbed(V lang) {
        return getLangManager().getEmbed(lang);
    }

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
