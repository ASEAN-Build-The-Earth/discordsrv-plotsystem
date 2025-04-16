package github.tintinkung.discordps.core.providers;

import github.tintinkung.discordps.DiscordPS;

/**
 * A base class that provides access to the main {@link DiscordPS} plugin instance.
 */
public abstract class PluginProvider {

    /**
     * The reference to the main {@link DiscordPS} plugin instance.
     */
    protected final DiscordPS plugin;

    /**
     * Constructs a new {@code PluginProvider} with the specified {@link DiscordPS} plugin instance.
     * @param plugin the plugin instance to be provided; must not be {@code null}
     */
    public PluginProvider(DiscordPS plugin) {
        this.plugin = plugin;
    }
}
