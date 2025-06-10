package asia.buildtheearth.asean.discord.plotsystem.api.test.mock;

/**
 * Simple super class store static plugin instance.
 *
 * <p>Additionally store a {@link MockEventListener} to handle API event system.</p>
 *
 * @see MockDiscordPlotSystemAPI#plugin
 * @see #getInstance()
 * @see #getEventListener()
 */
public abstract class MockDiscordPlotSystemAPI {

    /**
     * Mocked plugin instance.
     */
    protected static MockDiscordPlotSystemPlugin plugin;

    /**
     * Mocked event listener.
     */
    protected MockEventListener eventListener;

    /**
     * Get the plugin instance.
     *
     * @return {@link MockDiscordPlotSystemPlugin}
     */
    public static MockDiscordPlotSystemPlugin getInstance() {
        return plugin;
    }

    /**
     * Get the event listener.
     *
     * @return {@link MockEventListener}
     */
    public MockEventListener getEventListener() {
        return this.eventListener;
    }
}
