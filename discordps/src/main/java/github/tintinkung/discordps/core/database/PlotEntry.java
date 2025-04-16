package github.tintinkung.discordps.core.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/// | - - - - | - - - - | - - - - -  | - - - - - | - - -  | - - - -  | - - - - - - -  | - - - - - | - - - - - -  | - - - -  |
/// | plot_id | city_id | country_id | review_id | status | owner_id | mc_coordinates | city_name | country_name | feedback |
/// | - - - - | - - - - | - - - - -  | - - - - - | - - -  | - - - -  | - - - - - - -  | - - - - - | - - - - - -  | - - - -  |
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
     * Fetch all submitted plot entry ordered by plot ID
     * @throws SQLException on failure
     */
    public static List<PlotEntry> fetchSubmittedPlots() throws SQLException {
        // TODO: Add configurable option to filter out unfinished plot ( AND `status` != 'unfinished' )
        String query = "SELECT plots.id AS plot_id, "
                + "plots.city_project_id AS city_id, city_projects.country_id AS country_id, plots.review_id AS review_id, "
                + "plots.`status` AS `status`, plots.owner_uuid AS owner_id, "
                + "plots.mc_coordinates AS mc_coordinates, city_projects.`name` AS city_name, "
                + "countries.`name` AS country_name, reviews.feedback AS feedback "
                + "FROM plotsystem_plots AS plots "
                + "LEFT JOIN plotsystem_city_projects AS city_projects ON city_projects.id = plots.city_project_id "
                + "LEFT JOIN plotsystem_reviews AS reviews ON reviews.id = plots.review_id "
                + "LEFT JOIN plotsystem_countries AS countries ON countries.id = city_projects.country_id "
                + "WHERE city_projects.`visible` = 1 AND `status` != 'unclaimed' "
                + "ORDER BY plots.id";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            ResultSet rs = statement.executeQuery();
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
            DatabaseConnection.closeResultSet(rs);
            return result;
        }

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

