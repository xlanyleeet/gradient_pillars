package ua.xlany.gradientpillars.commands.subcommands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.PlayerStats;

import java.util.Collections;
import java.util.List;

public class StatsCommand implements SubCommand {

    private final GradientPillars plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public StatsCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        PlayerStats stats;
        String targetName;

        if (args.length > 0) {
            targetName = args[0];
            stats = plugin.getStatsManager().getStats(targetName);
            if (stats == null) {
                sender.sendMessage(mm.deserialize(plugin.getMessageManager().getMessage("stats.not-found")));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(
                        mm.deserialize("<red>This command can only be run by a player (or specify a player name)."));
                return true;
            }
            Player player = (Player) sender;
            targetName = player.getName();
            stats = plugin.getStatsManager().getStats(player.getUniqueId());
            if (stats == null) {
                // Should not happen for online player usually, but just in case
                sender.sendMessage(mm.deserialize(plugin.getMessageManager().getMessage("stats.not-found")));
                return true;
            }
        }

        sendStatsMessage(sender, stats, targetName);
        return true;
    }

    private void sendStatsMessage(CommandSender sender, PlayerStats stats, String name) {
        int wins = stats.getWins();
        int losses = stats.getLosses();
        int total = wins + losses;
        double winRate = total > 0 ? (double) wins / total * 100 : 0.0;

        String header = plugin.getMessageManager().getMessage("stats.header").replace("{player}", name);
        String winsMsg = plugin.getMessageManager().getMessage("stats.wins").replace("{count}", String.valueOf(wins));
        String lossesMsg = plugin.getMessageManager().getMessage("stats.losses").replace("{count}",
                String.valueOf(losses));
        String rateMsg = plugin.getMessageManager().getMessage("stats.win-rate").replace("{rate}",
                String.format("%.1f", winRate));

        sender.sendMessage(mm.deserialize(header));
        sender.sendMessage(mm.deserialize(winsMsg));
        sender.sendMessage(mm.deserialize(lossesMsg));
        sender.sendMessage(mm.deserialize(rateMsg));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return null; // Return null to suggest online players
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "View player statistics";
    }

    @Override
    public String getUsage() {
        return "/gp stats [player]";
    }
}
