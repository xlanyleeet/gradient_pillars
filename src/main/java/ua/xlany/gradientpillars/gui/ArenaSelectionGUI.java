package ua.xlany.gradientpillars.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.managers.ArenaManager;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

import java.util.ArrayList;
import java.util.List;

public class ArenaSelectionGUI {

    private final GradientPillars plugin;
    private final ChestGui gui;

    public ArenaSelectionGUI(GradientPillars plugin) {
        this.plugin = plugin;

        // Get legacy title for ChestGui using MessageManager helper
        String title = plugin.getMessageManager().getLegacyString("gui.arena-selection.title");

        this.gui = new ChestGui(6, title);

        // Disable clicks outside by default
        this.gui.setOnGlobalClick(event -> event.setCancelled(true));

        // Auto-update task when GUI is open
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gui.getViewers().isEmpty()) {
                    return; // Don't update if no one is watching
                }
                update();
                gui.update();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        update();
    }

    public void open(Player player) {
        gui.show(player);
    }

    public void update() {
        OutlinePane pane = new OutlinePane(0, 0, 9, 6);

        ArenaManager arenaManager = plugin.getArenaManager();
        if (arenaManager == null)
            return;

        List<Arena> arenas = new ArrayList<>(arenaManager.getArenas());

        for (Arena arena : arenas) {
            ItemStack item = createArenaItem(arena);
            GuiItem guiItem = new GuiItem(item, event -> {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    plugin.getGameManager().joinGame(player, arena.getName());
                    player.closeInventory();
                }
            });
            pane.addItem(guiItem);
        }

        gui.addPane(pane);
    }

    private ItemStack createArenaItem(Arena arena) {
        Game game = plugin.getGameManager().findGameByArena(arena.getName());
        GameState state = (game != null) ? game.getState() : GameState.WAITING;
        int playerCount = (game != null) ? game.getPlayerCount() : 0;

        Material material;
        String stateKey;

        switch (state) {
            case WAITING:
                material = Material.LIME_WOOL;
                stateKey = "available";
                break;
            case COUNTDOWN:
                material = Material.ORANGE_WOOL;
                stateKey = "starting";
                break;
            case ACTIVE:
                material = Material.RED_WOOL;
                stateKey = "in-progress";
                break;
            case ENDING:
                material = Material.RED_WOOL;
                stateKey = "ending";
                break;
            default:
                material = Material.GRAY_WOOL;
                stateKey = "unknown";
        }

        // Get value as string for replacement
        String stateName = plugin.getMessageManager().getMessage("gui.arena-selection.status." + stateKey);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Using Adventure Component for display name
            Component nameComponent = plugin.getMessageManager().getComponent("gui.arena-selection.item-name",
                    "arena", arena.getName());
            meta.displayName(nameComponent);

            List<Component> lore = new ArrayList<>();

            // Status line
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.status",
                    "status", stateName));

            // Players line
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.players",
                    "current", String.valueOf(playerCount),
                    "max", String.valueOf(arena.getMaxPlayers())));

            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getComponent("gui.arena-selection.info.click-to-join"));

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}
