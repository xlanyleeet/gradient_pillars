package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class SaveArenaCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public SaveArenaCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§c§l✘ §cВкажи назву арени!");
            player.sendMessage("§7Використання: §e/gp arena save <arena>");
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage("§c§l✘ §cАрени §e" + arenaName + " §cне існує!");
            return true;
        }

        // Детальна перевірка налаштування арени
        if (arena.getWorldName() == null) {
            player.sendMessage("§c§l✘ §cАрену не налаштовано!");
            player.sendMessage("§7  Не встановлено світ! Використай: §e/gp arena setworld " + arenaName);
            return true;
        }
        if (arena.getSpectator() == null) {
            player.sendMessage("§c§l✘ §cАрену не налаштовано!");
            player.sendMessage("§7  Не встановлено точку спостерігача! Використай: §e/gp arena spectator " + arenaName);
            return true;
        }
        if (arena.getPillarCount() == 0) {
            player.sendMessage("§c§l✘ §cАрену не налаштовано!");
            player.sendMessage(
                    "§7  Не встановлено жодного стовпа! Використай: §e/gp arena addpillar <номер> " + arenaName);
            return true;
        }

        plugin.getArenaManager().saveArena(arena);
        player.sendMessage("§a§l✔ §aАрену §e" + arenaName + " §aзбережено!");
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Показуємо назви арен
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(arena.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "Зберегти налаштування арени";
    }

    @Override
    public String getUsage() {
        return "/gp arena save <arena>";
    }
}
