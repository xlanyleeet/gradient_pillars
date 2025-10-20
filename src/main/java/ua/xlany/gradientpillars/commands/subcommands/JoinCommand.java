package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.Collections;
import java.util.List;

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

        boolean success = plugin.getGameManager().joinGame(player);

        if (!success) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.game-full"));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
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
