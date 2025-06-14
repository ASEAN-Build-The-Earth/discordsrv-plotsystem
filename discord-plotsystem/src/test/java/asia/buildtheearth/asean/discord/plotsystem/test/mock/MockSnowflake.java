package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import github.scarsz.discordsrv.dependencies.jda.api.utils.TimeUtil;

import java.util.concurrent.ThreadLocalRandom;


public enum MockSnowflake {
    UNKNOWN(0),
    /** Main Discord Guild */
    MAIN_GUILD(1),
    /** Main webhook channel */
    WEBHOOK_CHANNEL_ID(2),
    /** Main webhook ID */
    WEBHOOK_ID(3),
    /** Showcase webhook channel */
    SHOWCASE_CHANNEL_ID(4),
    /** Showcase webhook ID */
    SHOWCASE_ID(5),
    /** Notification Channel */
    NOTIFICATION(6);

    private final long mockID;

    MockSnowflake(long mockID) {
        this.mockID = mockID;
    }

    public long getMockID() {
        return mockID;
    }

    /**
     * Create a unique snowflake using the system time a randomized bits.
     * {@snippet : long snowflake = (System.currentTimeMillis() - 1420070400000L) << 22;}
     *
     * @return A snowflake long using {@link System#currentTimeMillis()}
     * @see TimeUtil#getDiscordTimestamp(long)
     */
    public static long getRandom() {
        long timestamp = (System.currentTimeMillis() - 1420070400000L) << TimeUtil.TIMESTAMP_OFFSET;
        long increment = ThreadLocalRandom.current().nextLong(0, 1 << 12); // 12 bits randomness
        return timestamp | increment;
    }
}
