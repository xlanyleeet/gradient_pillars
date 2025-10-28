package ua.xlany.gradientpillars.managers;

import org.bukkit.configuration.ConfigurationSection;
import ua.xlany.gradientpillars.GradientPillars;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class DatabaseManager {

    private final GradientPillars plugin;
    private Connection connection;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private Properties properties;
    private boolean tablesCreated = false;

    public DatabaseManager(GradientPillars plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection dbConfig = plugin.getConfig().getConfigurationSection("database");
        if (dbConfig == null) {
            plugin.getLogger().severe("Database configuration not found!");
            return;
        }

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

            Class.forName("org.mariadb.jdbc.Driver");

            String url = String.format("jdbc:mariadb://%s:%d/%s", host, port, database);
            connection = DriverManager.getConnection(url, username, password);

            // Логуємо тільки перше підключення
            if (!tablesCreated) {
                plugin.getLogger().info("Successfully connected to database!");
                createTables();
                tablesCreated = true;
            }

            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "MariaDB driver not found!", e);
            return false;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database!", e);
            return false;
        }
    }

    private void createTables() {
        String createStatsTable = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;

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
