package ua.xlany.gradientpillars.commands.subcommands.arena;

import net.kyori.adventure.text.minimessage.MiniMessage;
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
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red><bold>✘ <red>Вкажи назву арени!"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<gray>Використання: <yellow>/gp arena save <arena>"));
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red><bold>✘ <red>Арени <yellow>" + arenaName + " <red>не існує!"));
            return true;
        }

        // Детальна перевірка налаштування арени
        if (arena.getWorldName() == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red><bold>✘ <red>Арену не налаштовано!"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>  Не встановлено світ! Використай: <yellow>/gp arena setworld " + arenaName));
            return true;
        }
        if (arena.getSpectator() == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red><bold>✘ <red>Арену не налаштовано!"));
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>  Не встановлено точку спостерігача! Використай: <yellow>/gp arena spectator " + arenaName));
            return true;
        }
        if (arena.getPillarCount() == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red><bold>✘ <red>Арену не налаштовано!"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                            "<gray>  Не встановлено жодного стовпа! Використай: <yellow>/gp arena addpillar <номер> "
                                    + arenaName));
            return true;
        }

        plugin.getArenaManager().saveArena(arena);
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<green><bold>✔ <green>Арену <yellow>" + arenaName + " <green>збережено!"));
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
