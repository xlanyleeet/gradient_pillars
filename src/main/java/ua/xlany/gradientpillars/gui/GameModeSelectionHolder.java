package ua.xlany.gradientpillars.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import ua.xlany.gradientpillars.models.Game;

/**
 * Holder для GUI вибору режиму гри
 */
public class GameModeSelectionHolder implements InventoryHolder {

    private final Game game;

    public GameModeSelectionHolder(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public Inventory getInventory() {
        return null; // Не використовується
    }
}
