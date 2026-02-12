package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.PlayerStats;

import java.util.Collections;
import java.util.List;

public class StatsCommand implements SubCommand {

    private final GradientPillars plugin;

    public StatsCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.player-only"));
            return true;
        }

        // Run asynchronously to avoid blocking the main thread with DB calls
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerStats stats = null;
            String targetName = null;

            if (args.length > 0) {
                // Check stats for another player
                if (!sender.hasPermission("gradientpillars.stats.others")) {
                    sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.no-permission"));
                    return;
                }

                targetName = args[0];
                stats = plugin.getStatsManager().getStats(targetName);
            } else {
                // Check own stats
                if (sender instanceof Player player) {
                    targetName = player.getName();
                    stats = plugin.getStatsManager().getStats(player.getUniqueId());

                    // If no stats found in DB yet (e.g. new player), create empty stats object for
                    // display
                    if (stats == null) {
                        stats = new PlayerStats(player.getUniqueId(), player.getName(), 0, 0);
                    }
                }
            }

            if (stats == null) {
                sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.stats.not-found", "player",
                        targetName));
                return;
            }

            // Calculate extra stats
            int gamesPlayed = stats.getWins() + stats.getLosses();
            double winRate = 0.0;
            if (gamesPlayed > 0) {
                winRate = (double) stats.getWins() / gamesPlayed * 100.0;
            }

            // Send stats
            sender.sendMessage(
                    plugin.getMessageManager().getComponent("commands.stats.header", "player", stats.getPlayerName()));
            sender.sendMessage(plugin.getMessageManager().getComponent("commands.stats.wins", "wins",
                    String.valueOf(stats.getWins())));
            sender.sendMessage(plugin.getMessageManager().getComponent("commands.stats.losses", "losses",
                    String.valueOf(stats.getLosses())));
            sender.sendMessage(plugin.getMessageManager().getComponent("commands.stats.games-played", "count",
                    String.valueOf(gamesPlayed)));
            sender.sendMessage(plugin.getMessageManager().getComponent("commands.stats.win-rate", "rate",
                    String.format("%.1f", winRate)));
        });

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("gradientpillars.stats.others")) {
            return null; // Return null to show online players
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "View your or another player's statistics";
    }

    @Override
    public String getUsage() {
        return "/gp stats [player]";
    }
}
