package ua.xlany.gradientpillars.listeners;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
            // Очистити дропи
            event.setKeepInventory(false);
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Видалити повідомлення про смерть
            event.deathMessage(null);

            // Обробити смерть гравця (елімінація, повідомлення)
            plugin.getGameManager().handlePlayerDeath(player);

            // Автоматичний respawn
            player.getScheduler().run(plugin, (t) -> {
                player.spigot().respawn();
            }, null);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Якщо гравець в грі - встановити точку респавну на спектатора
        var game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game != null) {
            Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
            if (arena != null && arena.getSpectator() != null) {
                Location spectator = arena.getSpectator().clone();

                // Переконатись що світ встановлено
                if (spectator.getWorld() == null && arena.getWorldName() != null) {
                    World world = Bukkit.getWorld(arena.getWorldName());
                    if (world != null) {
                        spectator.setWorld(world);
                    }
                }

                event.setRespawnLocation(spectator);

                // Якщо гравець мертвий (не живий в грі) - одразу встановити режим спектатора
                if (!game.isPlayerAlive(player.getUniqueId())) {
                    player.getScheduler().run(plugin, (t) -> {
                        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                        player.sendMessage(
                                plugin.getMessageManager().getPrefixedComponent("game.spectator.now-spectating"));
                    }, null);
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
