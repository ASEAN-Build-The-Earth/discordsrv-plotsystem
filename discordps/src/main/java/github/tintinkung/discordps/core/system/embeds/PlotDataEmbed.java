package github.tintinkung.discordps.core.system.embeds;

/**
 * Represent webhook owned embeds
 */
@FunctionalInterface
public interface PlotDataEmbed {

    /**
     * Build the embed into serializable object
     *
     * @return The built embed data
     */
    github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed build();
}