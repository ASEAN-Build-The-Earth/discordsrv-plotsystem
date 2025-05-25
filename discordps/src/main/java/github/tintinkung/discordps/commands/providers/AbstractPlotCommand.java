package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.Interaction;
import github.tintinkung.discordps.commands.interactions.PlotInteraction;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.MemberOwnable;
import github.tintinkung.discordps.core.system.io.LanguageFile;
import github.tintinkung.discordps.core.system.io.SystemLang;
import github.tintinkung.discordps.core.system.io.lang.CommandInteractions;
import github.tintinkung.discordps.core.system.io.lang.PlotCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Super class to all plot management commands
 *
 * @param <I> The command's interaction payload
 * @param <V> The command's language data that contains messages enum
 */
public abstract class AbstractPlotCommand<
        I extends Interaction & PlotInteraction, V extends SystemLang
        > extends SystemCommandProvider<I, V> {

    /**
     * Construct a new plot command with no description.
     *
     * <p>Note that description is set to {@link LanguageFile#NULL_LANG} by default,
     * use {@link #setDescription(String)} to override a new description in.</p>
     *
     * @param name The subcommand name
     */
    public AbstractPlotCommand(@NotNull String name) {
        super(name, LanguageFile.NULL_LANG);
    }

    /**
     * Queue a new message embed to this interaction.
     *
     * <p>By default, this has ephemeral set to {@code true}.
     *    Override this method to modify the interaction.</p>
     *
     * @param hook The interaction hook to send message embed to
     * @param embed The message embed to send
     */
    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).setEphemeral(true).queue();
    }

    /**
     * Queue a new message embed to this interaction with an action row.
     *
     * <p>By default, this has ephemeral set to {@code true}.
     *    Override this method to modify the interaction.</p>
     *
     * @param hook The interaction hook to send message embed to
     * @param interactions The action row interaction to attach to the embed
     * @param embed The message embed to send
     */
    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull ActionRow interactions, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).addActionRows(interactions).setEphemeral(true).queue();
    }

    /**
     * Get a display emoji for each status
     * which is a colored circles for default status colors.
     *
     * @param status The thread status
     * @return {@link Emoji} instance from unicode
     */
    protected Emoji getStatusEmoji(@NotNull ThreadStatus status) {
        return switch (status) {
            case on_going -> Emoji.fromUnicode("U+26AA");
            case finished -> Emoji.fromUnicode("U+1F7E1");
            case rejected -> Emoji.fromUnicode("U+1F534");
            case approved -> Emoji.fromUnicode("U+1F7E2");
            case archived -> Emoji.fromUnicode("U+1F535");
            case abandoned -> Emoji.fromUnicode("U+1F7E3");
        };
    }

    /**
     * Data register for plot entry selection menu
     *
     * @see #EntryMenuBuilder(SelectionMenu.Builder, Consumer) Create new register
     * @see #registerMenuOption(String, SelectOption) Register an option
     * @see #build() Build to SelectionMenu
     */
    protected static class EntryMenuBuilder {

        private final Consumer<List<SelectOption>> defaultOptionSelector;
        private final SelectionMenu.Builder menu;

        /**
         * Create a new menu register from an instance,
         * applying default option selector for choosing the default option to display for first registry.
         *
         * @param menu The menu builder instance
         * @param defaultOptionSelector Action invoked when the register initially apply the first default option
         */
        public EntryMenuBuilder(SelectionMenu.Builder menu, Consumer<List<SelectOption>> defaultOptionSelector) {
            this.menu = menu;
            this.defaultOptionSelector = defaultOptionSelector;
        }

        /**
         * Register a new option to this entry, set default option if this is the first register.
         *
         * @param label The option label of this entry, conventionally from {@link #formatEntryOption(WebhookEntry, BiConsumer)}
         * @param entryOption The actual option data that will be added
         */
        public void registerMenuOption(String label, SelectOption entryOption) {
            if(this.menu.getOptions().isEmpty()) {
                this.defaultOptionSelector.accept(Collections.singletonList(entryOption));

                // Format latest entry as "@entry [Latest]"
                final String latest = DiscordPS.getSystemLang().get(CommandInteractions.LABEL_LATEST);

                this.menu.addOptions(entryOption.withLabel(label + " [" + latest + "]")
                    .withDefault(true));
            }
            else this.menu.addOptions(entryOption);
        }

        /**
         * Build to SelectionMenu
         *
         * @return Serialized menu data
         */
        public SelectionMenu build() {
            return this.menu.build();
        }
    }

    /**
     * Format each entry as {@link #getStatusEmoji(ThreadStatus) :emoji:} {@code @discord-name (minecraft-name)}
     * and build the data into {@link SelectOption}
     *
     * @param entry The webhook entry to be formatted
     * @param out Consumer to accept output data as {@link String label} and {@link SelectOption}
     */
    protected void formatEntryOption(@NotNull WebhookEntry entry, @NotNull BiConsumer<String, SelectOption> out) {
        Emoji statusEmote = getStatusEmoji(entry.status());

        MemberOwnable member = new MemberOwnable(entry.ownerUUID());
        String entryID = Long.toUnsignedString(entry.messageID());
        String label = member.formatOwnerName() + " (" + entry.status() + ")";
        SelectOption entryOption = SelectOption.of(label, entryID).withEmoji(statusEmote);

        out.accept(label, entryOption);
    }

    /**
     * Common Error constants.
     * Stores each error type and the description message.
     *
     * @see #get()
     * @see #getMessage()
     */
    protected enum Error {
        /**
         * Intended for when plot action that returns {@link java.util.Optional} return an empty.
         */
        PLOT_CREATE_RETURNED_NULL(NoSuchElementException::new, PlotCommand.EMPTY_ACTION_EXCEPTION),
        /**
         * When fetching plot data with no owner which is not supported by this plugin.
         */
        PLOT_FETCH_UNKNOWN_OWNER(RuntimeException::new, PlotCommand.UNKNOWN_OWNER_EXCEPTION),
        /**
         * When fetching null plot data, the only case of this happening is an SQL exception.
         */
        PLOT_FETCH_RETURNED_NULL(RuntimeException::new, PlotCommand.NULL_ACTION_EXCEPTION);

        private final @NotNull Function<String, Throwable> throwable;
        private final @NotNull SystemLang message;

        Error(@NotNull Function<String, Throwable> throwable,
              @NotNull SystemLang message) {
            this.throwable = throwable;
            this.message = message;
        }

        /**
         * Create a throwable instance of this error
         *
         * @return Throwable as a new instance
         */
        public @NotNull Throwable get() {
            return this.throwable.apply(this.getMessage());
        }

        /**
         * Get the description message
         *
         * @return Message from system language file
         */
        public @NotNull String getMessage() {
            return DiscordPS.getSystemLang().get(this.message);
        }
    }
}
