package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class SetSpectatorCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public SetSpectatorCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§c§l✘ §cВкажи назву арени!");
            player.sendMessage("§7Використання: §e/gp arena spectator <arena>");
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage("§c§l✘ §cАрени §e" + arenaName + " §cне існує!");
            player.sendMessage("§7Створи арену: §e/gp arena create " + arenaName);
            return true;
        }

        Location loc = player.getLocation();
        arena.setSpectator(loc);
        player.sendMessage("§a§l✔ §aТочку спостерігача встановлено для арени §e" + arenaName);

        plugin.getArenaManager().cacheArena(arena);
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
        return "spectator";
    }

    @Override
    public String getDescription() {
        return "Встановити точку спостерігача для арени";
    }

    @Override
    public String getUsage() {
        return "/gp arena spectator <arena>";
    }
}
