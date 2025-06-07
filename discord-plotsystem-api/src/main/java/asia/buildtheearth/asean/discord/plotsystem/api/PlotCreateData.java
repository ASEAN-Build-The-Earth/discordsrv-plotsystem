package asia.buildtheearth.asean.discord.plotsystem.api;

/**
 * Data representing the initial state when first creating a plot.
 * <p>
 * This record is used to create a new plot with a specific status, owner, and metadata.
 *
 * @param plotID         The unique integer ID of the plot being created.
 * @param ownerUUID      The UUID of the plot owner. Must be parsable by {@link java.util.UUID#fromString(String)}.
 * @param status         The initial {@link PlotStatus} of the plot. Use {@link #prepareStatus(String)} to convert from string.
 * @param cityProjectID  A team-defined city project identifier. Can be any string up to 255 characters in length.
 * @param countryCode    ISO 3166 Alpha-2 country code, represented as a 2-letter string (e.g., "MY", "TH").
 * @param geoCoordinates The geographical coordinates of the plot in {@code WGS84 EPSG:4979} format.
 *                       Recommended to use the <a href="https://smybteapi.buildtheearth.net/projection/toGeo?mcpos=x,z">
 *                       smybteapi <code>toGeo</code> endpoint</a> for conversion.
 *
 * @see asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent
 */
public record PlotCreateData(
    int plotID,
    String ownerUUID,
    PlotStatus status,
    String cityProjectID,
    String countryCode,
    double[] geoCoordinates
) {

    /**
     * Enumeration of valid plot statuses during creation.
     *
     * @see #prepareStatus(String)
     */
    public enum PlotStatus {
        /** Plot is ongoing and awaiting submission. */
        ON_GOING("on_going"),
        /** Plot is submitted and awaiting reviews. */
        FINISHED("finished"),
        /** Plot has been reviewed and marked as rejected. */
        REJECTED("rejected"),
        /** Plot has been reviewed and marked as approved. */
        APPROVED("approved"),
        /** Plot is marked as archived and closed for further interactions. */
        ARCHIVED("archived"),
        /** Plot is abandoned and available for a new owner to claim.  */
        ABANDONED("abandoned");

        private final String name;

        /**
         * Constructs a {@link PlotStatus} with a string identifier.
         *
         * @param name The lowercase identifier used internally for this status.
         */
        PlotStatus(String name) {
            this.name = name;
        }

        /**
         * Returns the internal string identifier for this status.
         * Used for internal comparisons and mappings.
         *
         * @return The lowercase string identifier of this status.
         */
        public String getName() {
            return name;
        }
    }

    /**
     * Parses a string into a {@link PlotStatus} enum.
     *
     * <p>Use this method to safely map user or external input to a {@link PlotStatus} value.</p>
     *
     * @apiNote Value is supported by Plot-System database status enum:
     *          {@code unfinished}, {@code unreviewed}, {@code unclaimed}, {@code completed}
     * @param from The input string representing the status (case-insensitive).
     * @return The corresponding {@link PlotStatus}.
     * @throws IllegalArgumentException If the given string does not match a valid status.
     */
    public static PlotStatus prepareStatus(String from) throws IllegalArgumentException {
        return switch (from) {
            case "ON_GOING", "on_going",
                 "UNFINISHED", "unfinished" -> PlotStatus.ON_GOING;
            case "FINISHED", "finished",
                 "COMPLETED", "completed",
                 "UNREVIEWED", "unreviewed" -> PlotStatus.FINISHED;
            case "REJECTED", "rejected" -> PlotStatus.REJECTED;
            case "APPROVED", "approved" -> PlotStatus.APPROVED;
            case "ARCHIVED", "archived" -> PlotStatus.ARCHIVED;
            case "ABANDONED", "abandoned",
                 "UNCLAIMED", "unclaimed" -> PlotStatus.ABANDONED;
            case null -> throw new IllegalArgumentException("Status string cannot be null");
            default -> throw new IllegalArgumentException("Invalid Status String: " + from);
        };
    }

    @Override
    public String toString() {
        return "PlotCreateData{plotID=" + plotID + ", ownerUUID=" + ownerUUID
            + ", status='" + status + '\'' + ", cityProjectID='" + cityProjectID
            + '\'' + ", countryCode='" + countryCode + '\''
            + ", geoCoordinates=" + java.util.Arrays.toString(geoCoordinates) + '}';
    }
}
