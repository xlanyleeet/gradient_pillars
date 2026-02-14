package ua.xlany.gradientpillars.commands.subcommands.arena;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class SetMinPlayersCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public SetMinPlayersCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red><bold>✘ <red>Вкажи кількість гравців та назву арени!"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>Використання: <yellow>/gp arena minplayers <кількість> <arena>"));
            return true;
        }

        int minPlayers;
        try {
            minPlayers = Integer.parseInt(args[0]);
            if (minPlayers < 1) {
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<red><bold>✘ <red>Мінімальна кількість гравців має бути більше 0!"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.invalid-number"));
            return true;
        }

        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red><bold>✘ <red>Арени <yellow>" + arenaName + " <red>не існує!"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<gray>Створи арену: <yellow>/gp arena create " + arenaName));
            return true;
        }

        if (minPlayers > arena.getMaxPlayers()) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red><bold>✘ <red>Мінімальна кількість гравців не може бути більше максимальної ("
                            + arena.getMaxPlayers() + ")!"));
            return true;
        }

        arena.setMinPlayers(minPlayers);
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<green><bold>✔ <green>Мінімальну кількість гравців встановлено: <yellow>" + minPlayers
                        + " <green>для арени <yellow>" + arenaName));
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<gray>  Не забудь зберегти арену: <yellow>/gp arena save " + arenaName));

        plugin.getArenaManager().cacheArena(arena);
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Показуємо приклади кількості
            completions.add("2");
            completions.add("4");
            completions.add("8");
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
        return "minplayers";
    }

    @Override
    public String getDescription() {
        return "Встановити мінімальну кількість гравців";
    }

    @Override
    public String getUsage() {
        return "/gp arena minplayers <кількість> <arena>";
    }
}
