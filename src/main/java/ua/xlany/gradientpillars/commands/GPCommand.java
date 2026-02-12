package ua.xlany.gradientpillars.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.commands.subcommands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Головний обробник команди /gp
 * Делегує виконання підкомандам
 */
@SuppressWarnings("UnstableApiUsage")
public class GPCommand implements BasicCommand {

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
        registerSubCommand(new StatsCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sendUsage(sender);
            return;
        }

        // Передаємо аргументи без назви підкоманди
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
    }

    private void sendUsage(CommandSender sender) {
        var mm = MiniMessage.miniMessage();
        Component separator = mm.deserialize("<yellow><bold><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        sender.sendMessage(separator);
        sender.sendMessage(mm.deserialize("<gold><bold>Gradient Pillars <dark_gray>| <white>Команди"));
        sender.sendMessage(separator);

        for (SubCommand subCommand : subCommands.values()) {
            sender.sendMessage(
                    mm.deserialize("<yellow>" + subCommand.getUsage() + " <gray>- " + subCommand.getDescription()));
        }
        sender.sendMessage(separator);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();
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
