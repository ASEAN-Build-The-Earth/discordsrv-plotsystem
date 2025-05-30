package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

/**
 * Represent all language configuration providers
 *
 * @see #getKey()
 */
public interface LangConfig {

    /**
     * Get the YAML path of this language config
     *
     * @return The YAML path in string
     */
    @org.jetbrains.annotations.NotNull String getKey();
}
