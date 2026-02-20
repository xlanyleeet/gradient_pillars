package ua.xlany.gradientpillars.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArenaSelectionGUI {

    private final GradientPillars plugin;

    public ArenaSelectionGUI(GradientPillars plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas());
        arenas.sort(Comparator.comparing(Arena::getName));

        Component title = plugin.getMessageManager().getComponent("gui.arena-selection.title");

        List<Item> items = new ArrayList<>();
        for (Arena arena : arenas) {
            Game game = plugin.getGameManager().getGameByArena(arena.getName());
            ItemStack itemStack = createArenaItem(arena, game);

            items.add(new SimpleItem(new ItemBuilder(itemStack), click -> {
                if (!arena.isSetup()) {
                    return;
                }
                plugin.getGameManager().joinGame(player, arena.getName());
            }));
        }

        Gui gui = PagedGui.items()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "# # # < # > # # #")
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        ItemBuilder builder = new ItemBuilder(Material.ARROW);
                        builder.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                                plugin.getMessageManager().getComponent("gui.arena-selection.previous-page")));
                        return builder;
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        ItemBuilder builder = new ItemBuilder(Material.ARROW);
                        builder.setDisplayName(LegacyComponentSerializer.legacySection().serialize(
                                plugin.getMessageManager().getComponent("gui.arena-selection.next-page")));
                        return builder;
                    }
                })
                .setContent(items)
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(LegacyComponentSerializer.legacySection().serialize(title))
                .setGui(gui)
                .build();

        window.open();
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
                    addLore(lore, "info.click-to-spectate"); // Changed key
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
