package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.dependencies.okhttp3.HttpUrl;

import java.text.DecimalFormat;
import java.util.function.BiFunction;

/**
 * Coordinates Utility
 * <ul>
 *     <li>Format coordinates to "x,z"</li>
 *     <li>Format coordinates to NSEW</li>
 *     <li>Convert minecraft coordinates to Geo using
 *         <a href="https://smybteapi.buildtheearth.net/projection/toGeo?mcpos=x,z">smybteapi endpoint</a>,
 *         use {@link CoordinatesUtil#initCoordinatesFunction(BiFunction)} to optimize API call with physical method reference.
 *     </li>
 * </ul>
 * @see #formatGeoCoordinatesNumeric
 * @see #formatGeoCoordinatesNSEW
 * @see #convertToGeo
 * @see #initCoordinatesFunction
 */
public class CoordinatesUtil {

    private record API(String host, String path, String query, BiFunction<Double, Double, String> coordinates) {}

    private static final DecimalFormat decFormat1 = new DecimalFormat();
    private static final BiFunction<Double, Double, String> query = (x, y) -> String.join(",", String.valueOf(x), String.valueOf(y));

    private static CoordinatesConversion coordinatesConversion = (x, y) -> {
        throw new RuntimeException(
            "coordinatesConversion's Plot-System Symbol has not been initialized, "
            + "please redirect this object to use the default function."
        );
    };
    private static final API toGeoAPI = new API(
        "smybteapi.buildtheearth.net",
        "projection/toGeo",
        "mcpos", query
    );

    /**
     * Optimize {@link CoordinatesUtil#convertToGeo(double, double)}
     * with a function reference so that it doesn't request for API every time.
     * @param function The handler as a {@link BiFunction} of {@link CoordinatesConversion#convertToGeo(double, double)}
     */
    public static void initCoordinatesFunction(BiFunction<Double, Double, double[]> function) {
        coordinatesConversion = function::apply;
    }

    /**
     * Get formatted numeric geographic coordinates
     *
     * @param coordinates - WG84 EPSG:4979 coordinates as double array
     * @return - Formatted numeric coordinates as String
     */
    public static String formatGeoCoordinatesNumeric(double[] coordinates) throws RuntimeException {
        return coordinates[1] + "," + coordinates[0];
    }


    /**
     * Get formatted NSEW geographic coordinates
     *
     * @param coordinates - WG84 EPSG:4979 coordinates as double array
     * @return - Formatted NSEW coordinates as String
     */
    public static String formatGeoCoordinatesNSEW(double[] coordinates) {
        double fixedLon = coordinates[0];
        double fixedLat = coordinates[1];
        String eo = fixedLon < 0 ? "W" : "E";
        String ns = fixedLat < 0 ? "S" : "N";
        double absLon = Math.abs(fixedLon);
        double absLat = Math.abs(fixedLat);
        int longitudeDegrees = (int) absLon;
        int latitudeDegrees = (int) absLat;
        double minLon = absLon * 60 - longitudeDegrees * 60;
        double minLat = absLat * 60 - latitudeDegrees * 60;
        int longitudeMinutes = (int) minLon;
        int latitudeMinutes = (int) minLat;
        double secLon = minLon * 60 - longitudeMinutes * 60;
        double secLat = minLat * 60 - latitudeMinutes * 60;
        String formattedLongitude = longitudeDegrees + "°" + longitudeMinutes + "'" + decFormat1.format(secLon) + "\"" + eo;
        String formattedLatitude = latitudeDegrees + "°" + latitudeMinutes + "'" + decFormat1.format(secLat) + "\"" + ns;
        return formattedLatitude + " " + formattedLongitude;
    }


    /**
     * Plot System CoordinateConversion class reference for converting minecraft coordinate.
     * @see <a href="https://github.com/AlpsBTE/Plot-System/blob/caaf70230ac3c3cd24d22a391e4be5765563a5c7/src/main/java/com/alpsbte/plotsystem/utils/conversion/CoordinateConversion.java#L60">
     *     com.alpsbte.plotsystem.utils.conversion.CoordinateConversion#convertToGeo
     * </a>
     * @param xCords - Minecraft player x-axis coordinates
     * @param yCords - Minecraft player y-axis coordinates
     * @return - WG84 EPSG:4979 coordinates as double array {lon,lat} in degrees
     */
    public static double[] convertToGeo(double xCords, double yCords) throws RuntimeException {

        try {
            return coordinatesConversion.convertToGeo(xCords, yCords);
        }
        catch (RuntimeException ex) {
            HttpUrl httpURL = new HttpUrl.Builder()
                .scheme("https")
                .host(toGeoAPI.host())
                .addPathSegments(toGeoAPI.path())
                .addQueryParameter(toGeoAPI.query(), toGeoAPI.coordinates().apply(xCords, yCords))
                .build();
            return coordinatesConversion.convertToGeo(httpURL);
        }
    }

}
