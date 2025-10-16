package ua.xlany.gradientpillars.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

public class WorldListener implements Listener {

    private final GradientPillars plugin;

    public WorldListener(GradientPillars plugin) {
        this.plugin = plugin;
    }

    /**
     * Перевірити чи гравець знаходиться в світі арени
     */
    private boolean isInArenaWorld(Player player) {
        World world = player.getWorld();

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getWorldName() != null && arena.getWorldName().equals(world.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Перевірити чи гравець може взаємодіяти з світом (не спектатор)
     */
    private boolean canInteract(Player player) {
        var game = plugin.getGameManager().getPlayerGame(player.getUniqueId());

        // Якщо гравець не в грі, але в світі арени - заборонити
        if (isInArenaWorld(player) && game == null) {
            return false;
        }

        // Якщо гравець в грі, але мертвий (спектатор) - заборонити
        if (game != null && !game.isPlayerAlive(player.getUniqueId())) {
            return false;
        }

        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!canInteract(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!canInteract(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Спектатори не отримують шкоду
        var game = plugin.getGameManager().getPlayerGame(player.getUniqueId());
        if (game != null && !game.isPlayerAlive(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Гравці не в грі, але в світі арени - теж не отримують шкоду
        if (isInArenaWorld(player) && game == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        // Спектатори не можуть атакувати
        if (!canInteract(damager)) {
            event.setCancelled(true);
            return;
        }

        // Не можна атакувати спектаторів
        var victimGame = plugin.getGameManager().getPlayerGame(victim.getUniqueId());
        if (victimGame != null && !victimGame.isPlayerAlive(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!canInteract(player)) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        if (!canInteract(player)) {
            event.setCancelled(true);
        }
    }
}
