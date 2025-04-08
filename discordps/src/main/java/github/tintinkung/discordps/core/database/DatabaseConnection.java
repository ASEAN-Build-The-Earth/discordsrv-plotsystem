package github.tintinkung.discordps.core.database;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.tintinkung.discordps.ConfigPaths;
import github.tintinkung.discordps.DiscordPS;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseConnection {
    // https://regex101.com/r/qtakRP/1
    private final static Pattern JDBC_PATTERN = Pattern.compile("^(?<proto>\\w+):(?<engine>\\w+)://(?<host>.+?)(:(?<port>\\d{1,5}|PORT))?((/\\w+)+|/)\\??(?<params>.+)?$");

    private final static Properties config = new Properties();
    private static HikariDataSource dataSource;

    private static int connectionClosed, connectionOpened;

    public static boolean InitializeDatabase() throws ClassNotFoundException {
        FileConfiguration configFile = DiscordPS.getPlugin().getConfig();
        String URL = configFile.getString(ConfigPaths.DATABASE_URL);
        String name = configFile.getString(ConfigPaths.DATABASE_NAME);
        String username = configFile.getString(ConfigPaths.DATABASE_USERNAME);
        String password = configFile.getString(ConfigPaths.DATABASE_PASSWORD);

        // Validate config
        if(StringUtils.isBlank(URL)) return false;
        Matcher matcher = JDBC_PATTERN.matcher(URL);

        if(!matcher.matches()) {
            DiscordPS.error("Not using JDBC because the JDBC connection string is invalid!");
            return false;
        }

        if(!matcher.group("proto").equalsIgnoreCase("jdbc")) {
            DiscordPS.error("Not using JDBC because the protocol of the JDBC URL is wrong!");
            return false;
        }

        // Decide SQL driver to choose, else HikariDataSource will try to automatically find it
        String engine = matcher.group("engine");

        // Provided by our plugin since Plot-System mainly use mariadb
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

        // Provided by DiscordSRV
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

        dataSource = new HikariDataSource(new HikariConfig(config));
        DiscordPS.info("Initialized database with: " + (
            dataSource.getDriverClassName() == null?
            dataSource.getDataSourceClassName()
            : dataSource.getDriverClassName())
        );
        createTables();
        return true;
    }

    @Deprecated
    public static Connection getConnection() {
        int retries = 3;
        while (retries > 0) {
            try {
                return dataSource.getConnection();
            } catch (SQLException ex) {
                DiscordPS.error("Database connection failed!\n\n" + ex.getMessage(), ex);
            }
            retries--;
        }
        return null;
    }

    public static StatementBuilder createStatement(String sql) {
        return new StatementBuilder(sql);
    }

    public static void closeResultSet(ResultSet resultSet) throws SQLException {
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

    static void createTables() {
        try (Connection con = dataSource.getConnection()) {
            for (String table : StatementBuilder.Tables.getTables()) {
                Objects.requireNonNull(con).prepareStatement(table).executeUpdate();
            }
        } catch (SQLException ex) {
            DiscordPS.error("An error occurred while creating database table!", ex);
        }
    }

    /**
     * Returns a missing auto increment id
     *
     * @param table in the database
     * @return smallest missing auto increment id in the table
     */
    public static int getTableID(String table) {
        try {
            String query = "SELECT id + 1 available_id FROM $table t WHERE NOT EXISTS (SELECT * FROM $table WHERE $table.id = t.id + 1) ORDER BY id LIMIT 1"
                    .replace("$table", table);
            try (ResultSet rs = DatabaseConnection.createStatement(query).executeQuery()) {
                if (rs.next()) {
                    int i = rs.getInt(1);
                    DatabaseConnection.closeResultSet(rs);
                    return i;
                }

                DatabaseConnection.closeResultSet(rs);
                return 1;
            }
        } catch (SQLException ex) {
            DiscordPS.error("A SQL error occurred!", ex);
            return 1;
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

        private static class Tables {
            private final static List<String> tables;

            public static List<String> getTables() {
                return tables;
            }

            static {
                tables = Collections.singletonList(
                        // plotsystem_discord_webhook
                        "CREATE TABLE IF NOT EXISTS `langUsers` (" +
                                "`uuid` varchar(36) PRIMARY KEY," +
                                "`lang` varchar(10)" +
                                ");"
                );
            }
        }
    }
}
