package ua.xlany.gradientpillars.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.commands.subcommands.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Головний обробник команди /gp
 * Делегує виконання підкомандам
 */
public class GPCommand {

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

    public void register(Commands commands) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("gp")
                .requires(ctx -> ctx.getSender().hasPermission("gradientpillars.command"))
                .executes(ctx -> {
                    sendUsage(ctx.getSource().getSender());
                    return 1;
                });

        for (SubCommand sub : subCommands.values()) {
            root.then(Commands.literal(sub.getName())
                    .executes(ctx -> executeSubCommand(ctx, sub, new String[0]))
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> suggestSubCommand(ctx, sub, builder))
                            .executes(ctx -> {
                                String argsStr = StringArgumentType.getString(ctx, "args");
                                return executeSubCommand(ctx, sub, argsStr.split(" "));
                            })));
        }

        commands.register(root.build(), "Gradient Pillars main command", List.of("gradientpillars", "pillars"));
    }

    private int executeSubCommand(CommandContext<CommandSourceStack> ctx, SubCommand sub, String[] args) {
        CommandSender sender = ctx.getSource().getSender();

        // Дозволяємо виконання команди з консолі тільки для певних команд (наприклад,
        // reload)
        // Для більшості команд вимагаємо гравця, але перевірку краще робити всередині
        // самої команди
        // Тим не менш, для сумісності з існуючим кодом, де багато кастів (Player)
        // sender,
        // ми зробимо базову перевірку тут, але дозволимо reload для всіх.

        if (!(sender instanceof Player) && !(sub instanceof ReloadCommand)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command!"));
            return 0;
        }

        sub.execute(sender, args);
        return 1;
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

    private CompletableFuture<Suggestions> suggestSubCommand(CommandContext<CommandSourceStack> ctx, SubCommand sub,
            SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String[] args;

        if (remaining.isEmpty()) {
            args = new String[0];
        } else {
            args = remaining.split(" ", -1);
        }

        List<String> suggestions = sub.tabComplete(ctx.getSource().getSender(), args);
        if (suggestions != null) {
            String lastArg = args.length > 0 ? args[args.length - 1] : "";
            for (String s : suggestions) {
                if (s.toLowerCase().startsWith(lastArg.toLowerCase())) {
                    builder.suggest(s);
                }
            }
        }
        return builder.buildFuture();
    }
}
