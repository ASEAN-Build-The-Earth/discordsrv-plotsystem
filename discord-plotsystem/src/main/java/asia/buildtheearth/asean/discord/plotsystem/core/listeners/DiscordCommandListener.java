package asia.buildtheearth.asean.discord.plotsystem.core.listeners;

import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.commands.ReviewCommand;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotFetchCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionMapping;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.PlotCommand;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.SetupWebhookEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.*;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.providers.DiscordCommandProvider;
import asia.buildtheearth.asean.discord.plotsystem.commands.SetupCommand;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.interactions.ReplyAction;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

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

    private static final String SLASH_PLOT_ARCHIVE =  PlotCommand.PLOT_CTL + "/" + PlotCommand.ARCHIVE;
    private static final String SLASH_PLOT_FETCH =  PlotCommand.PLOT_CTL + "/" + PlotCommand.FETCH;
    private static final String SLASH_PLOT_DELETE =  PlotCommand.PLOT_CTL + "/" + PlotCommand.DELETE;
    private static final String SLASH_PLOT_SHOWCASE =  PlotCommand.PLOT_CTL + "/" + PlotCommand.SHOWCASE;

    private static final String SLASH_REVIEW_EDIT =  ReviewCommand.REVIEW + "/" + ReviewCommand.EDIT;
    private static final String SLASH_REVIEW_SEND =  ReviewCommand.REVIEW + "/" + ReviewCommand.SEND;
    private static final String SLASH_REVIEW_ARCHIVE =  ReviewCommand.REVIEW + "/" + ReviewCommand.ARCHIVE;

    private final DiscordPS plugin;

    /**
     * Initialize discord command listener with the plugin.
     *
     * @param plugin The plugin instance to register the listener with.
     */
    public DiscordCommandListener(DiscordPS plugin) {
        this.plugin = plugin;
    }

    /**
     * Remove command data from getting listen to
     * including its interactions.
     */
    @Override
    public void clearCommands() {
        super.clearCommands();
        super.clearInteractions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean ifUnknownChannel(@NotNull ReplyAction replyAction) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Plugin getPlugin() {
        return this.plugin;
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
                SetupCommand.WEBHOOK_YML
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
                SetupCommand.SHOWCASE_YML
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
     * Entry point for {@code /review edit} command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_REVIEW_EDIT)
    public void onReviewEdit(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, true, ReviewCommand.class, ReviewCommand::getEditCommand,
            () -> new OnReview(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(ReviewCommand.PLOT_ID)).getAsLong()
            )
        );
    }

    /**
     * Entry point for {@code /review send} command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_REVIEW_SEND)
    public void onReviewSend(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, true, ReviewCommand.class, ReviewCommand::getSendCommand,
            () -> new OnReview(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(ReviewCommand.PLOT_ID)).getAsLong()
            )
        );
    }

    /**
     * Entry point for {@code /review archive} command
     *
     * @param event Slash command event activated by JDA
     */
    @SlashCommand(path = SLASH_REVIEW_ARCHIVE)
    public void onReviewArchive(@NotNull SlashCommandEvent event) {
        if(requiredReady(event)) return;

        this.onSlashCommand(event, false, ReviewCommand.class, ReviewCommand::getArchiveCommand,
            () -> new OnPlotArchive(
                event.getUser().getIdLong(),
                event.getIdLong(),
                Objects.requireNonNull(event.getOption(PlotCommand.PLOT_ID)).getAsLong(),
                Objects.requireNonNull(event.getOption(PlotCommand.PLOT_OVERRIDE)).getAsBoolean()
            )
        );
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

                // Send warning message if more than one status is picked
                if(initialStatus != null) {
                    String pickedStatus = initialStatus.name();
                    EmbedBuilder embed =  DiscordPS.getSystemLang().getEmbedBuilder(
                        PlotFetchCommand.EMBED_PICKED_MORE_STATUS,
                        description -> description.replace(Format.LABEL, pickedStatus));

                    event.getHook()
                        .sendMessageEmbeds(embed.setColor(Constants.ORANGE).build())
                        .setEphemeral(true)
                        .queue();
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
            EmbedBuilder embed =  DiscordPS.getSystemLang()
                .getEmbedBuilder(CommandInteractions.EMBED_PLUGIN_NOT_READY);
            event.deferReply(true).addEmbeds(embed.setColor(Constants.RED).build()).queue();
            return true;
        }
        else return false;
    }
}
