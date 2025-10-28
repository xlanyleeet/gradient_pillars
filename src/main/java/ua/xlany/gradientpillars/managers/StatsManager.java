package ua.xlany.gradientpillars.managers;

import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.PlayerStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    private final GradientPillars plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerStats> cachedStats = new HashMap<>();

    public StatsManager(GradientPillars plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public PlayerStats getStats(UUID uuid) {
        // Спочатку перевіряємо кеш
        if (cachedStats.containsKey(uuid)) {
            return cachedStats.get(uuid);
        }

        // Завантажуємо з БД
        PlayerStats stats = loadStats(uuid);
        if (stats != null) {
            cachedStats.put(uuid, stats);
        }
        return stats;
    }

    public PlayerStats getStats(String playerName) {
        // Шукаємо в кеші за іменем
        for (PlayerStats stats : cachedStats.values()) {
            if (stats.getPlayerName().equalsIgnoreCase(playerName)) {
                return stats;
            }
        }

        // Завантажуємо з БД
        PlayerStats stats = loadStatsByName(playerName);
        if (stats != null) {
            cachedStats.put(stats.getUuid(), stats);
        }
        return stats;
    }

    private PlayerStats loadStats(UUID uuid) {
        String query = "SELECT * FROM player_stats WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlayerStats(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("player_name"),
                        rs.getInt("wins"),
                        rs.getInt("losses"));
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load stats for " + uuid, e);
        }

        return null;
    }

    private PlayerStats loadStatsByName(String playerName) {
        String query = "SELECT * FROM player_stats WHERE player_name = ?";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new PlayerStats(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("player_name"),
                        rs.getInt("wins"),
                        rs.getInt("losses"));
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load stats for " + playerName, e);
        }

        return null;
    }

    public void addWin(UUID uuid, String playerName) {
        PlayerStats stats = getStats(uuid);
        if (stats == null) {
            stats = new PlayerStats(uuid, playerName, 1, 0);
        } else {
            stats.addWin();
        }

        cachedStats.put(uuid, stats);
        saveStats(stats);
    }

    public void addLoss(UUID uuid, String playerName) {
        PlayerStats stats = getStats(uuid);
        if (stats == null) {
            stats = new PlayerStats(uuid, playerName, 0, 1);
        } else {
            stats.addLoss();
        }

        cachedStats.put(uuid, stats);
        saveStats(stats);
    }

    private void saveStats(PlayerStats stats) {
        String query = """
                INSERT INTO player_stats (uuid, player_name, wins, losses)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                player_name = VALUES(player_name),
                wins = VALUES(wins),
                losses = VALUES(losses)
                """;

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, stats.getUuid().toString());
            stmt.setString(2, stats.getPlayerName());
            stmt.setInt(3, stats.getWins());
            stmt.setInt(4, stats.getLosses());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats for " + stats.getPlayerName(), e);
        }
    }

    public void clearCache() {
        cachedStats.clear();
    }

    public void removeFromCache(UUID uuid) {
        cachedStats.remove(uuid);
    }
}
