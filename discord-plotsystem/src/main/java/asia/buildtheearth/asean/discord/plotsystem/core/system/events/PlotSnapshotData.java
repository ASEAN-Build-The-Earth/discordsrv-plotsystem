package asia.buildtheearth.asean.discord.plotsystem.core.system.events;

import com.alpsbte.plotsystem.api.model.PlotSnapshot;
import com.alpsbte.plotsystem.api.model.PlotReviewOutcome;
import com.alpsbte.plotsystem.api.model.PlotStatus;

public record PlotSnapshotData(
    int plotID,
    String ownerUUID,
    PlotStatusValue status,
    String cityProjectID,
    String countryCode,
    double[] geoCoordinates
) {
    public enum PlotStatusValue {
        ON_GOING("on_going"),
        FINISHED("finished"),
        REJECTED("rejected"),
        APPROVED("approved"),
        ARCHIVED("archived"),
        ABANDONED("abandoned");

        private final String name;

        PlotStatusValue(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static PlotSnapshotData from(PlotSnapshot plot) {
        return from(plot, toStatus(plot));
    }

    public static PlotSnapshotData from(PlotSnapshot plot, PlotStatusValue status) {
        return new PlotSnapshotData(
            plot.id(),
            plot.owner() == null ? null : plot.owner().toString(),
            status,
            plot.cityProjectId(),
            plot.countryCode(),
            plot.longitude() == null || plot.latitude() == null
                ? null
                : new double[] { plot.longitude(), plot.latitude() }
        );
    }

    private static PlotStatusValue toStatus(PlotStatus status) {
        return switch (status) {
            case UNCLAIMED -> PlotStatusValue.ABANDONED;
            case UNFINISHED -> PlotStatusValue.ON_GOING;
            case UNREVIEWED -> PlotStatusValue.FINISHED;
            case COMPLETED -> PlotStatusValue.APPROVED;
        };
    }

    private static PlotStatusValue toStatus(PlotSnapshot plot) {
        if(plot.status() == PlotStatus.COMPLETED && plot.reviewOutcome() == PlotReviewOutcome.REJECTED) {
            return PlotStatusValue.REJECTED;
        }
        return toStatus(plot.status());
    }
}
