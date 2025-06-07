package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.plotsystem.commands.providers.PlotCommandProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotShowcaseCommand;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.BuildTeamLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.events.PlotShowcaseEvent;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnPlotShowcase;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableButton;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.stream.Collectors;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotShowcaseCommand.*;

abstract sealed class AbstractPlotShowcaseCommand
        extends PlotCommandProvider<OnPlotShowcase, PlotShowcaseCommand>
        implements PlotShowcaseEvent
        permits asia.buildtheearth.asean.discord.plotsystem.commands.PlotShowcaseCommand {

    public AbstractPlotShowcaseCommand(@NotNull String name) {
        super(name);
    }

    @Override
    protected String[] getLangArgs() {
        return new String[] { Format.OWNER, Format.THREAD_ID };
    }

    protected Button getConfirmButton(@NotNull OnPlotShowcase payload) {
        return Button.success(
            AvailableButton.PLOT_SHOWCASE_CONFIRM.resolve(payload.eventID, payload.userID),
            getLangManager().get(CommandInteractions.BUTTON_CONFIRM)
        );
    }

    protected Button getCancelButton(@NotNull OnPlotShowcase payload) {
        return Button.secondary(
            AvailableButton.PLOT_SHOWCASE_CANCEL.resolve(payload.eventID, payload.userID),
            getLangManager().get(CommandInteractions.BUTTON_CANCEL)
        );
    }

    @Override
    protected abstract void onCommandTriggered(InteractionHook hook, OnPlotShowcase payload);

    protected @NotNull MessageEmbed formatShowcaseInfo(@NotNull PlotData plotData) {

        String mediaFiles = plotData
            .getImageFiles()
            .stream()
            .map(File::getName)
            .collect(Collectors.joining(", "));

        String owner = plotData.isOwnerHasDiscord()
            ? plotData.getOwnerMentionOrName() + " (" + plotData.getOwner().getName() + ")"
            : plotData.formatOwnerName();

        String countryCode = plotData.getPlot().countryCode();
        String cityProjectID = plotData.getPlot().cityProjectID();
        String country = DiscordPS.getMessagesLang().get(BuildTeamLang.getCountry().getName(countryCode), countryCode);
        String city = DiscordPS.getMessagesLang().get(BuildTeamLang.getCityProject().getName(cityProjectID), cityProjectID);


        return getLangManager().getEmbedBuilder(EMBED_SHOWCASE_INFO)
            .setColor(Constants.GREEN)
            .setThumbnail(plotData.getAvatarAttachmentOrURL())
            // Showcase Title
            .addField(getLang(MESSAGE_PLOT_TITLE),
                "```" +
                DiscordPS.getMessagesLang().get(PlotInformation.SHOWCASE_TITLE)
                    .replace(Format.OWNER, owner)
                    .replace(Format.PLOT_ID, String.valueOf(plotData.getPlot().plotID()))
                    .replace(Format.COUNTRY, country)
                    .replace(Format.CITY, city)
                + "```"
                , false)
            // Showcase Owner Name
            .addField(getLang(MESSAGE_OWNER),
                owner, false)
            // Showcase Owner Avatar
            .addField(getLang(MESSAGE_OWNER_AVATAR),
                plotData.getAvatarAttachmentOrURL(), false)
            // Attached Image File(s)
            .addField(getLang(MESSAGE_IMAGE_FILES),
                mediaFiles, false)
            // Display coordinate location
            .addField(getLang(MESSAGE_LOCATION),
                plotData.getDisplayCords(), false)
            // Google Map Link (via button)
            .addField(getLang(MESSAGE_GOOGLE_MAP_LINK),
                "https://www.google.com/maps/place/" + plotData.getGeoCoordinates(), false)
            .build();
    }

    protected MessageEmbed formatSuccessfulEmbed(@NotNull MessageReference message) {
        return new EmbedBuilder()
            .setTitle(getLang(MESSAGE_SHOWCASED_TO)
                .replace(Format.THREAD_ID, message.getMessageId()))
            .addField(getLang(MESSAGE_THREAD_ID),
                "```" + message.getMessageId() + "```",
                false)
            .setColor(Constants.GREEN)
            .build();
    }
}