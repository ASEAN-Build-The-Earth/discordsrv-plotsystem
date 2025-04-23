package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.utils.BuilderUser;
import github.tintinkung.discordps.utils.CoordinatesUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlotData {

    private final OfflinePlayer owner;

    private final @Nullable Member ownerDiscord;

    private final PlotEntry plot;
    private final ThreadStatus status;
    private final String geoCoordinates;
    private final String displayCords;
    private final Set<Long> statusTags;

    private @Nullable EmbedBuilder embed = null;

    public PlotData(@NotNull PlotEntry plot) {
        this.plot = plot;
        this.owner = Bukkit.getOfflinePlayer(UUID.fromString(plot.ownerUUID()));
        this.ownerDiscord = BuilderUser.getAsDiscordMember(owner);

        String[] mcLocation = plot.mcCoordinates().split(",");

        double xCords = Double.parseDouble(mcLocation[0].trim());
        double zCords = Double.parseDouble(mcLocation[2].trim());
        double[] geoCords = CoordinatesUtil.convertToGeo(xCords, zCords);

        this.geoCoordinates = CoordinatesUtil.formatGeoCoordinatesNumeric(geoCords);
        this.displayCords = CoordinatesUtil.formatGeoCoordinatesNSEW(geoCords);

        status = ThreadStatus.toPlotStatus(plot.status());
        String tagID = status.toTag().getTag().getID();
        Checks.isSnowflake(tagID, "Forum Tag ID");
        statusTags = Set.of(Long.parseUnsignedLong(tagID));
    }

    public EmbedBuilder prepareEmbed() {
        String owner = isOwnerHasDiscord()? getOwnerDiscord().orElseThrow().getAsMention() : this.owner.getName();

        return embed = new EmbedBuilder()
                .setTitle("Plot by " + owner)
                .addField(makeDescriptionField(plot.plotID(), plot.cityName(), plot.countryName(), getDisplayCords()))
                .addField(makeStatusField(status))
                .setColor(getStatus().toTag().getColor());
    }

    /**
     * Clone an embed from a previously sent plot data.
     * @param fromEmbed The previous embed, must be formatted the same as {@link #prepareEmbed()}
     * @return A new embed builder without status field, thumbnail data and embed color.
     */
    public static @NotNull EmbedBuilder fromEmbed(@NotNull MessageEmbed fromEmbed) {
        MessageEmbed.AuthorInfo author = fromEmbed.getAuthor();
        MessageEmbed.Field descField = fromEmbed.getFields().getFirst();

        if(author == null) throw new IllegalArgumentException("Trying to restore webhook embed with null author");

        return new EmbedBuilder()
                .setTitle(fromEmbed.getTitle())
                .setAuthor(author.getName(), null, author.getIconUrl())
                .addField(descField);
    }

    /**
     * Format a plot status field for message embed, will display as:
     * @param status The thread status of this plot
     * @return {@link MessageEmbed.Field new field}
     */
    @Contract("_ -> new")
    public static @NotNull MessageEmbed.Field makeStatusField(ThreadStatus status) {
        return new MessageEmbed.Field(getDisplayStatus(status), getDisplayDetail(status), true);
    }

    /**
     *
     * Format a plot description field for message embed.
     * <p>Example display:</p>
     * <blockquote>
     *    <p>🏠 Plot #1 at Bangkok, Thailand</p>
     *    <p>{@code
     *    3°2'52.82"N 101°27'17.452"E
     *    }</p>
     * </blockquote>
     * @return {@link MessageEmbed.Field new field}
     */
    @Contract("_, _, _, _ -> new")
    public static @NotNull MessageEmbed.Field makeDescriptionField(
            int plotID,
            @NotNull String city,
            @NotNull String country,
            @NotNull String geoCoordinates) {
        return new MessageEmbed.Field(
                ":house: Plot #" + plotID + " at "
                    + String.join(", ", country, city),
                "```" + geoCoordinates + "```",
                true
        );
    }

    public static String getDisplayStatus(ThreadStatus status) {
        return switch (status) {
            case on_going -> ":white_circle: On Going";
            case finished -> ":yellow_circle: Submitted";
            case rejected -> ":red_circle: Rejected";
            case approved -> ":green_circle: Approved";
            case archived -> ":blue_circle: Archived";
            case abandoned -> ":white_circle: Abandoned";
        };
    }

    public static String getDisplayDetail(ThreadStatus status) {
        return switch (status) {
            case on_going -> null;
            case finished -> "Please wait for staff to review this plot.";
            case rejected -> "This plot is rejected, please make changes given my our staff team and re-submit this plot.";
            case approved -> "Plot is completed and staff has approved this plot.";
            case archived -> "The plot has been marked as archived.";
            case abandoned -> "The user has abandoned their plot, anyone can re-claim this plot.";
        };
    }

    public OfflinePlayer getOwner() {
        return this.owner;
    }

    public boolean isOwnerHasDiscord() {
        return this.ownerDiscord != null;
    }

    public Optional<Member> getOwnerDiscord() {
        return Optional.ofNullable(this.ownerDiscord);
    }

    public ThreadStatus getStatus() {
        return this.status;
    }

    public Set<Long> getStatusTags() {
        return this.statusTags;
    }

    public Optional<EmbedBuilder> getEmbed() {
        return Optional.ofNullable(this.embed);
    }

    public String getDisplayCords() {
        return displayCords;
    }

    public String getGeoCoordinates() {
        return geoCoordinates;
    }

    public PlotEntry getPlot() {
        return plot;
    }
}
