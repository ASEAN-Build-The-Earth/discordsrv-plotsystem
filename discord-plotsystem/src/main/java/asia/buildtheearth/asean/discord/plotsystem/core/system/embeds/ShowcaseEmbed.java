package asia.buildtheearth.asean.discord.plotsystem.core.system.embeds;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;
import asia.buildtheearth.asean.discord.plotsystem.core.system.MemberOwnable;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import org.jetbrains.annotations.NotNull;

import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.InfoComponent;

@Deprecated
public class ShowcaseEmbed extends EmbedBuilder implements PlotDataEmbed {

    public ShowcaseEmbed(@NotNull PlotData data, WebhookEntry entry) {
        super();

        // String title = InfoComponent.makeTitle(data.getPlot().plotID(), data.getPlot().cityName(), data.getPlot().countryName());

        this.setThumbnail(data.getAvatarAttachmentOrURL());
        // this.setTitle(title);
        this.setDescription("Built by "
            + (data.isOwnerHasDiscord()? data.getOwnerMentionOrName()
            + " (" + data.getOwner().getName() + ")"
            : data.formatOwnerName()));
        this.appendDescription("\nTracker thread: " + entry.ownerID());
        this.setColor(AvailableTag.ARCHIVED.getColor());
    }

    public ShowcaseEmbed(int plotID,
                         @NotNull MemberOwnable owner,
                         @NotNull String cityName,
                         @NotNull String countryName,
                         @NotNull String threadID) {
        super();

        this.setThumbnail(owner.getAvatarAttachmentOrURL());
        // this.setTitle(InfoComponent.makeTitle(plotID, cityName, countryName));
        this.setDescription("Built by "
                + (owner.isOwnerHasDiscord()? owner.getOwnerMentionOrName()
                + " (" + owner.getOwner().getName() + ")"
                : owner.formatOwnerName()));
        this.appendDescription("\nHistories: <#" + threadID + ">");
        this.setColor(AvailableTag.ARCHIVED.getColor());
    }
}
