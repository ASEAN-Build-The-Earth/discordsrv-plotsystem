package github.tintinkung.discordps.core.database;

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

public class DatabaseConnection {
    private final static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    private static int connectionClosed, connectionOpened;

    public static void InitializeDatabase() throws ClassNotFoundException {
        Class.forName("org.mariadb.jdbc.Driver"); // newer: com.mysql.cj.jdbc.Driver

        FileConfiguration configFile = DiscordPS.getPlugin().getConfig();
        String URL = configFile.getString(ConfigPaths.DATABASE_URL);
        String name = configFile.getString(ConfigPaths.DATABASE_NAME);
        String username = configFile.getString(ConfigPaths.DATABASE_USERNAME);
        String password = configFile.getString(ConfigPaths.DATABASE_PASSWORD);

        config.setJdbcUrl(URL + name);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(3);

        dataSource = new HikariDataSource(config);

        createTables();
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
                        // LangUser
                        "CREATE TABLE IF NOT EXISTS `langUsers` (" +
                                "`uuid` varchar(36) PRIMARY KEY," +
                                "`lang` varchar(10)" +
                                ");"
                );
            }
        }
    }
}
