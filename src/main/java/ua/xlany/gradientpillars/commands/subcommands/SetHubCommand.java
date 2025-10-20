package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.Collections;
import java.util.List;

public class SetHubCommand implements SubCommand {

    private final GradientPillars plugin;

    public SetHubCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gradientpillars.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        plugin.getConfigManager().setHub(
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch());

        player.sendMessage(plugin.getMessageManager().getPrefixedComponent("hub.set"));
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "sethub";
    }

    @Override
    public String getDescription() {
        return "Встановити точку хабу";
    }

    @Override
    public String getUsage() {
        return "/gp sethub";
    }
}
