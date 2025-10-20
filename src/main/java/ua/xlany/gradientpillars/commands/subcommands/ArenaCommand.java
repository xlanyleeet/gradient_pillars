package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.commands.subcommands.arena.*;

import java.util.*;

/**
 * Головний роутер для команд налаштування арен
 * Делегує виконання підкомандам у папці arena/
 */
public class ArenaCommand implements SubCommand {

    private final GradientPillars plugin;
    private final Map<String, ArenaSubCommand> arenaSubCommands;

    public ArenaCommand(GradientPillars plugin) {
        this.plugin = plugin;
        this.arenaSubCommands = new HashMap<>();

        // Реєструємо всі підкоманди arena
        registerArenaSubCommand(new CreateArenaCommand(plugin));
        registerArenaSubCommand(new EditArenaCommand(plugin));
        registerArenaSubCommand(new DeleteArenaCommand(plugin));
        registerArenaSubCommand(new AddPillarCommand(plugin));
        registerArenaSubCommand(new SetLobbyCommand(plugin));
        registerArenaSubCommand(new SetSpectatorCommand(plugin));
        registerArenaSubCommand(new SetWorldCommand(plugin));
        registerArenaSubCommand(new SetMinPlayersCommand(plugin));
        registerArenaSubCommand(new SetMaxPlayersCommand(plugin));
        registerArenaSubCommand(new SaveArenaCommand(plugin));
    }

    /**
     * Зареєструвати підкоманду arena
     */
    private void registerArenaSubCommand(ArenaSubCommand subCommand) {
        arenaSubCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Перевірка прав доступу
        if (!sender.hasPermission("gradientpillars.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.no-permission"));
            return true;
        }

        // Тільки гравці можуть налаштовувати арени
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Якщо не вказано підкоманду - показати використання
        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        // Знайти підкоманду
        String subCommandName = args[0].toLowerCase();
        ArenaSubCommand subCommand = arenaSubCommands.get(subCommandName);

        if (subCommand == null) {
            sendUsage(player);
            return true;
        }

        // Делегувати виконання підкоманді (передати args БЕЗ назви підкоманди)
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(player, subArgs);
    }

    private void sendUsage(Player player) {
        player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                "commands.usage", "usage",
                "/gp arena <create|edit|delete|addpillar|lobby|spectator|setworld|minplayers|maxplayers|save> [параметри]"));
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Показати всі доступні підкоманди arena
            for (String subCmdName : arenaSubCommands.keySet()) {
                if (subCmdName.startsWith(args[0].toLowerCase())) {
                    completions.add(subCmdName);
                }
            }
        } else if (args.length >= 2) {
            // Делегувати автодоповнення відповідній підкоманді
            String subCommandName = args[0].toLowerCase();
            ArenaSubCommand subCommand = arenaSubCommands.get(subCommandName);

            if (subCommand != null) {
                // Передати args БЕЗ назви підкоманди
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }

        return completions;
    }

    @Override
    public String getName() {
        return "arena";
    }

    @Override
    public String getDescription() {
        return "Налаштування арен";
    }

    @Override
    public String getUsage() {
        return "/gp arena <create|edit|delete|addpillar|lobby|spectator|setworld|minplayers|maxplayers|save> [параметри]";
    }
}
