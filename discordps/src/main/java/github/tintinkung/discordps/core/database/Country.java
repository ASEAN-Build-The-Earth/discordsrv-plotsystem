package github.tintinkung.discordps.core.database;


import github.tintinkung.discordps.DiscordPS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * <a href="https://github.com/AlpsBTE/Plot-System/blob/5e0ecacfcc41e4f0bb42c656736bb2d21a2c6912/src/main/java/com/alpsbte/plotsystem/core/system/Country.java">
 *     Original File
 * </a>
 */
public class Country {

    private final int ID;
    private int serverID;

    private String name;
    private String headID;

    private String continent;

    public Country(int ID) throws SQLException {
        this.ID = ID;

        try (ResultSet rs = DatabaseConnection.createStatement("SELECT server_id, name, head_id, continent FROM plotsystem_countries WHERE id = ?")
                .setValue(this.ID).executeQuery()) {

            if (rs.next()) {
                this.serverID = rs.getInt(1);
                this.name = rs.getString(2);
                this.headID = rs.getString(3);
                this.continent = rs.getString(4);
            }

            DatabaseConnection.closeResultSet(rs);
        }
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public static List<Country> getCountries() {
        try (ResultSet rs = DatabaseConnection.createStatement("SELECT id FROM plotsystem_countries ORDER BY server_id").executeQuery()) {
            List<Country> countries = new ArrayList<>();
            while (rs.next()) {
                countries.add(new Country(rs.getInt(1)));
            }

            DatabaseConnection.closeResultSet(rs);
            return countries;
        } catch (SQLException ex) {
            DiscordPS.error("A SQL error occurred!", ex);
        }
        return new ArrayList<>();
    }

    public static void removeCountry(int countryID) throws SQLException {
        DatabaseConnection.createStatement("DELETE FROM plotsystem_countries WHERE id = ?")
                .setValue(countryID).executeUpdate();
    }

    public static void setHeadID(int countryID, int headID) throws SQLException {
        DatabaseConnection.createStatement("UPDATE plotsystem_countries SET head_id = ? WHERE id = ?")
                .setValue(headID)
                .setValue(countryID).executeUpdate();
    }
}
