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

        if (item == null) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        // Перевірка на предмет виходу з лобі
        if (item.getType() == Material.RED_BED) {
            // Перевірка, що гравець в лобі (не в активній грі)
            if (game != null && (game.getState() == GameState.WAITING || game.getState() == GameState.COUNTDOWN)) {
                event.setCancelled(true);
                plugin.getGameManager().leaveGame(player);
            }
        }

        // Перевірка на годинник пропуску очікування
        if (item.getType() == Material.CLOCK) {
            if (game != null && game.getState() == GameState.COUNTDOWN) {
                event.setCancelled(true);

                // Перевірка прав
                if (!player.hasPermission("gradientpillars.skipwait")) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("commands.no-permission"));
                    return;
                }

                // Скоротити таймер до 10 секунд
                plugin.getGameManager().skipCountdown(game);
                item.setAmount(0); // Видалити предмет після використання

                // Повідомити всіх гравців
                for (java.util.UUID playerId : game.getPlayers()) {
                    Player p = org.bukkit.Bukkit.getPlayer(playerId);
                    if (p != null) {
                        p.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                                "game.start.skip-wait", "player", player.getName()));
                    }
                }
            }
        }
    }
}
