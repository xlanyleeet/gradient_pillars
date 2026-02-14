package ua.xlany.gradientpillars.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.gui.ArenaSelectionGUI;
import ua.xlany.gradientpillars.gui.ArenaSelectionHolder;
import ua.xlany.gradientpillars.gui.GameModeSelectionGUI;
import ua.xlany.gradientpillars.gui.GameModeSelectionHolder;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameMode;

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

        // Перевіряємо чи це GUI вибору режиму через InventoryHolder
        if (event.getInventory().getHolder() instanceof GameModeSelectionHolder holder) {
            // Блокуємо взаємодію з GUI
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            Game game = holder.getGame();
            int slot = event.getSlot();

            // Визначити який режим було обрано на основі слоту (слоти 10-16 для 7 режимів)
            if (slot >= 10 && slot <= 16) {
                int modeIndex = slot - 10;
                GameMode[] modes = GameMode.values();
                if (modeIndex < modes.length) {
                    GameMode selectedMode = modes[modeIndex];
                    
                    // Зареєструвати голос
                    game.voteForMode(player.getUniqueId(), selectedMode);

                    // Повідомити гравця
                    String modeName = plugin.getMessageManager().getMessage(selectedMode.getTranslationKey() + ".name");
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                            "game.mode.voted",
                            "mode", modeName));

                    // Оновити GUI для відображення нового голосу
                    GameModeSelectionGUI gui = new GameModeSelectionGUI(plugin, game);
                    gui.open(player);
                }
            }
            return;
        }

        // Перевіряємо чи це GUI вибору арени через InventoryHolder
        if (!(event.getInventory().getHolder() instanceof ArenaSelectionHolder)) {
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
