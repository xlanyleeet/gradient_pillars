package ua.xlany.gradientpillars.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ArenaSelectionGUI {

    private final GradientPillars plugin;

    public ArenaSelectionGUI(GradientPillars plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        arenas.sort(Comparator.comparing(Arena::getName)); // Sort alphabetically for consistency

        Component title = plugin.getMessageManager().getComponent("gui.arena-selection.title");

        // Create GUI (6 rows to accommodate pagination controls if needed)
        ChestGui gui = new ChestGui(6, LegacyComponentSerializer.legacySection().serialize(title));

        // Disable item interactions
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // Create a paginated pane for arenas
        PaginatedPane pages = new PaginatedPane(0, 0, 9, 5); // 9x5 area

        List<GuiItem> items = new ArrayList<>();
        for (Arena arena : arenas) {
            Game game = plugin.getGameManager().getGameByArena(arena.getName());
            ItemStack itemStack = createArenaItem(arena, game);

            GuiItem guiItem = new GuiItem(itemStack, event -> {
                // Determine if we can join
                if (!arena.isSetup())
                    return;

                // Join game logic
                player.closeInventory();
                plugin.getGameManager().joinGame(player, arena.getName());
            });

            items.add(guiItem);
        }

        pages.populateWithGuiItems(items);
        gui.addPane(pages);

        // Add navigation controls if multiple pages
        if (pages.getPages() > 1) {
            StaticPane navigation = new StaticPane(0, 5, 9, 1);

            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.displayName(plugin.getMessageManager().getComponent("gui.arena-selection.previous-page"));
            prevItem.setItemMeta(prevMeta);

            navigation.addItem(new GuiItem(prevItem, event -> {
                if (pages.getPage() > 0) {
                    pages.setPage(pages.getPage() - 1);
                    gui.update();
                }
            }), 0, 0);

            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.displayName(plugin.getMessageManager().getComponent("gui.arena-selection.next-page"));
            nextItem.setItemMeta(nextMeta);

            navigation.addItem(new GuiItem(nextItem, event -> {
                if (pages.getPage() < pages.getPages() - 1) {
                    pages.setPage(pages.getPage() + 1);
                    gui.update();
                }
            }), 8, 0);

            gui.addPane(navigation);
        }

        gui.show(player);
    }

    private ItemStack createArenaItem(Arena arena, Game game) {
        Material material;
        List<Component> lore = new ArrayList<>();

        if (!arena.isSetup()) {
            material = Material.RED_CONCRETE;
            addLore(lore, "status.not-setup");
            lore.add(Component.empty());
            addLore(lore, "info.missing");
            if (arena.getPillars().isEmpty())
                addLore(lore, "info.missing-pillars");
            lore.add(Component.empty());
            addLore(lore, "info.unavailable");
        } else if (game == null || game.getState() == GameState.WAITING) {
            int current = game != null ? game.getPlayerCount() : 0;

            material = Material.LIME_CONCRETE;
            addLore(lore, "status.available");
            lore.add(Component.empty());
            addLore(lore, "info.players", "current", String.valueOf(current), "max",
                    String.valueOf(arena.getMaxPlayers()));
            addLore(lore, "info.minimum", "min", String.valueOf(arena.getMinPlayers()));
            lore.add(Component.empty());
            addLore(lore, "info.click-to-join");
        } else {
            GameState state = game.getState();
            int current = game.getPlayerCount();
            int max = arena.getMaxPlayers();

            switch (state) {
                case COUNTDOWN -> {
                    material = Material.YELLOW_CONCRETE;
                    addLore(lore, "status.starting");
                    lore.add(Component.empty());
                    addLore(lore, "info.players", "current", String.valueOf(current), "max", String.valueOf(max));
                    addLore(lore, "info.countdown", "time", String.valueOf(game.getCountdownTimeLeft()));
                    lore.add(Component.empty());
                    addLore(lore, "info.click-to-join");
                }
                case ACTIVE -> {
                    material = Material.ORANGE_CONCRETE;
                    addLore(lore, "status.in-progress");
                    lore.add(Component.empty());
                    addLore(lore, "info.players", "current", String.valueOf(current), "max", String.valueOf(max));
                    lore.add(Component.empty());
                    addLore(lore, "info.cannot-join");
                }
                case ENDING, RESTORING -> {
                    material = Material.PURPLE_CONCRETE;
                    addLore(lore, "status.ending");
                    lore.add(Component.empty());
                    addLore(lore, "info.cannot-join");
                }
                default -> {
                    material = Material.GRAY_CONCRETE;
                    addLore(lore, "status.unknown");
                }
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(
                    plugin.getMessageManager().getComponent("gui.arena-selection.item-name", "arena", arena.getName()));
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void addLore(List<Component> lore, String suffix, String... args) {
        lore.add(plugin.getMessageManager().getComponent("gui.arena-selection." + suffix, args));
    }
}
