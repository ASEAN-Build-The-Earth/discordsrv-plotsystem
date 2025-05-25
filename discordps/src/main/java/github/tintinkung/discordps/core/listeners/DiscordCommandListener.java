package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionMapping;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.PlotCommand;
import github.tintinkung.discordps.commands.events.PlotFetchEvent;
import github.tintinkung.discordps.commands.events.SetupWebhookEvent;
import github.tintinkung.discordps.commands.interactions.*;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.providers.DiscordCommandProvider;
import github.tintinkung.discordps.commands.SetupCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.EventListener;
import java.util.Objects;

/**
 * Discord slash command listener
 *
 */
@SuppressWarnings("unused")
final public class DiscordCommandListener extends DiscordCommandProvider implements EventListener {
    private static final String SLASH_SETUP_WEBHOOK =  SetupCommand.SETUP + "/" + SetupCommand.WEBHOOK;
    private static final String SLASH_SETUP_SHOWCASE =  SetupCommand.SETUP + "/" + SetupCommand.SHOWCASE;
    private static final String SLASH_SETUP_CHECKLIST =  SetupCommand.SETUP + "/" + SetupCommand.HELP;

    private static final String SLASH_PLOT_ARCHIVE =  PlotCommand.PLOT + "/" + PlotCommand.ARCHIVE;
    private static final String SLASH_PLOT_FETCH =  PlotCommand.PLOT + "/" + PlotCommand.FETCH;
    private static final String SLASH_PLOT_DELETE =  PlotCommand.PLOT + "/" + PlotCommand.DELETE;
    private static final String SLASH_PLOT_SHOWCASE =  PlotCommand.PLOT + "/" + PlotCommand.SHOWCASE;

    public DiscordCommandListener(DiscordPS plugin) {
        super(plugin);
    }

    /**
     * Remove command data from getting listen to
     * including its interactions.
     * {@inheritDoc}
     */
    @Override
    public void clearCommands() {
        super.clearCommands();
        super.clearInteractions();
    }

    /**
     * Entry point for {@code /setup webhook} command
     *
     * @param event Slash command event activated by JDA
     * @see SetupWebhookEvent#onSetupWebhook(InteractionHook, OnSetupWebhook) Event handler
     */
    @SlashCommand(path = SLASH_SETUP_WEBHOOK)
    public void onSetupWebhook(@NotNull SlashCommandEvent event) {
        this.onSlashCommand(event, false, SetupCommand.class, SetupCommand::getWebhookCommand,
            () -> new OnSetupWebhook(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(SetupCommand.WEBHOOK_NAME)).getAsString(),
                Objects.requireNonNull(event.getOption(SetupCommand.WEBHOOK_CHANNEL)).getAsLong(),
                "webhook.yml"
            )
        );
    }

    /**
     * Entry point for {@code /setup showcase} command
     *
     * @param event Slash command event activated by JDA
     * @see SetupWebhookEvent#onSetupWebhook(InteractionHook, OnSetupWebhook) Event handler
     */
    @SlashCommand(path = SLASH_SETUP_SHOWCASE)
    public void onSetupShowcase(@NotNull SlashCommandEvent event) {
        this.onSlashCommand(event, false, SetupCommand.class, SetupCommand::getShowcaseCommand,
            () -> new OnSetupWebhook(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(SetupCommand.WEBHOOK_NAME)).getAsString(),
                Objects.requireNonNull(event.getOption(SetupCommand.WEBHOOK_CHANNEL)).getAsLong(),
                "showcase.yml"
            )
        );
    }

    /**
     * Entry point for setup checklist command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_SETUP_CHECKLIST)
    public void onSetupHelp(@NotNull SlashCommandEvent event) {
        this.onSlashCommand(event, true, SetupCommand.class, SetupCommand::getHelpCommand);
    }

    /**
     * Entry point for plot archive command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_PLOT_ARCHIVE)
    public void onPlotArchive(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, false, PlotCommand.class, PlotCommand::getArchiveCommand,
            () -> new OnPlotArchive(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(PlotCommand.PLOT_ID)).getAsLong(),
                Objects.requireNonNull(event.getOption(PlotCommand.PLOT_OVERRIDE)).getAsBoolean()
            )
        );
    }


    /**
     * Entry point for plot delete command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_PLOT_DELETE)
    public void onPlotDelete(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, true, PlotCommand.class, PlotCommand::getDeleteCommand, () ->
            new OnPlotDelete(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(PlotCommand.PLOT_ID)).getAsLong()
            )
        );
    }

    /**
     * Entry point for plot showcase command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_PLOT_SHOWCASE)
    public void onPlotShowcase(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, true, PlotCommand.class, PlotCommand::getShowcaseCommand, () ->
            new OnPlotShowcase(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(PlotCommand.PLOT_ID)).getAsLong()
            )
        );
    }

    /**
     * Entry point for plot fetch command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_PLOT_FETCH)
    public void onPlotFetch(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, true, PlotCommand.class, PlotCommand::getFetchCommand, () -> {
            long plotID = Objects.requireNonNull(event.getOption(PlotCommand.PLOT_ID)).getAsLong();
            boolean override = Objects.requireNonNull(event.getOption(PlotCommand.PLOT_OVERRIDE)).getAsBoolean();

            ThreadStatus initialStatus = null;

            for(ThreadStatus status : ThreadStatus.VALUES) {
                OptionMapping option = event.getOption(status.name());

                if(option == null) continue;

                if(initialStatus != null) {
                    PlotFetchEvent.onManyStatusPicked(event.getHook(), initialStatus.name());
                    break;
                }
                initialStatus = status;
            }

            return new OnPlotFetch(
                    event.getUser().getIdLong(),
                    event.getIdLong(),
                    plotID,
                    override,
                    initialStatus
            );
        });
    }


    /**
     * If guard to exit slash command early
     * if the command required plugin to be ready first.
     *
     * @param event The triggered slash command event
     * @return True if the plugin is NOT ready
     * @see DiscordPS#isReady()
     */
    public boolean requiredReady(SlashCommandEvent event) {
        if(!DiscordPS.getPlugin().isReady()) {
            event.deferReply(true).addEmbeds(new EmbedBuilder()
                .setTitle("Discord Plot-System is **NOT** Ready")
                .setDescription("Cannot use plugin related command, "
                        + "please set up the plugin first with `/setup webhook` (`/setup help` for more info)")
                .setColor(Color.RED)
                .build())
            .queue();

            return true;
        }
        else return false;
    }
}
