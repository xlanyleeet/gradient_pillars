package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final GradientPillars plugin;

    public ReloadCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gradientpillars.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.no-permission"));
            return true;
        }

        plugin.reload();
        sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.reload"));
        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Перезавантажити конфігурацію";
    }

    @Override
    public String getUsage() {
        return "/gp reload";
    }
}
