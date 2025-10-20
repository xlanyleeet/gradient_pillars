package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class EditArenaCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public EditArenaCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage", "/gp arena edit <назва>"));
            return true;
        }

        String arenaName = args[0];
        Arena existingArena = plugin.getArenaManager().getArena(arenaName);

        if (existingArena == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.not-found", "arena", arenaName));
            return true;
        }

        plugin.getArenaManager().cacheArena(existingArena);
        player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                "arena.edit-mode", "arena", arenaName));
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Показуємо існуючі арени
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
        return "edit";
    }

    @Override
    public String getDescription() {
        return "Редагувати існуючу арену";
    }

    @Override
    public String getUsage() {
        return "/gp arena edit <назва>";
    }
}
