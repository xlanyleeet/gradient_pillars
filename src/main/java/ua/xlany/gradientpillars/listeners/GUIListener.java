package ua.xlany.gradientpillars.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.gui.ArenaSelectionGUI;

public class GUIListener implements Listener {

    private final GradientPillars plugin;

    public GUIListener(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Перевіряємо чи це GUI вибору арени
        String title = event.getView().title().toString();
        if (!title.contains("Виберіть арену")) {
            return;
        }

        // Блокуємо взаємодію з GUI
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        int slot = event.getSlot();
        ArenaSelectionGUI gui = new ArenaSelectionGUI(plugin);
        String arenaName = gui.getArenaNameFromSlot(slot);

        if (arenaName != null) {
            player.closeInventory();
            
            // Приєднуємось до арени
            plugin.getGameManager().joinGame(player, arenaName);
        }
    }
}
