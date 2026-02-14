package ua.xlany.gradientpillars.commands.subcommands.arena;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class SetWorldCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public SetWorldCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red><bold>✘ <red>Вкажи назву арени!"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<gray>Використання: <yellow>/gp arena setworld <arena>"));
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red><bold>✘ <red>Арени <yellow>" + arenaName + " <red>не існує!"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<gray>Створи арену: <yellow>/gp arena create " + arenaName));
            return true;
        }

        String worldName = player.getWorld().getName();
        arena.setWorldName(worldName);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green><bold>✔ <green>Світ <yellow>" + worldName
                + " <green>встановлено для арени <yellow>" + arenaName));

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
        return "setworld";
    }

    @Override
    public String getDescription() {
        return "Встановити світ для арени";
    }

    @Override
    public String getUsage() {
        return "/gp arena setworld <arena>";
    }
}
