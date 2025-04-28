package github.tintinkung.discordps.core.system.embeds;

import github.scarsz.discordsrv.dependencies.commons.io.FilenameUtils;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.api.events.*;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.system.PlotData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Create a plot information message embed.
 *
 * <p>Example display:</p>
 * <b>🏠 Plot #1 (Bangkok, Thailand)</b>
 * <p>{@code
 *    3°2'52.82"N 101°27'17.452"E
 * }</p>
 * <b>☰ Plot History</b>
 * <p>◆︎ @user created yesterday</p>
 */
@Deprecated
public class InfoEmbed implements PlotDataEmbed {
    public static final String HISTORY_FIELD_NAME = ":bookmark: Plot History";

    private final String title;
    private final String description;
    private final StringBuilder histories;
    private final EmbedBuilder embed;

    public InfoEmbed(@NotNull PlotData data) {
        // Setup fields
        this.embed = new EmbedBuilder();
        this.title = makeTitle(data.getPlot().plotID(), data.getPlot().cityName(), data.getPlot().countryName());
        this.description = makeDescription(data.getDisplayCords());
        this.histories = new StringBuilder();

        // Determine owner
        data.getOwnerDiscord().ifPresentOrElse(
                (owner) -> embed.setAuthor(data.formatOwnerName(owner), null, owner.getEffectiveAvatarUrl()),
                () -> embed.setAuthor(data.getOwner().getName())
        );

        // Figure out builder avatar
        data.getAvatarFile().ifPresentOrElse(
                (file) -> embed.setThumbnail("attachment://" + file.getName()),
                () -> embed.setThumbnail(data.getAvatarURL().toString())
        );

        embed.setColor(data.getStatus().toTag().getColor());
    }

    public InfoEmbed(String title, String description, StringBuilder histories, EmbedBuilder embed) {
        this.embed = embed;
        this.title = title;
        this.description = description;
        this.histories = histories;
    }

    @Contract("_ -> new")
    public static @NotNull InfoEmbed from(@NotNull MessageEmbed embed) {
        EmbedBuilder from = new EmbedBuilder();
        List<MessageEmbed.Field> fields = embed.getFields();
        MessageEmbed.Thumbnail thumbnail = embed.getThumbnail();
        StringBuilder prevHistory = new StringBuilder();

        from.copyFrom(embed);

        // Retrieve stored avatar image in the player's media path
        if(thumbnail != null) {
            if(FilenameUtils.getBaseName(thumbnail.getUrl()).equals("avatar-image"))
                from.setThumbnail("attachment://" + FilenameUtils.getName(thumbnail.getUrl()));
            else if(FilenameUtils.getBaseName(thumbnail.getProxyUrl()).equals("avatar-image"))
                from.setThumbnail("attachment://" + FilenameUtils.getName(thumbnail.getProxyUrl()));
        }

        fields.forEach(field -> {
            if(field.getName() != null && field.getName().equals(HISTORY_FIELD_NAME))
                prevHistory.append(field.getValue());
        });

        from.clearFields();

        return new InfoEmbed(embed.getTitle(), embed.getDescription(), prevHistory, from);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setColor(Color color) {
        this.embed.setColor(color);
    }

    private void appendHistories(@Nullable String history) {
        if(history == null || StringUtils.isBlank(history)) return;
        if(!histories.isEmpty()) histories.append("\n");
        histories.append(":small_blue_diamond: ").append(history);
    }

    /**
     * Add new history message into the embed's history field.
     * @param message The history message to add to
     */
    public void addHistory(@NotNull String message) {
        appendHistories(message);
    }

    /**
     * Format a message and add to history field by event.
     * @param event The occurred event.
     */
    public <T extends PlotEvent> void addHistory(@NotNull T event) {
        switch (event) {
            case PlotCreateEvent    ignored -> addHistoryFormatted("Plot is claimed and under construction.");
            case PlotSubmitEvent    ignored -> addHistoryFormatted("Plot submitted and awaiting review.");
            case PlotApprovedEvent  ignored -> addHistoryFormatted("Plot has been approved");
            case PlotRejectedEvent  ignored -> addHistoryFormatted("Plot has been rejected");
            case PlotAbandonedEvent ignored -> addHistoryFormatted("Plot has been abandoned by the builder.");
            default -> appendHistories(null);
        }
    }

    /**
     * Add history message to this info embed with this format:
     * <p><b>20/04/2021</b> • {message}</p>
     * @param message The history message to be added.
     */
    private void addHistoryFormatted(String message) {
        appendHistories("<t:" + Instant.now().getEpochSecond() + ":d> • " + message);
    }

    private static String makeTitle(int plotID, @NotNull String city, @NotNull String country) {
        return ":house: Plot #" + plotID + " (" + String.join(", ", country, city) + ")";
    }

    private static String makeDescription(@NotNull String geoCoordinates) {
        return "```" + geoCoordinates + "```";
    }

    private static @NotNull MessageEmbed.Field makeHistoryField(@NotNull String histories) {
        return new MessageEmbed.Field(HISTORY_FIELD_NAME, histories, false);
    }

    @Override
    public  @NotNull MessageEmbed build() {
        embed.setTitle(this.title);
        embed.setDescription(this.description);

        if(!histories.isEmpty())
            embed.addField(makeHistoryField(this.histories.toString()));

        return embed.build();
    }
}
