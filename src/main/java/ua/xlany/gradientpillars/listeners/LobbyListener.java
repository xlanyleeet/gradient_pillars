package ua.xlany.gradientpillars.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

public class LobbyListener implements Listener {

    private final GradientPillars plugin;

    public LobbyListener(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.RED_BED) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        // Перевірка, що гравець в лобі (не в активній грі)
        if (game != null && (game.getState() == GameState.WAITING || game.getState() == GameState.COUNTDOWN)) {
            event.setCancelled(true);
            plugin.getGameManager().leaveGame(player);
        }
    }
}
