package asia.buildtheearth.asean.discord.plotsystem.core.database;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.util.SQLUtil;
import asia.buildtheearth.asean.discord.plotsystem.ConfigPaths;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static asia.buildtheearth.asean.discord.plotsystem.Debug.Error.DATABASE_NOT_INITIALIZED;
import static asia.buildtheearth.asean.discord.plotsystem.Debug.Warning.DATABASE_STRUCTURE_NOT_MATCH;

public class DatabaseConnection {

    /**
     * Default database table name, which can be configured differently in config.yml
     */
    private static String webhookTableName = "discord_webhook";
    private final static Properties config = new Properties();
    private static HikariDataSource dataSource;

    /**
     * Matches {@code  <proto>:<engine>://<HOST>:<PORT>/?<params>}
     * <a href="https://regex101.com/r/qtakRP/1">regex101.com</a>
     */
    public final static Pattern JDBC_PATTERN = Pattern.compile(
            "^(?<proto>\\w+):(?<engine>\\w+)://(?<host>.+?)(:(?<port>\\d{1,5}|PORT))?((/\\w+)+|/)\\??(?<params>.+)?$"
    );

    private static int connectionClosed, connectionOpened;

    @Contract(pure = true)
    public static String getWebhookTableName() {
        return webhookTableName;
    }

    public static boolean InitializeDatabase() throws ClassNotFoundException {
        FileConfiguration configFile = DiscordPS.getPlugin().getConfig();
        String URL = configFile.getString(ConfigPaths.DATABASE_URL);
        String name = configFile.getString(ConfigPaths.DATABASE_NAME);
        String username = configFile.getString(ConfigPaths.DATABASE_USERNAME);
        String password = configFile.getString(ConfigPaths.DATABASE_PASSWORD);
        String webhookTable = configFile.getString(ConfigPaths.DATABASE_WEBHOOK_TABLE);

        // Validate config
        if(StringUtils.isBlank(URL)) return false;
        Matcher matcher = JDBC_PATTERN.matcher(URL);

        if(!matcher.matches()) {
            DiscordPS.error(DATABASE_NOT_INITIALIZED,"Not using JDBC because the JDBC connection string is invalid!");
            return false;
        }

        if(!matcher.group("proto").equalsIgnoreCase("jdbc")) {
            DiscordPS.error(DATABASE_NOT_INITIALIZED, "Not using JDBC because the protocol of the JDBC URL is wrong!");
            return false;
        }

        // Decide SQL driver to choose, else HikariDataSource will try to automatically find it
        String engine = matcher.group("engine");

        // MariaDB: Provided by our plugin since Plot-System mainly use mariadb
        if(engine.equalsIgnoreCase("mariadb")) {
            Class.forName("org.mariadb.jdbc.MariaDbDataSource");

            config.setProperty("dataSourceClassName", org.mariadb.jdbc.MariaDbDataSource.class.getName());
            config.setProperty("dataSource.user", username);
            config.setProperty("dataSource.password", password);
            config.setProperty("dataSource.databaseName", name);
            config.setProperty("maximumPoolSize", String.valueOf(3));

            if(matcher.group("host") != null)
                config.setProperty("dataSource.serverName", matcher.group("host"));
            if(matcher.group("port") != null)
                config.setProperty("dataSource.portNumber", matcher.group("port"));
        }

        // MySQL: Provided by DiscordSRV 1.29.0
        if(engine.equalsIgnoreCase("mysql")) {
            // The MySQL DataSource is known to be broken with respect to network timeout support.
            // Use jdbcUrl configuration with a specified driver instead.
            Class.forName("com.mysql.cj.jdbc.Driver");
            config.setProperty("jdbcUrl", URL);
            config.setProperty("username", username);
            config.setProperty("password", password);
            config.setProperty("dataSource.databaseName", name);
            config.setProperty("dataSource.cachePrepStmts", "true");
            config.setProperty("dataSource.prepStmtCacheSize", String.valueOf(250));
            config.setProperty("dataSource.prepStmtCacheSqlLimit", String.valueOf(2048));
            config.setProperty("driverClassName", com.mysql.cj.jdbc.Driver.class.getName());
            config.setProperty("maximumPoolSize", String.valueOf(3));
        }

        // SQLite: Partial support for unit testing (driver is in test scope)
        if(engine.equalsIgnoreCase("sqlite")) {
            Class.forName("org.sqlite.SQLiteDataSource");

            config.setProperty("dataSourceClassName", "org.sqlite.SQLiteDataSource");
            String filename = matcher.group("host");
            if(filename != null) {
                DiscordPS.debug("SQLite detected, treating the host '" + filename + "' as connection file.");
                config.setProperty("jdbcUrl", "jdbc:sqlite:" + filename);
            }
            else DiscordPS.error("No filename detected for SQLite database connection");

            config.setProperty("maximumPoolSize", String.valueOf(3));
        }

        dataSource = new HikariDataSource(new HikariConfig(config));
        DiscordPS.info("Initialized database with: " + (
            dataSource.getDriverClassName() == null?
            dataSource.getDataSourceClassName()
            : dataSource.getDriverClassName())
        );

        // Validate webhook table and create if not exist.
        try {
            if(webhookTable == null) {
                validateWebhookTable(getWebhookTableName());
            }
            else if(StringUtils.isBlank(webhookTable)) {
                DiscordPS.warning("A configured database webhook table name is blank, "
                        + "please specify it or delete the entry entirely. "
                        + "Using the default value of '" + getWebhookTableName() + "'");
                validateWebhookTable(getWebhookTableName());
            }
            else {
                DatabaseConnection.webhookTableName = webhookTable;
                validateWebhookTable(webhookTable);
            }
        }
        catch (SQLException ex) {
            DiscordPS.error(DATABASE_NOT_INITIALIZED, "Failed to validate database webhook table.");
            return false;
        }

        return true;
    }

    @Contract("_ -> new")
    public static @NotNull StatementBuilder createStatement(String sql) {
        return new StatementBuilder(sql);
    }

    public static void closeResultSet(@NotNull ResultSet resultSet) throws SQLException {
        if(resultSet.isClosed()
                && resultSet.getStatement().isClosed()
                && resultSet.getStatement().getConnection().isClosed())
            return;

        resultSet.close();
        resultSet.getStatement().close();
        resultSet.getStatement().getConnection().close();

        connectionClosed++;

        if(connectionOpened > connectionClosed + 5)
            DiscordPS.error("There are multiple database connections opened. Please report this issue.");
    }

    public static void shutdown() {
        dataSource.close();
    }

    static void validateWebhookTable(String table) throws SQLException {
        if(StringUtils.isBlank(table)) throw new SQLException("Configured Database webhook table name is blank");

        try (Connection connection = dataSource.getConnection()) {
           boolean sqlite = ConnectionUtil.checkIfSQLite(connection);
            if (!sqlite && ConnectionUtil.checkIfTableExists(connection, table)) {
                if(!ConnectionUtil.checkIfTableMatchesStructure(connection, table, WebhookTable.getExpected()))
                    DiscordPS.warning(DATABASE_STRUCTURE_NOT_MATCH,"JDBC table " +
                        table + " does not match expected structure. Required manual validation."
                    );
            } else {
                String newTable = sqlite
                        ? WebhookTable.getCreateStatementSQLite(table)
                        : WebhookTable.getCreateStatement(table);
                DiscordPS.info("Initializing new database table '" + table + "' for webhook management.");
                try (final PreparedStatement statement = connection.prepareStatement(newTable)) {
                    statement.executeUpdate();
                }
            }

        } catch (SQLException ex) {
            DiscordPS.error("An error occurred while creating database table!", ex);
            throw ex;
        }
    }

    public static class StatementBuilder implements AutoCloseable {
        private final String sql;
        private final List<Object> values = new ArrayList<>();

        private Connection connection = null;
        private PreparedStatement statement = null;
        private ResultSet resultSet = null;

        public StatementBuilder(String sql) {
            this.sql = sql;
        }

        private void openConnection() throws SQLException {
            connection = dataSource.getConnection();
            if (connection == null) throw new NullPointerException("Connection is invalid: null");
        }

        public ResultSet executeQuery() throws SQLException {
            if (connection != null) throw new IllegalStateException("Builder is already fired.");

            openConnection();
            statement = connection.prepareStatement(sql);
            resultSet = iterateValues(statement).executeQuery();

            connectionOpened++;

            return resultSet;
        }

        public void executeUpdate() throws SQLException {
            if (connection != null) throw new IllegalStateException("Builder is already fired.");

            openConnection();
            statement = connection.prepareStatement(sql);
            iterateValues(statement).executeUpdate();
            close();
        }

        public StatementBuilder setValue(Object value) {
            values.add(value);
            return this;
        }

        private PreparedStatement iterateValues(PreparedStatement ps) throws SQLException {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            return ps;
        }

        @Override
        public void close() throws SQLException {
            if (connection != null) {
                connection.close();
                connection = null;
            }

            if (statement != null) {
                statement.close();
                statement = null;
            }

            if (resultSet != null) {
                resultSet.close();
                resultSet = null;
            }
        }
    }

    private static abstract class WebhookTable {
        private final static String statusEnum = ThreadStatus.constructEnumString();

        private final static Map<String, String> expected = Map.of(
                "message_id", "bigint(20) unsigned",
                "thread_id", "bigint(20) unsigned",
                "plot_id", "int(11)",
                "status", "enum" + statusEnum,
                "owner_uuid", "varchar(36)",
                "owner_id", "varchar(20)",
                "feedback", "varchar(1024)",
                "version", "int(11)"
        );

        public static Map<String, String> getExpected() {
            return expected;
        }

        @Contract(pure = true)
        public static @NotNull String getCreateStatementSQLite(@NotNull String tableName) {
            return "CREATE TABLE IF NOT EXISTS [" + tableName.trim() + "] "
                    + "([message_id] INT,[thread_id] INT,[plot_id] INT,[status] TEXT,"
                    + "[owner_uuid] TEXT,[owner_id] INT,[feedback] TEXT,[version] INT);";
        }

        @Contract(pure = true)
        public static @NotNull String getCreateStatement(@NotNull String tableName) {
            return  "CREATE TABLE IF NOT EXISTS `" + tableName.trim() + "` (" +
                    " `message_id`       BIGINT UNSIGNED NOT NULL," +
                    " `thread_id`        BIGINT UNSIGNED NOT NULL," +
                    " `plot_id`          INT NOT NULL," +
                    " `status`           ENUM " + statusEnum + " NOT NULL," +
                    " `owner_uuid`       varchar(36) NOT NULL COLLATE 'utf8mb4_general_ci'," +
                    " `owner_id`         varchar(20) NULL DEFAULT NULL," +
                    " `feedback`         varchar(1024) NULL DEFAULT NULL," +
                    " `version`          INT NULL DEFAULT NULL," +
                    " PRIMARY KEY        (`message_id`)," +
                    " INDEX              (`plot_id`)" +
                    ");";
        }
    }

    private static abstract class ConnectionUtil {
        public static boolean checkIfTableExists(@NotNull Connection connection, @NotNull String table) {
            boolean tableExists = false;
            try (final PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1")) {
                statement.executeQuery();
                tableExists = true;
            } catch (SQLException ex) {
                DiscordPS.error("Failed to check if the required SQL table exist", ex);
            }
            return tableExists;
        }

        public static boolean checkIfTableMatchesStructure(Connection connection, String table, Map<String, String> expectedColumns) throws SQLException {
            final Set<String> found = new HashSet<>();
            for (Map.Entry<String, String> entry : SQLUtil.getTableColumns(connection, table).entrySet()) {
                if (!expectedColumns.containsKey(entry.getKey())) continue; // only check columns that we're expecting
                final String expectedType = expectedColumns.get(entry.getKey());
                final String actualType = entry.getValue();
                if (!expectedType.equals(actualType)) {
                    DiscordPS.error("Expected type " + expectedType + " for column " + entry.getKey() + ", got " + actualType);
                    return false;
                }
                found.add(entry.getKey());
            }

            return found.containsAll(expectedColumns.keySet());
        }

        public static boolean checkIfSQLite(@NotNull Connection connection) {
            try { // SQLite connection require different webhook table
                return connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("sqlite");
            } catch (SQLException ex) {
                DiscordPS.error("Cannot detect database connection engine! initial table creation might throw error.", ex);
                return false;
            }
        }
    }
}
