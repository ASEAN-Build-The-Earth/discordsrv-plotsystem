package github.tintinkung.discordps.core.system.embeds;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.tintinkung.discordps.core.database.ThreadStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class StatusEmbed extends EmbedBuilder implements PlotDataEmbed {
    public StatusEmbed(@NotNull ThreadStatus status) {
        super();

        this.setTitle(getDisplayStatus(status));
        this.setDescription(getDisplayDetail(status));
        this.setColor(status.toTag().getColor());
        this.setFooter("Updated");
        this.setTimestamp(Instant.now());
    }

    public StatusEmbed(@NotNull MessageEmbed from) {
        super();

        this.copyFrom(from);
    }

    @Contract(pure = true)
    private static @NotNull String getDisplayStatus(@NotNull ThreadStatus status) {
        return switch (status) {
            case on_going -> ":white_circle: On Going";
            case finished -> ":yellow_circle: Submitted";
            case rejected -> ":red_circle: Rejected";
            case approved -> ":green_circle: Approved";
            case archived -> ":blue_circle: Archived";
            case abandoned -> ":white_circle: Abandoned";
        };
    }

    @Contract(pure = true)
    private static @NotNull String getDisplayDetail(@NotNull ThreadStatus status) {
        return switch (status) {
            case on_going -> "The plot is under construction.";
            case finished -> "Please wait for staff to review this plot.";
            case rejected -> "This plot is rejected, please make changes given my our staff team and re-submit this plot.";
            case approved -> "Plot is completed and staff has approved this plot.";
            case archived -> "The plot has been marked as archived.";
            case abandoned -> "The user has abandoned their plot, anyone can re-claim this plot.";
        };
    }
}
