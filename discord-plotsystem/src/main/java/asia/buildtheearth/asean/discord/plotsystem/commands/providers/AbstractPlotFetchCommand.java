package asia.buildtheearth.asean.discord.plotsystem.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.selections.SelectionMenu;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotFetchEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotFetch;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableButton;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotFetchCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotFetchCommand.*;

public abstract class AbstractPlotFetchCommand
        extends AbstractPlotCommand<OnPlotFetch, PlotFetchCommand>
        implements PlotFetchEvent {

    public AbstractPlotFetchCommand(@NotNull String name) {
        super(name);
    }

    @Override
    public abstract void onCommandTriggered(InteractionHook hook, OnPlotFetch payload);

    protected final @NotNull MessageEmbed formatConfirmationEmbed(long plotID, boolean override, @Nullable ThreadStatus status) {
        return new EmbedBuilder()
            .setColor(Constants.GREEN)
            .setTitle(getLang(MESSAGE_CONFIRM_FETCH))
            .addField(getLang(MESSAGE_PLOT_ID),  "```" + plotID + "```", true)
            .addField(getLang(MESSAGE_OVERRIDE),  "```" + override + "```", true)
            .addField(getLang(MESSAGE_STATUS),  "```" + (status == null? "N/A" : status.name()) + "```", true)
            .build();
    }

    // Interactive Components

    protected static final CommandSelectionMenu FETCH_SELECTION_MENU = payload -> SelectionMenu.create(
        AvailableButton.PLOT_FETCH_SELECTION.resolve(payload.eventID, payload.userID)
    );

    protected MessageEmbed formatOverrideWarning(@NotNull String size, boolean override) {
        return getLangManager()
            .getEmbedBuilder(EMBED_ALREADY_REGISTERED,
                description -> description.replace(Format.COUNT, size))
            .appendDescription(override
                ? getLang(MESSAGE_OVERRIDE_ENABLED)
                : getLang(MESSAGE_OVERRIDE_DISABLED))
            .setColor(Constants.ORANGE)
            .build();
    }

    protected MessageEmbed formatSuccessEmbed(int plotID, @NotNull WebhookEntry entry) {
        LanguageFile.EmbedLang lang = getEmbed(EMBED_FETCH_SUCCESS);
        EmbedBuilder embed = new EmbedBuilder().setColor(Constants.GREEN);
        embed.setTitle(lang.title()
            .replace(Format.PLOT_ID, String.valueOf(plotID))
            .replace(Format.THREAD_ID, Long.toUnsignedString(entry.threadID()))
        );
        if(entry.ownerID() != null) embed.setDescription(lang.description()
            .replace(Format.USER_ID, entry.ownerID())
        );
        return embed.build();
    }

    protected enum Button {
        FETCH_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.PLOT_FETCH_CONFIRM, CommandInteractions.BUTTON_CONFIRM),
        FETCH_DISMISS(ButtonStyle.SECONDARY, AvailableButton.PLOT_FETCH_DISMISS, CommandInteractions.BUTTON_DISMISS),
        OVERRIDE_CONFIRM(ButtonStyle.SUCCESS, AvailableButton.PLOT_OVERRIDE_CONFIRM, CommandInteractions.BUTTON_CONFIRM),
        OVERRIDE_CANCEL(ButtonStyle.SECONDARY, AvailableButton.PLOT_OVERRIDE_CANCEL, CommandInteractions.BUTTON_CANCEL),
        CREATE_REGISTER(ButtonStyle.SUCCESS, AvailableButton.PLOT_CREATE_REGISTER, CommandInteractions.BUTTON_CREATE_REGISTER),
        CREATE_UNTRACKED(ButtonStyle.PRIMARY, AvailableButton.PLOT_CREATE_UNTRACKED, CommandInteractions.BUTTON_CREATE_UNTRACKED),
        CREATE_CANCEL(ButtonStyle.SECONDARY, AvailableButton.PLOT_CREATE_CANCEL, CommandInteractions.BUTTON_CANCEL);

        private final CommandButton button;

        Button(ButtonStyle style, AvailableButton type, CommandInteractions label) {
            this.button = payload -> github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button.of(
                    style, type.resolve(payload.eventID, payload.userID), DiscordPS.getSystemLang().get(label)
            );
        }

        public Component get(OnPlotFetch interaction) {
            return button.apply(interaction);
        }
    }
}
