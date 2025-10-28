package ua.xlany.gradientpillars.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

public class GameListener implements Listener {

    private final GradientPillars plugin;

    public GameListener(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        if (game != null && game.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        if (game != null && game.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Перевірка максимальної висоти будівництва
        if (game != null && game.getState() == GameState.ACTIVE) {
            Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
            if (arena != null) {
                int blockY = event.getBlock().getY();
                if (blockY > arena.getMaxY()) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                            "game.build-limit", "y", String.valueOf(arena.getMaxY())));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        if (game != null && game.getState() == GameState.ACTIVE) {
            Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
            if (arena != null) {
                double playerY = player.getLocation().getY();
                if (playerY < arena.getMinY()) {
                    player.setHealth(0.0);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Game game = plugin.getGameManager().getPlayerGame(victim.getUniqueId());

            if (game != null && game.getState() != GameState.ACTIVE) {
                event.setCancelled(true);
            }
        }

        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Game game = plugin.getGameManager().getPlayerGame(attacker.getUniqueId());

            if (game != null && game.getState() != GameState.ACTIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

            if (game != null && game.getState() != GameState.ACTIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        if (game != null && game.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        if (game != null && game.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
        }
    }
}
