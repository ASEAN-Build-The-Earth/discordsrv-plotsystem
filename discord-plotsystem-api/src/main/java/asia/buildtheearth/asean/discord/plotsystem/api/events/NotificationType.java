package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Plot-related notifications that can be dispatched by the system.
 *
 * <p>Notification messages are managed by the main plugin configuration</p>
 *
 * @see PlotNotificationEvent
 */
public enum NotificationType {
    /** Triggered when a plot is first created or reclaimed */
    ON_CREATED,

    /** Triggered when a plot has been submitted by the user */
    ON_SUBMITTED,

    /** Triggered when a plot has been reviewed (either approved or rejected) */
    ON_REVIEWED,

    /** Triggered when a plot has been explicitly approved */
    ON_APPROVED,

    /** Triggered when a plot has been explicitly rejected */
    ON_REJECTED,

    /** Triggered when a plot's review has been undone */
    ON_UNDO_REVIEW,

    /** Triggered when a submission has been undone */
    ON_UNDO_SUBMIT,

    /** Triggered when a plot is showcased (e.g., highlighted for public viewing) */
    ON_SHOWCASED,

    /** Triggered when a plot is abandoned manually or via system command */
    ON_ABANDONED,

    /**
     * Triggered when a plot is about to be abandoned due to inactivity.
     *
     * <p><strong>This notification is not managed by the API directly</strong> and must be
     * manually triggered by the implementation. It is up to determine how inactivity is tracked
     * and when a plot qualifies as inactive before it becomes abandoned.</p>
     *
     * @deprecated For internal use only.
     * To dispatch an inactivity notification, use {@link InactivityNoticeEvent}.
     */
    @Deprecated(since = "1.2.0")
    @SuppressWarnings("DeprecatedIsStillUsed")
    ON_INACTIVITY
}
