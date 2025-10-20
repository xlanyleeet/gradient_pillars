package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class AddPillarCommand implements ArenaSubCommand {

    private final GradientPillars plugin;
    
    public AddPillarCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage", "/gp arena addpillar <номер> <arena>"));
            return true;
        }

        int pillarNumber;
        try {
            pillarNumber = Integer.parseInt(args[0]);
            if (pillarNumber < 1 || pillarNumber > 16) {
                player.sendMessage("§c§l✘ §cНомер стовпа має бути від 1 до 16!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.invalid-number"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c§l✘ §cВкажи назву арени!");
            player.sendMessage("§7Використання: §e/gp arena addpillar " + pillarNumber + " <arena>");
            return true;
        }

        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage("§c§l✘ §cАрени §e" + arenaName + " §cне існує!");
            player.sendMessage("§7Створи арену: §e/gp arena create " + arenaName);
            return true;
        }

        Location loc = player.getLocation();
        arena.setPillar(pillarNumber - 1, loc);
        player.sendMessage("§a§l✔ §aСтовп #" + pillarNumber + " §aдодано для арени §e" + arenaName);

        plugin.getArenaManager().cacheArena(arena);
        return true;
    }
    
    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Показуємо номери стовпів (1-16)
            for (int i = 1; i <= 16; i++) {
                String num = String.valueOf(i);
                if (num.startsWith(args[0])) {
                    completions.add(num);
                }
            }
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
        return "addpillar";
    }
    
    @Override
    public String getDescription() {
        return "Додати стовп до арени";
    }
    
    @Override
    public String getUsage() {
        return "/gp arena addpillar <номер> <arena>";
    }
}
