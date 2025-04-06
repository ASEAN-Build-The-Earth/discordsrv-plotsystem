package github.tintinkung.discordps.core.utils;


/**
 * Plot System CoordinateConversion class reference for converting minecraft coordinate.
 * @see <a href="https://github.com/AlpsBTE/Plot-System/blob/caaf70230ac3c3cd24d22a391e4be5765563a5c7/src/main/java/com/alpsbte/plotsystem/utils/conversion/CoordinateConversion.java#L60">
 *     com.alpsbte.plotsystem.utils.conversion.CoordinateConversion#convertToGeo
 * </a>
 */
@FunctionalInterface
public interface CoordinatesConversion {

    /**
     * Converts Minecraft coordinates to geographic coordinates
     *
     * @param xCords - Minecraft player x-axis coordinates
     * @param yCords - Minecraft player y-axis coordinates
     * @return - WG84 EPSG:4979 coordinates as double array {lon,lat} in degrees
     */
    double[] convertToGeo(double xCords, double yCords) throws RuntimeException;
}
