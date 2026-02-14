package ua.xlany.gradientpillars.commands.subcommands.arena;

import net.kyori.adventure.text.minimessage.MiniMessage;
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
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<green><bold>✔ <green>Арену <yellow>" + arenaName + " <green>створено!"));
            player.sendMessage(MiniMessage.miniMessage().deserialize(""));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<yellow><bold><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><bold>Чеклист налаштування арени:"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<yellow><bold><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>[ ] <yellow>/gp arena setworld " + arenaName + " <gray>- Встановити світ"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>[ ] <yellow>/gp arena lobby " + arenaName + " <gray>- Встановити лобі"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>[ ] <yellow>/gp arena spectator " + arenaName + " <gray>- Точка спостерігача"));
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>[ ] <yellow>/gp arena addpillar 1 " + arenaName + " <gray>- Додати стовпи (1-16)"));
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>[ ] <yellow>/gp arena minplayers " + arenaName + " 2 <gray>- Мін. гравців (опц.)"));
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>[ ] <yellow>/gp arena maxplayers " + arenaName + " 16 <gray>- Макс. гравців (опц.)"));
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<gray>[ ] <yellow>/gp arena save " + arenaName + " <gray>- Зберегти арену"));
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<yellow><bold><st>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
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
