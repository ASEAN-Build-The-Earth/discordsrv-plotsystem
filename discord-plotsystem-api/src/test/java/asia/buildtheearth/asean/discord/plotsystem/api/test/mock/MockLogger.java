package asia.buildtheearth.asean.discord.plotsystem.api.test.mock;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A custom handler to memorize all logged message.
 *
 * <p>Logs is tracked as a simple {@link ArrayList} instance.</p>
 *
 * @see #equals(Collection)
 * @see #popFirst()
 * @see #clear()
 */
public class MockLogger extends Handler {
    private final List<String> log = new ArrayList<>();

    @Override
    public void publish(@NotNull LogRecord record) {
        this.log.add(record.getMessage());
    }

    /**
     * Check equality of the current logged messages.
     *
     * @param messages Collection of messages assuming that it is {@link List#equals(Object)} to the logged list.
     * @return True if the comparison returns true.
     * @see List#equals(Object)
     */
    public boolean equals(@NotNull Collection<String> messages) {
        return this.log.equals(messages);
    }

    /**
     * Remove the first logged element and return it.
     *
     * @return The first logged message.
     * @see List#removeFirst()
     */
    public String popFirst() {
        return this.log.removeFirst();
    }

    /**
     * Clear all logged message.
     *
     * @see List#clear()
     */
    public void clear() {
        this.log.clear();
    }

    @Override public void flush() {}
    @Override public void close() throws SecurityException {}
}
