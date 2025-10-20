package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.ArrayList;
import java.util.List;

public class CreateArenaCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public CreateArenaCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage", "/gp arena create <назва>"));
            return true;
        }

        String arenaName = args[0];

        if (plugin.getArenaManager().getArena(arenaName) != null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.already-exists", "arena", arenaName));
            return true;
        }

        boolean created = plugin.getArenaManager().createArena(arenaName);
        if (created) {
            player.sendMessage("§a§l✔ §aАрену §e" + arenaName + " §aстворено!");
            player.sendMessage("");
            player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§6§lЧеклист налаштування арени:");
            player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§7[ ] §e/gp arena setworld " + arenaName + " §7- Встановити світ");
            player.sendMessage("§7[ ] §e/gp arena lobby " + arenaName + " §7- Встановити лобі");
            player.sendMessage("§7[ ] §e/gp arena spectator " + arenaName + " §7- Точка спостерігача");
            player.sendMessage("§7[ ] §e/gp arena addpillar 1 " + arenaName + " §7- Додати стовпи (1-16)");
            player.sendMessage("§7[ ] §e/gp arena minplayers " + arenaName + " 2 §7- Мін. гравців (опц.)");
            player.sendMessage("§7[ ] §e/gp arena maxplayers " + arenaName + " 16 §7- Макс. гравців (опц.)");
            player.sendMessage("§7[ ] §e/gp arena save " + arenaName + " §7- Зберегти арену");
            player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // Для create не показуємо автодоповнення (назву вводить користувач)
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Створити нову арену";
    }

    @Override
    public String getUsage() {
        return "/gp arena create <назва>";
    }
}
