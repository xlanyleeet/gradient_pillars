package ua.xlany.gradientpillars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.commands.subcommands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Головний обробник команди /gp
 * Делегує виконання підкомандам
 */
public class GPCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands;

    public GPCommand(GradientPillars plugin) {
        this.subCommands = new HashMap<>();

        // Реєструємо всі підкоманди
        registerSubCommand(new PlayCommand(plugin));
        registerSubCommand(new JoinCommand(plugin));
        registerSubCommand(new LeaveCommand(plugin));
        registerSubCommand(new ArenaCommand(plugin));
        registerSubCommand(new SetHubCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sendUsage(sender);
            return true;
        }

        // Передаємо аргументи без назви підкоманди
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(sender, subArgs);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§6§lGradient Pillars §8| §fКоманди");
        sender.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        for (SubCommand subCommand : subCommands.values()) {
            sender.sendMessage("§e" + subCommand.getUsage() + " §7- " + subCommand.getDescription());
        }
        sender.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Автодоповнення назв підкоманд
            for (String subCommandName : subCommands.keySet()) {
                if (subCommandName.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommandName);
                }
            }
        } else if (args.length >= 2) {
            // Делегуємо автодоповнення підкоманді
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                List<String> subCompletions = subCommand.tabComplete(sender, subArgs);
                if (subCompletions != null) {
                    completions.addAll(subCompletions);
                }
            }
        }

        return completions;
    }
}
