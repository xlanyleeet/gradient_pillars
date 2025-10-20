package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class SetMaxPlayersCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public SetMaxPlayersCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c§l✘ §cВкажи кількість гравців та назву арени!");
            player.sendMessage("§7Використання: §e/gp arena maxplayers <кількість> <arena>");
            return true;
        }

        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(args[0]);
            if (maxPlayers < 1) {
                player.sendMessage("§c§l✘ §cМаксимальна кількість гравців має бути більше 0!");
                return true;
            }
            if (maxPlayers > 16) {
                player.sendMessage("§c§l✘ §cМаксимальна кількість гравців не може перевищувати 16!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.invalid-number"));
            return true;
        }

        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage("§c§l✘ §cАрени §e" + arenaName + " §cне існує!");
            player.sendMessage("§7Створи арену: §e/gp arena create " + arenaName);
            return true;
        }

        if (maxPlayers < arena.getMinPlayers()) {
            player.sendMessage("§c§l✘ §cМаксимальна кількість гравців не може бути менше мінімальної (" + arena.getMinPlayers() + ")!");
            return true;
        }

        arena.setMaxPlayers(maxPlayers);
        player.sendMessage("§a§l✔ §aМаксимальну кількість гравців встановлено: §e" + maxPlayers + " §aдля арени §e" + arenaName);
        player.sendMessage("§7  Не забудь зберегти арену: §e/gp arena save " + arenaName);

        plugin.getArenaManager().cacheArena(arena);
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Показуємо приклади кількості
            completions.add("8");
            completions.add("12");
            completions.add("16");
        } else if (args.length == 2) {
            // Показуємо назви арен
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(arena.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getName() {
        return "maxplayers";
    }

    @Override
    public String getDescription() {
        return "Встановити максимальну кількість гравців";
    }

    @Override
    public String getUsage() {
        return "/gp arena maxplayers <кількість> <arena>";
    }
}
