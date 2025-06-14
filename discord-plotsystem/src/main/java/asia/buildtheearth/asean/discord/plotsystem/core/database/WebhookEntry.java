package asia.buildtheearth.asean.discord.plotsystem.core.database;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.Debug.Warning.RUNTIME_SQL_EXCEPTION;

/**
 * Plot's Webhook database entry.
 *
 * @param messageID bigint(20) unsigned
 * @param threadID bigint(20) unsigned
 * @param plotID  int(11)
 * @param status enum({@link ThreadStatus})
 * @param ownerUUID varchar(36)
 * @param ownerID varchar(20)
 * @param feedback varchar(1024)
 * @param version  int(11)
 */
public record WebhookEntry(
        long messageID,
        long threadID,
        int plotID,
        @NotNull ThreadStatus status,
        @NotNull String ownerUUID,
        @Nullable String ownerID,
        @Nullable String feedback,
        int version) {

    /**
     * Database Version.
     * Changes if major structure get edited.
     */
    private static final int VERSION = 1;
    private static final Function<String, String> WEBHOOK_ENTRIES_QUERY = (table) -> "SELECT "
            + "webhook.message_id, webhook.thread_id, webhook.plot_id, "
            + "webhook.status, webhook.owner_uuid, webhook.owner_id, webhook.feedback, webhook.version "
            + "FROM " + table + " AS webhook ";

    /**
     * Get the tracking version on this plugin.
     *
     * @return The webhook database version.
     */
    public static int getVersion() {
        return VERSION;
    }

    /**
     * Query entry by plot ID
     *
     * @param plotID the plot ID
     * @return all entries with the plot ID
     * @throws SQLException If something happens
     */
    @NotNull
    public static List<WebhookEntry> getByPlotID(int plotID) throws SQLException {
        return getByID("plot_id", plotID);
    }

    /**
     * Query entry by thread ID
     *
     * @param threadID the thread ID
     * @return all entries with the thread ID
     * @throws SQLException If something happens
     */
    @NotNull
    public static List<WebhookEntry> getByThreadID(long threadID) throws SQLException {
        return getByID("thread_id", threadID);
    }

    /**
     * Query entry by message ID
     *
     * @param messageID the message ID
     * @return the entry if ID is valid
     * @throws SQLException If something happens
     */
    @Nullable
    public static WebhookEntry getByMessageID(long messageID) throws SQLException {
        List<WebhookEntry> result = getByID("message_id", messageID);
        return result.isEmpty()? null : result.getFirst();
    }

    /**
     * Get a webhook entry by column name and value.
     *
     * @param key The column to get as
     * @param value The value of that column's entry position
     * @return All entries that met the query
     * @throws SQLException If something happens
     */
    private static @NotNull List<WebhookEntry> getByID(@NotNull String key, @NotNull Object value) throws SQLException {
        String query = WEBHOOK_ENTRIES_QUERY.apply(DatabaseConnection.getWebhookTableName())
                + "WHERE webhook." + key + " = ? ORDER BY webhook.message_id DESC";
        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(value);
            ResultSet rs = statement.executeQuery();
            List<WebhookEntry> result = new ArrayList<>();
            while(rs.next()) {
                result.add(new WebhookEntry(
                    rs.getLong("message_id"),
                    rs.getLong("thread_id"),
                    rs.getInt("plot_id"),
                    ThreadStatus.valueOf(rs.getString("status")),
                    rs.getString("owner_uuid"),
                    rs.getString("owner_id"),
                    rs.getString("feedback"),
                    rs.getInt("version")
                ));
            }
            DatabaseConnection.closeResultSet(rs);
            return result;
        }
    }

    /**
     * Check if the plot (ID) already existed in the database.
     *
     * @param plotID The plot ID to check
     */
    public static Optional<WebhookEntry> ifPlotExisted(int plotID) {
        // Check if plot already been created by the system
        try {
            List<WebhookEntry> entries = WebhookEntry.getByPlotID(plotID);
            if(!entries.isEmpty()) {
                // using the first data as the main entry as it is the latest one
                return Optional.of(entries.getFirst());
            }
            else return Optional.empty();
        }
        catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            Notification.sendErrorEmbed(
                "SQL exception occurred trying to check for plot entry (plot ID: "
                + plotID + ")", ex.toString());
            return Optional.empty();
        }
    }

    /**
     * Insert a new webhook entry to database table.
     *
     * @param messageID The message ID (primary key)
     * @param threadID The forum thread ID of this entry
     * @param plotID The plot ID of this thread
     * @param status The current status inserting this entry
     * @param ownerUUID The
     */
    public static void insertNewEntry(long messageID,
                                      long threadID,
                                      int plotID,
                                      @NotNull ThreadStatus status,
                                      @NotNull String ownerUUID) {
        String query = "INSERT INTO " + DatabaseConnection.getWebhookTableName()
                + " (message_id, thread_id, plot_id, status, owner_uuid, version) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(messageID);
            statement.setValue(threadID);
            statement.setValue(plotID);
            statement.setValue(status.name());
            statement.setValue(ownerUUID);
            statement.setValue(VERSION);
            statement.executeUpdate();

            DiscordPS.debug("Added plot to webhook database (Plot ID: " + plotID + ")");

        } catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.error("Failed to insert new webhook entry. please check the database user permission.");
            DiscordPS.error("The plot ID: " + plotID + " will NOT be process because it failed to be stored in the database.");
            Notification.sendErrorEmbed(
                "Failed to insert new webhook entry to database, "
                + "The plot ID #`" + plotID + "` "
                + "will appear on thread but cannot be edited by the system.",
                ex.toString()
            );
        }
    }

    /**
     * Insert a new webhook entry to database table.
     *
     * @param messageID The message ID (primary key)
     * @param threadID The forum thread ID of this entry
     * @param plotID The plot ID of this thread
     * @param status The current status inserting this entry
     * @param ownerUUID The
     * @param ownerID The
     */
    public static void insertNewEntry(long messageID,
                                      long threadID,
                                      int plotID,
                                      @NotNull ThreadStatus status,
                                      @NotNull String ownerUUID,
                                      @NotNull String ownerID) {
        String query = "INSERT INTO " + DatabaseConnection.getWebhookTableName()
                + " (message_id, thread_id, plot_id, status, owner_uuid, owner_id, version) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(messageID);
            statement.setValue(threadID);
            statement.setValue(plotID);
            statement.setValue(status.name());
            statement.setValue(ownerUUID);
            statement.setValue(ownerID);
            statement.setValue(VERSION);
            statement.executeUpdate();

            DiscordPS.debug("Added plot to webhook database (Plot ID: " + plotID + ")");

        } catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.error("Failed to insert new webhook entry. please check the database user permission.");
            DiscordPS.error("The plot ID: " + plotID + " will NOT be process because it failed to be stored in the database.");
            Notification.sendErrorEmbed(
                    "Failed to insert new webhook entry to database, "
                            + "The plot ID #`" + plotID + "` "
                            + "will appear on thread but cannot be edited by the system.",
                    ex.toString()
            );
        }
    }

    /**
     * Update the plot's feedback message
     *
     * @param messageID The primary key of this plot's entry
     * @param feedback Feedback message to update to.
     * @throws SQLException If something happens
     */
    public static void updateEntryFeedback(long messageID, @NotNull String feedback) throws SQLException {
        String query = "UPDATE " + DatabaseConnection.getWebhookTableName()
                + " SET feedback = ? WHERE message_id = ?";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement
                    .setValue(feedback)
                    .setValue(messageID)
                    .executeUpdate();
        }
        catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.error("Failed to update webhook entry by threadID");
            throw ex;
        }
    }

    /**
     * Update a specific plot entry's status.
     *
     * @param messageID The primary key of this plot's entry
     * @param entry The status to update into
     * @throws SQLException If something happens
     */
    public static void updateEntryStatus(long messageID, @NotNull ThreadStatus entry) throws SQLException {
        String query = "UPDATE " + DatabaseConnection.getWebhookTableName()
                + " SET status = ? WHERE message_id = ?";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement
                    .setValue(entry.name())
                    .setValue(messageID)
                    .executeUpdate();
        }
        catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.error("Failed to update webhook entry by threadID");
            throw ex;
        }
    }

    /**
     * Update status entry from the giving message ID
     *
     * @param messageID The entry as message ID to update
     * @param entry The status to update into
     * @throws SQLException If something happens
     */
    public static void updateThreadStatus(long messageID, @NotNull ThreadStatus entry) throws SQLException {
        String query = "UPDATE " + DatabaseConnection.getWebhookTableName()
                + " SET status = ? WHERE message_id = ?";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement
                .setValue(entry.name())
                .setValue(messageID)
                .executeUpdate();
        }
        catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.error("Failed to update webhook entry by threadID");
            throw ex;
        }
    }

    public static void updateEntryOwnerID(long messageID, @NotNull String ownerID) throws SQLException {
        String query = "UPDATE " + DatabaseConnection.getWebhookTableName()
                + " SET owner_id = ? WHERE message_id = ?";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement
                    .setValue(ownerID)
                    .setValue(messageID)
                    .executeUpdate();
        }
        catch (SQLException ex) {
            DiscordPS.warning(RUNTIME_SQL_EXCEPTION, ex.getMessage());
            DiscordPS.error("Failed to update webhook entry by threadID");
            throw ex;
        }
    }

    /**
     * Deleted entry from being tracked
     *
     * @param messageID The message ID of entry to be deleted
     * @throws SQLException If the SQL delete query result in a failure
     */
    public static void deleteEntry(long messageID) throws SQLException {
        String query = "DELETE FROM " + DatabaseConnection.getWebhookTableName() + " WHERE message_id = ?";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            statement.setValue(messageID).executeUpdate();
        }
        catch (SQLException ex) {
            DiscordPS.error("Failed to delete webhook entry by messageID");
            throw ex;
        }
    }

    /**
     * Query all entries in the webhook table
     *
     * @return All possible webhook entries
     * @throws SQLException If something happens
     */
    public static @NotNull List<WebhookEntry> getAllEntries() throws SQLException {

        String query = WEBHOOK_ENTRIES_QUERY.apply(DatabaseConnection.getWebhookTableName())
                     + "ORDER BY webhook.plot_id";

        try(DatabaseConnection.StatementBuilder statement = DatabaseConnection.createStatement(query)) {
            ResultSet rs = statement.executeQuery();
            List<WebhookEntry> result = new ArrayList<>();
            while(rs.next()) {
                result.add(new WebhookEntry(
                        rs.getLong("message_id"),
                        rs.getLong("thread_id"),
                        rs.getInt("plot_id"),
                        ThreadStatus.valueOf(rs.getString("status")),
                        rs.getString("owner_uuid"),
                        rs.getString("owner_id"),
                        rs.getString("feedback"),
                        rs.getInt("version")
                ));
            }
            DatabaseConnection.closeResultSet(rs);
            return result;
        }

    }

    @Override
    public String toString() {
        return "WebhookEntry{" +
                "messageID=" + threadID +
                ", threadID=" + plotID +
                ", plotID=" + plotID +
                ", status=" + status +
                ", ownerUUID=" + ownerUUID +
                ", version=" + version +
                '}';
    }
}
