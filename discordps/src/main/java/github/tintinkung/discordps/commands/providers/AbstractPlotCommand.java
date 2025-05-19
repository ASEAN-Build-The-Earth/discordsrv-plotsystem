package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Emoji;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectOption;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import github.tintinkung.discordps.commands.interactions.Interaction;
import github.tintinkung.discordps.commands.interactions.PlotInteraction;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.MemberOwnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractPlotCommand<T extends Interaction & PlotInteraction> extends SubcommandProvider<T> {

    public AbstractPlotCommand(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).setEphemeral(true).queue();
    }

    protected void queueEmbed(@NotNull InteractionHook hook, @NotNull ActionRow interactions, @NotNull MessageEmbed embed) {
        hook.sendMessageEmbeds(embed).addActionRows(interactions).setEphemeral(true).queue();
    }

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

                this.menu.addOptions(entryOption.withLabel(label + " [Latest]")
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

    // Error Constants

    protected static final Throwable PLOT_CREATE_RETURNED_NULL = new NoSuchElementException(
            "Plot create action returned empty, possibly an internal error occurred! "
            + "please debug this process for more info."
    );

    protected static final Throwable PLOT_FETCH_UNKNOWN_OWNER = new RuntimeException(
            "Plot has no owner! This plugin does not support fetching un-claim plot.\n"
            + "A plot must be created by a member first before it can be manually fetch for any updates."
    );

    protected static final Throwable PLOT_FETCH_RETURNED_NULL = new RuntimeException(
            "Failed to fetch plot data from plot-system database, possibly sql exception has occurred. "
            + "please debug this process for more info."
    );
}
