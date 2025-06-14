package asia.buildtheearth.asean.discord.plotsystem.api.events;

/**
 * Event fired when a plot-related notification is about to be dispatched.
 *
 * <p>Notification messages cannot be overridden at runtime and should instead be configured
 * via the main plugin's configuration file.</p>
 *
 * <p>If additional checks are required before a notification is dispatched,
 * listeners can intercept this event and inspect the {@link NotificationType}.
 * Calling {@link #setCancelled()} will prevent the notification from being sent.
 * However, preferably this should be controlled through the plugin’s config.</p>
 *
 * <p>Note: {@link NotificationType#ON_INACTIVITY} is not managed by the API and
 * will never be dispatched automatically.
 * Refer to {@link InactivityNoticeEvent} to call this event manually.</p>
 *
 * @deprecated This event cannot be called directly with {@link asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI#callEvent(ApiEvent) #callEvent}
 * @see NotificationType
 * @see #setCancelled()
 * @see #isCancelled()
 * @see InactivityNoticeEvent
 */
@Deprecated(since = "1.2.2")
@SuppressWarnings("DeprecatedIsStillUsed")
public class PlotNotificationEvent extends PlotEvent {

    private final NotificationType type;
    private boolean cancelled = false;

    /**
     * Constructs a new {@link PlotNotificationEvent} for the given plot.
     *
     * @param plotID The ID of the plot this notification is related to
     * @param type   The type of notification being sent
     */
    public PlotNotificationEvent(int plotID, NotificationType type) {
        super(plotID);
        this.type = type;
    }

    /**
     * Cancels the notification. If cancelled, the system will not send
     * the associated message.
     */
    public void setCancelled() {
        this.cancelled = true;
    }

    /**
     * Checks whether this notification event has been cancelled.
     *
     * @return {@code true} if the event was cancelled, {@code false} otherwise
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Gets the type of the notification.
     *
     * @return The {@link NotificationType} associated with this event
     */
    public NotificationType getType() {
        return type;
    }
}
