package ua.xlany.gradientpillars.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GPCommand implements CommandExecutor, TabCompleter {

    private final GradientPillars plugin;

    public GPCommand(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage", "/gp <join|leave|setup|sethub|reload>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                return handleJoin(sender);

            case "leave":
                return handleLeave(sender);

            case "setup":
                return handleSetup(sender, args);

            case "sethub":
                return handleSetHub(sender);

            case "reload":
                return handleReload(sender);

            default:
                sender.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "commands.usage", "usage", "/gp <join|leave|setup|sethub|reload>"));
                return true;
        }
    }

    private boolean handleJoin(CommandSender sender) {
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

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getGameManager().isInGame(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.leave.not-in-game"));
            return true;
        }

        plugin.getGameManager().leaveGame(player);
        return true;
    }

    private boolean handleSetup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gradientpillars.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "commands.usage", "usage",
                    "/gp setup <create|edit|addpillar|lobby|spectator|setworld|save> [параметри]"));
            return true;
        }

        String subCommand = args[1].toLowerCase();

        // Команда create - створити нову арену
        if (subCommand.equals("create")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "commands.usage", "usage", "/gp setup create <назва>"));
                return true;
            }

            String arenaName = args[2];

            if (plugin.getArenaManager().getArena(arenaName) != null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "arena.already-exists", "arena", arenaName));
                return true;
            }

            boolean created = plugin.getArenaManager().createArena(arenaName);
            if (created) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "arena.created", "arena", arenaName));
            }
            return true;
        }

        // Команда edit - почати редагування існуючої арени
        if (subCommand.equals("edit")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "commands.usage", "usage", "/gp setup edit <назва>"));
                return true;
            }

            String arenaName = args[2];
            Arena existingArena = plugin.getArenaManager().getArena(arenaName);

            if (existingArena == null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "arena.not-found", "arena", arenaName));
                return true;
            }

            plugin.getArenaManager().cacheArena(existingArena);
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.edit-mode", "arena", arenaName));
            return true;
        }

        // Для addpillar потрібен номер стовпа
        if (subCommand.equals("addpillar")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "commands.usage", "usage", "/gp setup addpillar <номер> [arena]"));
                return true;
            }

            int pillarNumber;
            try {
                pillarNumber = Integer.parseInt(args[2]);
                if (pillarNumber < 1) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                            "errors.invalid-number"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "errors.invalid-number"));
                return true;
            }

            String arenaName = args.length >= 4 ? args[3] : "default";
            Arena arena = plugin.getArenaManager().getArena(arenaName);

            if (arena == null) {
                arena = new Arena(arenaName);
            }

            Location loc = player.getLocation();

            // Додаємо або замінюємо стовп з конкретним номером
            arena.setPillar(pillarNumber - 1, loc);
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                    "arena.setup.pillar-added", "number", String.valueOf(pillarNumber)));

            // Тимчасово зберігаємо в кеші (не зберігаємо в файл)
            plugin.getArenaManager().cacheArena(arena);
            return true;
        }

        // Для інших команд arena name може бути args[2]
        String arenaName = args.length >= 3 ? args[2] : "default";
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            arena = new Arena(arenaName);
        }

        Location loc = player.getLocation();

        switch (subCommand) {

            case "lobby":
                arena.setLobby(loc);
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("arena.setup.lobby-set"));
                break;

            case "spectator":
                arena.setSpectator(loc);
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("arena.setup.spectator-set"));
                break;

            case "setworld":
                String worldName = player.getWorld().getName();
                arena.setWorldName(worldName);
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "arena.setup.world-set", "world", worldName));
                break;

            case "save":
                // Детальна перевірка налаштування арени
                if (arena.getWorldName() == null) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-setup"));
                    player.sendMessage("§c  Не встановлено світ! Використай: /gp setup setworld");
                    return true;
                }
                if (arena.getLobby() == null) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-setup"));
                    player.sendMessage("§c  Не встановлено лобі! Використай: /gp setup lobby");
                    return true;
                }
                if (arena.getSpectator() == null) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-setup"));
                    player.sendMessage("§c  Не встановлено точку спостерігача! Використай: /gp setup spectator");
                    return true;
                }
                if (arena.getPillarCount() == 0) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-setup"));
                    player.sendMessage("§c  Не встановлено жодного стовпа! Використай: /gp setup addpillar <номер>");
                    return true;
                }

                plugin.getArenaManager().saveArena(arena);
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "arena.setup.saved", "arena", arena.getName()));
                break;

            default:
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "commands.usage", "usage",
                        "/gp setup <create|edit|addpillar|lobby|spectator|setworld|save> [параметри]"));
                return true;
        }

        // Кешуємо арену для подальших команд
        plugin.getArenaManager().cacheArena(arena);
        return true;
    }

    private boolean handleSetHub(CommandSender sender) {
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

    private boolean handleReload(CommandSender sender) {
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
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("join", "leave", "setup", "sethub", "reload");
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            List<String> setupCommands = Arrays.asList("create", "edit", "addpillar", "lobby", "spectator", "setworld",
                    "save");
            for (String sub : setupCommands) {
                if (sub.startsWith(args[1].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setup") &&
                (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("edit"))) {
            // Для create/edit - назви арен (лише для edit показуємо існуючі)
            if (args[1].equalsIgnoreCase("edit")) {
                for (Arena arena : plugin.getArenaManager().getArenas()) {
                    if (arena.getName().startsWith(args[2].toLowerCase())) {
                        completions.add(arena.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("addpillar")) {
            // Підказка номера стовпа
            for (int i = 1; i <= 16; i++) {
                completions.add(String.valueOf(i));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("setup") &&
                !args[1].equalsIgnoreCase("addpillar") &&
                !args[1].equalsIgnoreCase("create") &&
                !args[1].equalsIgnoreCase("edit")) {
            // Для інших команд setup - назва арени
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getName().startsWith(args[2].toLowerCase())) {
                    completions.add(arena.getName());
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("setup") && args[1].equalsIgnoreCase("addpillar")) {
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getName().startsWith(args[2].toLowerCase())) {
                    completions.add(arena.getName());
                }
            }
        }

        return completions;
    }
}
