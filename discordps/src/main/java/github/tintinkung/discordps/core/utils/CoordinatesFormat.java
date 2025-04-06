package github.tintinkung.discordps.core.utils;

/**
 * Plot System CoordinateConversion class reference for converting minecraft coordinate.
 * @see <a href="https://github.com/AlpsBTE/Plot-System/blob/caaf70230ac3c3cd24d22a391e4be5765563a5c7/src/main/java/com/alpsbte/plotsystem/utils/conversion/CoordinateConversion.java#L81">
 *     com.alpsbte.plotsystem.utils.conversion.CoordinateConversion#formatGeoCoordinatesNumeric
 * </a>
 */
@FunctionalInterface
public interface CoordinatesFormat {
    /**
     * Get formatted numeric geographic coordinates
     *
     * @param coordinates - WG84 EPSG:4979 coordinates as double array
     * @return - Formatted numeric coordinates as String
     */
    String formatGeoCoordinatesNumeric(double[] coordinates) throws RuntimeException;
}
