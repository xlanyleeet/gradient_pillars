package ua.xlany.gradientpillars.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

import java.util.ArrayList;
import java.util.List;

public class ArenaSelectionGUI {

    private final GradientPillars plugin;

    public ArenaSelectionGUI(GradientPillars plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());

        // Розмір інвентаря (9, 18, 27, 36, 45, 54)
        int size = Math.min(54, ((arenas.size() + 8) / 9) * 9);
        if (size < 9)
            size = 9;

        Component title = plugin.getMessageManager().getComponent("gui.arena-selection.title");
        ArenaSelectionHolder holder = new ArenaSelectionHolder();
        Inventory gui = Bukkit.createInventory(holder, size, title);
        holder.setInventory(gui);

        int slot = 0;
        for (Arena arena : arenas) {
            if (slot >= size)
                break;

            Game game = plugin.getGameManager().getGameByArena(arena.getName());
            ItemStack item = createArenaItem(arena, game);
            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    private ItemStack createArenaItem(Arena arena, Game game) {
        Material material;
        List<Component> lore = new ArrayList<>();

        // Перевіряємо чи арена налаштована
        if (!arena.isSetup()) {
            material = Material.RED_CONCRETE;
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.not-setup"));
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.missing"));
            if (arena.getPillars().isEmpty()) {
                lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.missing-pillars"));
            }
            if (arena.getLobby() == null) {
                lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.missing-lobby"));
            }
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.unavailable"));
        } else if (game == null) {
            // Арена налаштована, але гра не створена (ніхто не грає)
            material = Material.LIME_CONCRETE;
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.available"));
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.players",
                    "current", "0", "max", String.valueOf(arena.getMaxPlayers())));
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.minimum",
                    "min", String.valueOf(arena.getMinPlayers())));
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.click-to-join"));
        } else {
            GameState state = game.getState();
            int playerCount = game.getPlayerCount();
            int minPlayers = arena.getMinPlayers();
            int maxPlayers = arena.getMaxPlayers();

            switch (state) {
                case WAITING:
                    material = Material.LIME_CONCRETE;
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.available"));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.players",
                            "current", String.valueOf(playerCount), "max", String.valueOf(maxPlayers)));
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.minimum",
                            "min", String.valueOf(minPlayers)));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.click-to-join"));
                    break;

                case COUNTDOWN:
                    material = Material.YELLOW_CONCRETE;
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.starting"));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.players",
                            "current", String.valueOf(playerCount), "max", String.valueOf(maxPlayers)));
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.countdown",
                            "time", String.valueOf(game.getCountdownTimeLeft())));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.click-to-join"));
                    break;

                case ACTIVE:
                    material = Material.ORANGE_CONCRETE;
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.in-progress"));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.players",
                            "current", String.valueOf(playerCount), "max", String.valueOf(maxPlayers)));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.cannot-join"));
                    break;

                case ENDING:
                case RESTORING:
                    material = Material.PURPLE_CONCRETE;
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.ending"));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.cannot-join"));
                    break;

                default:
                    material = Material.GRAY_CONCRETE;
                    lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.status.unknown"));
                    break;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("§6§l" + arena.getName()));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public String getArenaNameFromSlot(int slot) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        if (slot >= 0 && slot < arenas.size()) {
            return arenas.get(slot).getName();
        }
        return null;
    }
}
