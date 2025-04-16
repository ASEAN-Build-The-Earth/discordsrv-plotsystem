package github.tintinkung.discordps.core.database;

import github.scarsz.discordsrv.util.SQLUtil;
import github.tintinkung.discordps.DiscordPS;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public record WebhookEntry(Long threadID, int plotID, ThreadStatus status) {


    @Nullable
    public static WebhookEntry getByPlotID(Integer plotID) throws SQLException {
        return getByID("plot_id", plotID);
    }

    @Nullable
    public static WebhookEntry getByThreadID(Long threadID) throws SQLException {
        return getByID("thread_id", threadID);
    }

    @Nullable
    private static WebhookEntry getByID(String key, Object value) throws SQLException {

        String query = "SELECT webhook.thread_id, webhook.plot_id, webhook.status "
                + "FROM ? AS webhook "
                + "WHERE webhook." + key + " = ?";
        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(DatabaseConnection.getWebhookTableName());
            statement.setValue(value);
            ResultSet rs = statement.executeQuery();
            WebhookEntry result = null;
            while(rs.next()) {
                result = new WebhookEntry(
                    rs.getLong("thread_id"),
                    rs.getInt("plot_id"),
                    ThreadStatus.valueOf(rs.getString("status"))
                );
            }
            DatabaseConnection.closeResultSet(rs);
            return result;
        }
    }

    public static void insertNewEntry(WebhookEntry entry) throws SQLException {
        String query = "INSERT INTO ? SET thread_id = ?, plot_id = ?, status = ?";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(DatabaseConnection.getWebhookTableName());
            statement.setValue(entry.threadID);
            statement.setValue(entry.plotID);
            statement.setValue(entry.status.name());
            statement.executeUpdate();

        } catch (SQLException ex) {
            DiscordPS.error("Failed to insert new webhook entry. please check the database user permission.");
            DiscordPS.error("The webhook entry " + entry + " will NOT be process be cause it failed to be stored in the database.");
            throw ex;
        }
    }

    public static List<WebhookEntry> getAllEntries() throws SQLException {

        String query = "SELECT webhook.thread_id, webhook.plot_id, webhook.status "
                     + "FROM ? AS webhook "
                     + "ORDER BY webhook.plot_id";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(DatabaseConnection.getWebhookTableName());
            ResultSet rs = statement.executeQuery();
            List<WebhookEntry> result = new ArrayList<>();
            while(rs.next()) {

                result.add(new WebhookEntry(
                        rs.getLong("thread_id"),
                        rs.getInt("plot_id"),
                        ThreadStatus.valueOf(rs.getString("status"))
                ));
            }
            DatabaseConnection.closeResultSet(rs);
            return result;
        }

    }

    @Override
    public String toString() {
        return "WebhookEntry{" +
                "threadID=" + threadID +
                ", plotID=" + plotID +
                ", status=" + status +
                '}';
    }
}
