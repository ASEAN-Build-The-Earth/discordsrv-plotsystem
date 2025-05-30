package asia.buildtheearth.asean.discord.plotsystem.core.database;

import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public record PlotEntry(
    int plotID,
    int cityID,
    int countryID,
    int reviewID,
    PlotStatus status,
    String ownerUUID,
    String mcCoordinates,
    String cityName,
    String countryName,
    String feedback) {

    /**
     * Query for all entries in the record {@link PlotEntry} using the following data:
     * <table border="1">
     *   <tr><td>plot_id</td> <td>city_id</td> <td>country_id</td> <td>review_id</td> <td>status</td> <td>owner_id</td> <td>mc_coordinates</td> <td>city_name</td> <td>country_name</td> <td>feedback</td></tr>
     *   <tr><td>int</td> <td>int</td> <td>int</td> <td>int</td> <td>{@link PlotStatus}</td> <td>String</td> <td>String</td> <td>String</td> <td>String</td> <td>String</td></tr>
     * </table>
     */
    private static final String PLOT_ENTRIES_QUERY = "SELECT plots.id AS plot_id, "
            + "plots.city_project_id AS city_id, city_projects.country_id AS country_id, plots.review_id AS review_id, "
            + "plots.`status` AS `status`, plots.owner_uuid AS owner_id, "
            + "plots.mc_coordinates AS mc_coordinates, city_projects.`name` AS city_name, "
            + "countries.`name` AS country_name, reviews.feedback AS feedback "
            + "FROM plotsystem_plots AS plots "
            + "LEFT JOIN plotsystem_city_projects AS city_projects ON city_projects.id = plots.city_project_id "
            + "LEFT JOIN plotsystem_reviews AS reviews ON reviews.id = plots.review_id "
            + "LEFT JOIN plotsystem_countries AS countries ON countries.id = city_projects.country_id ";

    public static @Nullable PlotEntry getByID(int plotID) {
        String query = PLOT_ENTRIES_QUERY
                + "WHERE plots.id = ? "
                + "ORDER BY plots.id";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(plotID);
            ResultSet rs = statement.executeQuery();
            List<PlotEntry> result = getAsEntry(rs);
            DatabaseConnection.closeResultSet(rs);
            return result.getFirst();
        }
        catch (SQLException ex) {
            DiscordPS.warning(Debug.Warning.RUNTIME_SQL_EXCEPTION, ex.getMessage());
            return null;
        }
    }

    /**
     * Fetch all submitted plot entry ordered by plot ID
     */
    public static @Nullable List<PlotEntry> fetchSubmittedPlots() {
        String query = PLOT_ENTRIES_QUERY
                + "WHERE city_projects.`visible` = 1 AND `status` != 'unclaimed' "
                + "ORDER BY plots.id";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            ResultSet rs = statement.executeQuery();
            List<PlotEntry> result = getAsEntry(rs);
            DatabaseConnection.closeResultSet(rs);
            return result;
        }
        catch (SQLException ex) {
            DiscordPS.warning(Debug.Warning.RUNTIME_SQL_EXCEPTION, ex.getMessage());
            return null;
        }
    }


    private static @NotNull List<PlotEntry> getAsEntry(@NotNull ResultSet rs) throws SQLException {
        List<PlotEntry> result = new ArrayList<>();
        while(rs.next()) {
            result.add(new PlotEntry(
                    rs.getInt("plot_id"),
                    rs.getInt("city_id"),
                    rs.getInt("country_id"),
                    rs.getInt("review_id"),
                    PlotStatus.valueOf(rs.getString("status")) ,
                    rs.getString("owner_id"),
                    rs.getString("mc_coordinates"),
                    rs.getString("city_name"),
                    rs.getString("country_name"),
                    rs.getString("feedback")
            ));
        }
        return result;
    }

    @Override
    public String toString() {
        return "PlotEntry{" +
                "plotID=" + plotID +
                ", cityID=" + cityID +
                ", countryID=" + countryID +
                ", reviewID=" + reviewID +
                ", status=" + status +
                ", ownerUUID='" + ownerUUID + '\'' +
                ", mcCoordinates='" + mcCoordinates + '\'' +
                ", cityName='" + cityName + '\'' +
                ", countryName='" + countryName + '\'' +
                ", feedback='" + feedback + '\'' +
                '}';
    }
}

