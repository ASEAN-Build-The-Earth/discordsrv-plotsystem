package github.tintinkung.discordps.core.database;

import github.tintinkung.discordps.DiscordPS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * <a href="https://github.com/AlpsBTE/Plot-System/blob/a3b547e6f1dab101bdb4700dc132a4a1722558b1/src/main/java/com/alpsbte/plotsystem/core/system/BuildTeam.java">
 *     Original File
 * </a>
 */
public class BuildTeam {
    private final int ID;
    private String name;

    private BuildTeam(int ID) throws SQLException {
        this.ID = ID;

        try (ResultSet rs = DatabaseConnection.createStatement("SELECT name FROM plotsystem_buildteams WHERE id = ?")
                .setValue(ID).executeQuery()) {

            if (rs.next()) this.name = rs.getString(1);
            DatabaseConnection.closeResultSet(rs);
        }
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public List<Country> getCountries() throws SQLException {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT country_id FROM plotsystem_buildteam_has_countries WHERE buildteam_id = ?")
                .setValue(ID).executeQuery()) {

            List<Country> countries = new ArrayList<>();
            while (rs.next()) countries.add(new Country(rs.getInt(1)));
            DatabaseConnection.closeResultSet(rs);
            return countries;
        }
    }

    public List<Builder> getReviewers() throws SQLException {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT builder_uuid FROM plotsystem_builder_is_reviewer WHERE buildteam_id = ?")
                .setValue(ID).executeQuery()) {

            List<Builder> builders = new ArrayList<>();
            while (rs.next()) builders.add(Builder.byUUID(UUID.fromString(rs.getString(1))));
            DatabaseConnection.closeResultSet(rs);
            return builders;
        }
    }

    public static List<BuildTeam> getBuildTeamsByReviewer(UUID reviewerUUID) throws SQLException {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT buildteam_id FROM plotsystem_builder_is_reviewer WHERE builder_uuid = ?")
                .setValue(reviewerUUID.toString()).executeQuery()) {

            List<BuildTeam> buildTeams = new ArrayList<>();
            while (rs.next()) buildTeams.add(new BuildTeam(rs.getInt(1)));
            DatabaseConnection.closeResultSet(rs);
            return buildTeams;
        }
    }

    public static List<BuildTeam> getBuildTeams() {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT id FROM plotsystem_buildteams").executeQuery()) {
            List<BuildTeam> buildTeams = new ArrayList<>();
            while (rs.next()) buildTeams.add(new BuildTeam(rs.getInt(1)));
            DatabaseConnection.closeResultSet(rs);
            return buildTeams;
        } catch (SQLException ex) {
            DiscordPS.error("A SQL error occurred!", ex);
        }
        return new ArrayList<>();
    }

    public static void addBuildTeam(String name) throws SQLException {
        int id = DatabaseConnection.getTableID("plotsystem_buildteams");
        DatabaseConnection.createStatement("INSERT INTO plotsystem_buildteams (id, name) VALUES (?, ?)")
                .setValue(id)
                .setValue(name).executeUpdate();
    }

    public static void removeBuildTeam(int serverID) throws SQLException {
        DatabaseConnection.createStatement("DELETE FROM plotsystem_buildteams WHERE id = ?")
                .setValue(serverID).executeUpdate();
    }

    public static void setBuildTeamName(int id, String newName) throws SQLException {
        DatabaseConnection.createStatement("UPDATE plotsystem_buildteams SET name = ? WHERE id = ?")
                .setValue(newName)
                .setValue(id).executeUpdate();
    }

    public static void addCountry(int id, int countryID) throws SQLException {
        DatabaseConnection.createStatement("INSERT plotsystem_buildteam_has_countries SET country_id = ?, buildteam_id = ?")
                .setValue(countryID)
                .setValue(id).executeUpdate();
    }

    public static void removeCountry(int id, int countryID) throws SQLException {
        DatabaseConnection.createStatement("DELETE FROM plotsystem_buildteam_has_countries WHERE country_id = ? AND buildteam_id = ?")
                .setValue(countryID)
                .setValue(id).executeUpdate();
    }

    public static void addReviewer(int id, String reviewerUUID) throws SQLException {
        DatabaseConnection.createStatement("INSERT plotsystem_builder_is_reviewer SET builder_uuid = ?, buildteam_id = ?")
                .setValue(reviewerUUID)
                .setValue(id).executeUpdate();
    }

    public static void removeReviewer(int id, String reviewerUUID) throws SQLException {
        DatabaseConnection.createStatement("DELETE FROM plotsystem_builder_is_reviewer WHERE builder_uuid = ? AND buildteam_id = ?")
                .setValue(reviewerUUID)
                .setValue(id).executeUpdate();
    }
}
