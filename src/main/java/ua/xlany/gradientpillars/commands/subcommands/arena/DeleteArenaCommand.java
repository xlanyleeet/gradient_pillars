package ua.xlany.gradientpillars.commands.subcommands.arena;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.List;

public class DeleteArenaCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public DeleteArenaCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage", "/gp arena delete <назва>"));
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.not-found", "arena", arenaName));
            return true;
        }

        // Перевірити чи арена не використовується
        if (plugin.getGameManager().getGameByArena(arenaName) != null) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red><bold>✘ <red>Неможливо видалити арену <yellow>" + arenaName + "<red>!"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>  Арена зараз використовується в грі. Зачекай поки гра завершиться."));
            return true;
        }

        // Видалити арену
        boolean deleted = plugin.getArenaManager().deleteArena(arenaName);

        if (deleted) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<green><bold>✔ <green>Арену <yellow>" + arenaName + " <green>успішно видалено!"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>  Конфігурацію арени видалено з сервера."));
        } else {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red><bold>✘ <red>Помилка при видаленні арени <yellow>" + arenaName + "<red>!"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>  Перевір логи сервера для деталей."));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Показуємо назви існуючих арен
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
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Видалити арену";
    }

    @Override
    public String getUsage() {
        return "/gp arena delete <назва>";
    }
}
