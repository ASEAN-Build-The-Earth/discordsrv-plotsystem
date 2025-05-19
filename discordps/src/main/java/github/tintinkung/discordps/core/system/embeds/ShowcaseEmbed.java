package github.tintinkung.discordps.core.system.embeds;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.AvailableTag;
import github.tintinkung.discordps.core.system.MemberOwnable;
import github.tintinkung.discordps.core.system.PlotData;
import org.jetbrains.annotations.NotNull;

import github.tintinkung.discordps.core.system.layout.InfoComponent;

public class ShowcaseEmbed extends EmbedBuilder implements PlotDataEmbed {

    public ShowcaseEmbed(@NotNull PlotData data, WebhookEntry entry) {
        super();

        String title = InfoComponent.makeTitle(data.getPlot().plotID(), data.getPlot().cityName(), data.getPlot().countryName());

        this.setThumbnail(data.getAvatarAttachmentOrURL());
        this.setTitle(title);
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
        this.setTitle(InfoComponent.makeTitle(plotID, cityName, countryName));
        this.setDescription("Built by "
                + (owner.isOwnerHasDiscord()? owner.getOwnerMentionOrName()
                + " (" + owner.getOwner().getName() + ")"
                : owner.formatOwnerName()));
        this.appendDescription("\nHistories: <#" + threadID + ">");
        this.setColor(AvailableTag.ARCHIVED.getColor());
    }
}
