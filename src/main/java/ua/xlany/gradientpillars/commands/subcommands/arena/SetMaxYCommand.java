package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.Collections;
import java.util.List;

public class SetMaxYCommand implements ArenaSubCommand {

    private final GradientPillars plugin;

    public SetMaxYCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage", "/gp arena maxy <arena> <y>"));
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.not-found", "arena", arenaName));
            return true;
        }

        try {
            int maxY = Integer.parseInt(args[1]);
            arena.setMaxY(maxY);
            plugin.getArenaManager().cacheArena(arena);

            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.setup.max-y-set", "y", String.valueOf(maxY)));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.invalid-number"));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getArenaManager().getArenas().stream()
                    .map(Arena::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "maxy";
    }

    @Override
    public String getDescription() {
        return "Встановити максимальну висоту будівництва";
    }

    @Override
    public String getUsage() {
        return "/gp arena maxy <arena> <y>";
    }
}
