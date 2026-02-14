package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.gui.ArenaSelectionGUI;

import java.util.ArrayList;
import java.util.List;

public class PlayCommand implements SubCommand {

    private final GradientPillars plugin;
    private final ArenaSelectionGUI gui;

    public PlayCommand(GradientPillars plugin) {
        this.plugin = plugin;
        this.gui = new ArenaSelectionGUI(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЦя команда тільки для гравців!");
            return false;
        }

        // Перевіряємо чи гравець вже в грі
        if (plugin.getGameManager().getPlayerGame(player.getUniqueId()) != null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.already-in-game"));
            return false;
        }

        // Відкриваємо GUI
        gui.open(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getDescription() {
        return "Відкрити меню вибору арени";
    }

    @Override
    public String getUsage() {
        return "/gp play";
    }
}
