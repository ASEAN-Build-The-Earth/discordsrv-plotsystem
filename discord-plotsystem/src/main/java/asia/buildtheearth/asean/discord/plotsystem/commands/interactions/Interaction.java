package asia.buildtheearth.asean.discord.plotsystem.commands.interactions;

/**
 * Super class of all slash command related interactions
 */
public class Interaction {

    /**
     * The owner of this interaction (snowflake),
     * as known as whoever trigger the slash command.
     */
    public final long userID;

    /**
     * The unique ID to save this interaction (snowflake).
     * Conventionally saved as the event ID of an initial interaction etc. slash command.
     */
    public final long eventID;

    /**
     * Create a user owned interaction with specified event ID
     *
     * @param userID The owner of this interaction (snowflake)
     * @param eventID The unique ID to save this interaction (snowflake)
     */
    Interaction(long userID, long eventID) {
        this.userID = userID;
        this.eventID = eventID;
    }

    /**
     * Get the interaction snowflake ID
     *
     * @return ID as string, use {@code this.userID} for original Long value
     */
    public String getID() {
        return Long.toUnsignedString(eventID);
    }

    /**
     * Get the owner snowflake ID
     *
     * @return ID as string, use {@code this.eventID} for original Long value
     */
    public String getUserID() {
        return Long.toUnsignedString(userID);
    }
}
