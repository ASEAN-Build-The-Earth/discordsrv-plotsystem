package github.tintinkung.discordps.commands.events;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

/**
 * Command handle for all registered slash command.
 * @see #getCommands()
 */
public interface CommandEvent {

    /**
     * Get a command data as a specified class.
     * @param <T> Template which defines the command data object.
     * @param command The base class to get the command data as.
     * @return The command data instance that the specified class.
     */
    <T extends CommandData> T fromClass(Class<T> command);

    /**
     * Get the commands interface
     * providing method to get the actual {@link CommandData} instance.
     * @return {@link CommandEvent}
     * @see CommandEvent#fromClass(Class)
     */
    CommandEvent getCommands();
}
