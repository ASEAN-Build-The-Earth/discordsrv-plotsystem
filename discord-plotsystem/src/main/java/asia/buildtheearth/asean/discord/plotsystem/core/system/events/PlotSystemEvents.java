package asia.buildtheearth.asean.discord.plotsystem.core.system.events;

import com.alpsbte.plotsystem.api.model.PlotAbandonReason;

import java.time.Instant;

/**
 * Internal Discord-side plot update commands.
 *
 * <p>Not a public extension API. Plot-System supplies lifecycle truth via Bukkit events;
 * this plugin maps those into webhook/history actions here.</p>
 */
public final class PlotSystemEvents {
    private PlotSystemEvents() {}

    public enum NotificationType {
        ON_CREATED,
        ON_SUBMITTED,
        ON_REVIEWED,
        ON_APPROVED,
        ON_REJECTED,
        ON_UNDO_REVIEW,
        ON_UNDO_SUBMIT,
        ON_SHOWCASED,
        ON_ABANDONED,
        ON_INACTIVITY
    }

    public abstract static class PlotEvent {
        private final int plotID;

        protected PlotEvent(int plotID) {
            this.plotID = plotID;
        }

        public int getPlotID() {
            return plotID;
        }
    }

    public abstract static class PlotReviewEvent extends PlotEvent {
        protected PlotReviewEvent(int plotID) {
            super(plotID);
        }
    }

    public abstract static class PlotUndoEvent extends PlotEvent {
        protected PlotUndoEvent(int plotID) {
            super(plotID);
        }
    }

    public abstract static class PlotClosureEvent extends PlotEvent {
        protected PlotClosureEvent(int plotID) {
            super(plotID);
        }
    }

    public static final class PlotCreateEvent extends PlotEvent {
        private final PlotSnapshotData data;

        public PlotCreateEvent(int plotID, PlotSnapshotData data) {
            super(plotID);
            this.data = data;
        }

        public PlotSnapshotData getData() {
            return data;
        }
    }

    public static final class PlotFeedbackEvent extends PlotReviewEvent {
        private final String feedback;

        public PlotFeedbackEvent(int plotID, String feedback) {
            super(plotID);
            this.feedback = feedback;
        }

        public String getFeedback() {
            return feedback;
        }
    }

    public static final class PlotApprovedEvent extends PlotReviewEvent {
        public PlotApprovedEvent(int plotID) {
            super(plotID);
        }
    }

    public static final class PlotRejectedEvent extends PlotReviewEvent {
        public PlotRejectedEvent(int plotID) {
            super(plotID);
        }
    }

    public static final class PlotAbandonedEvent extends PlotClosureEvent {
        private final PlotAbandonReason reason;

        public PlotAbandonedEvent(int plotID, PlotAbandonReason reason) {
            super(plotID);
            this.reason = reason;
        }

        public PlotAbandonReason getReason() {
            return reason;
        }
    }

    public static final class PlotSubmitEvent extends PlotEvent {
        public PlotSubmitEvent(int plotID) {
            super(plotID);
        }
    }

    public static final class PlotUndoSubmitEvent extends PlotUndoEvent {
        public PlotUndoSubmitEvent(int plotID) {
            super(plotID);
        }
    }

    public static final class PlotUndoReviewEvent extends PlotUndoEvent {
        public PlotUndoReviewEvent(int plotID) {
            super(plotID);
        }
    }

    public static final class InactivityNoticeEvent extends PlotEvent {
        private final Instant lastActivityAt;
        private final Instant abandonmentAt;
        private final int inactivityDays;

        public InactivityNoticeEvent(
                int plotID,
                Instant lastActivityAt,
                Instant abandonmentAt,
                int inactivityDays
        ) {
            super(plotID);
            this.lastActivityAt = lastActivityAt;
            this.abandonmentAt = abandonmentAt;
            this.inactivityDays = inactivityDays;
        }

        public Instant getLastActivityAt() {
            return lastActivityAt;
        }

        public Instant getAbandonmentAt() {
            return abandonmentAt;
        }

        public int getInactivityDays() {
            return inactivityDays;
        }
    }

    public static final class PlotArchiveEvent extends PlotClosureEvent {
        private final String owner;

        public PlotArchiveEvent(int plotID, String owner) {
            super(plotID);
            this.owner = owner;
        }

        public String getOwner() {
            return owner;
        }
    }

    public static final class PlotReclaimEvent extends PlotEvent {
        private final String owner;

        public PlotReclaimEvent(int plotID, String owner) {
            super(plotID);
            this.owner = owner;
        }

        public String getOwner() {
            return owner;
        }
    }
}
