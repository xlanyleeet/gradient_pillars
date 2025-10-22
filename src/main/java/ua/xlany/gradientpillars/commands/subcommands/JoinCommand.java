package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class JoinCommand implements SubCommand {

    private final GradientPillars plugin;

    public JoinCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getGameManager().isInGame(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.already-in-game"));
            return true;
        }

        String targetArena = args.length > 0 ? args[0] : null;
        plugin.getGameManager().joinGame(player, targetArena);

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();

            plugin.getArenaManager().getArenas().stream()
                    .filter(arena -> arena != null && arena.isSetup())
                    .map(arena -> arena.getName() == null ? "" : arena.getName())
                    .filter(name -> !name.isEmpty())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .forEach(suggestions::add);

            suggestions.sort(String::compareToIgnoreCase);
            return suggestions;
        }

        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Приєднатися до гри";
    }

    @Override
    public String getUsage() {
        return "/gp join";
    }
}
