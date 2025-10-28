package ua.xlany.gradientpillars.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.PlayerStats;

public class GradientPillarsPlaceholders extends PlaceholderExpansion {

    private final GradientPillars plugin;

    public GradientPillarsPlaceholders(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gp";
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) {
            // Повертаємо 0 якщо статистики немає
            return switch (params.toLowerCase()) {
                case "wins" -> "0";
                case "losses" -> "0";
                case "total" -> "0";
                case "winrate" -> "0.0";
                default -> null;
            };
        }

        return switch (params.toLowerCase()) {
            case "wins" -> String.valueOf(stats.getWins());
            case "losses" -> String.valueOf(stats.getLosses());
            case "total" -> String.valueOf(stats.getTotalGames());
            case "winrate" -> String.format("%.1f", stats.getWinRate());
            default -> null;
        };
    }
}
