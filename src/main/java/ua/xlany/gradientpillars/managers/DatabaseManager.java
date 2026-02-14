package ua.xlany.gradientpillars.managers;

import org.bukkit.configuration.ConfigurationSection;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.utils.LibraryUtil;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseManager {

    private final GradientPillars plugin;
    private final LibraryUtil libraryUtil;
    private Connection connection;
    private String type;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private Properties properties;
    private boolean tablesCreated = false;

    public DatabaseManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.libraryUtil = plugin.getLibraryUtil();
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection dbConfig = plugin.getConfig().getConfigurationSection("database");
        if (dbConfig == null) {
            plugin.getLogger().severe("Database configuration not found!");
            return;
        }

        this.type = dbConfig.getString("type", "sqlite").toLowerCase();
        this.host = dbConfig.getString("host", "localhost");
        this.port = dbConfig.getInt("port", 3306);
        this.database = dbConfig.getString("database", "gradientpillars");
        this.username = dbConfig.getString("username", "root");
        this.password = dbConfig.getString("password", "password");

        this.properties = new Properties();
        ConfigurationSection propsConfig = dbConfig.getConfigurationSection("properties");
        if (propsConfig != null) {
            for (String key : propsConfig.getKeys(false)) {
                properties.setProperty(key, String.valueOf(propsConfig.get(key)));
            }
        }
    }

    public boolean connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return true;
            }

            // Завантажуємо клас драйвера з нашого LibraryUtil ClassLoader
            // Це дозволяє використовувати драйвери з папки libs без додавання їх в
            // системний ClassLoader (що блокується в Java 16+)
            ClassLoader libLoader = libraryUtil.getClassLoader();
            Driver driver;

            if ("sqlite".equals(type)) {
                Class<?> driverClass = Class.forName("org.sqlite.JDBC", true, libLoader);
                driver = (Driver) driverClass.getConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));

                java.io.File dataFolder = new java.io.File(plugin.getDataFolder(), "database.db");
                if (!dataFolder.getParentFile().exists()) {
                    dataFolder.getParentFile().mkdirs();
                }
                connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            } else {
                Class<?> driverClass = Class.forName("org.mariadb.jdbc.Driver", true, libLoader);
                driver = (Driver) driverClass.getConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));

                String url = String.format("jdbc:mariadb://%s:%d/%s", host, port, database);
                connection = DriverManager.getConnection(url, username, password);
            }

            // Логуємо тільки перше підключення
            if (!tablesCreated) {
                plugin.getLogger().info("Successfully connected to database (" + type + ")!");
                createTables();
                tablesCreated = true;
            }

            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Database driver class not found for type: " + type, e);
            return false;
        } catch (Exception e) { // Catching generic exception because of cleaner code for
                                // reflection/instantiation issues
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database!", e);
            return false;
        }
    }

    // Shim class to allow DriverManager to access drivers loaded by a custom
    // ClassLoader
    // DriverManager only allows drivers loaded by the system class loader or the
    // same class loader as the caller
    private static class DriverShim implements Driver {
        private final Driver driver;

        DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }

    private void createTables() {
        String createStatsTable;

        if ("sqlite".equals(type)) {
            createStatsTable = """
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL,
                        wins INT DEFAULT 0,
                        losses INT DEFAULT 0,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;
        } else {
            createStatsTable = """
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL,
                        wins INT DEFAULT 0,
                        losses INT DEFAULT 0,
                        last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """;
        }

        try (PreparedStatement stmt = connection.prepareStatement(createStatsTable)) {
            stmt.executeUpdate();
            // Лог тільки один раз при створенні
            plugin.getLogger().info("Database tables created successfully!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables!", e);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed!");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error closing database connection!", e);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
