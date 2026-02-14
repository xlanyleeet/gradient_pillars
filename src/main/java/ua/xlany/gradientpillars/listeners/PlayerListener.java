package ua.xlany.gradientpillars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

public class PlayerListener implements Listener {

    private final GradientPillars plugin;

    public PlayerListener(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().isInGame(player.getUniqueId())) {
            event.setKeepInventory(false);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.deathMessage(null);
            plugin.getGameManager().handlePlayerDeath(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        var game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game != null) {
            Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
            if (arena != null && arena.getSpectator() != null) {
                Location spectator = arena.getSpectator().clone();
                if (spectator.getWorld() == null && arena.getWorldName() != null) {
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(arena.getWorldName());
                    if (world != null) {
                        spectator.setWorld(world);
                    }
                }

                event.setRespawnLocation(spectator);
                if (!game.isPlayerAlive(player.getUniqueId())) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                        player.sendMessage(
                                plugin.getMessageManager().getPrefixedComponent("game.spectator.now-spectating"));
                    });
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().isInGame(player.getUniqueId())) {
            plugin.getGameManager().leaveGame(player);
        }
    }
}
