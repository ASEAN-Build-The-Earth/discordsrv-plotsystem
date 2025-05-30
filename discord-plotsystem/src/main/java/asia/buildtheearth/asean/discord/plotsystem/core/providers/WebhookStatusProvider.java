package asia.buildtheearth.asean.discord.plotsystem.core.providers;

import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;

/**
 * Defines a mapping between {@link ThreadStatus} and {@link AvailableTag}
 * for webhook-related status data.
 *
 * <p>By convention, {@link ThreadStatus} uses snake_case lowercase, and
 * {@link AvailableTag} uses SNAKE_CASE uppercase. Implementations must make sure these are in sync.</p>
 *
 * <blockquote>
 * {@snippet :
 *public class Provider<T> extends Enum<T> implements WebhookStatusProvider {
 *
 *    @Override
 *    public ThreadStatus toStatus() {
 *        // Lowercase association
 *        return valueOf(ThreadStatus.class, name().toLowerCase());
 *    }
 *
 *    @Override
 *    public AvailableTag toTag() {
 *        // Uppercase association
 *        return valueOf(AvailableTag.class, name().toUpperCase());
 *    }
 *}
 * }</blockquote>
 */
public interface WebhookStatusProvider {

    /**
     * Returns the corresponding {@link ThreadStatus} that represents the raw webhook status.
     *
     * @return the associated {@link ThreadStatus}
     */
    ThreadStatus toStatus();

    /**
     * Returns the primary {@link AvailableTag} that represents this webhook status.
     *
     * @return the associated {@link AvailableTag}
     */
    AvailableTag toTag();
}