package ua.xlany.gradientpillars.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpectatorManager {

    private final GradientPillars plugin;

    public SpectatorManager(GradientPillars plugin) {
        this.plugin = plugin;
    }

    public void addSpectator(Game game, Player player) {
        // Додати в список спектаторів гри
        game.addSpectator(player.getUniqueId());

        // Видалити зі списку живих гравців, але залишити в загальному списку,
        // щоб гравець не викидався з гри повністю (якщо логіка дозволяє)
        // В поточній моделі Game, players - це всі, хто в грі. alivePlayers - живі.
        game.eliminatePlayer(player.getUniqueId());

        // Очистити інвентар та ефекти
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);

        // Встановити режим спектатора
        player.setGameMode(GameMode.SPECTATOR);

        // Телепортувати на точку спектатора
        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena != null) {
            Location spectatorLocation = arena.getSpectator();
            if (spectatorLocation != null) {
                player.teleport(spectatorLocation);
            } else if (!arena.getPillars().isEmpty()) {
                // Fallback: над першим стовпом
                player.teleport(arena.getPillars().get(0).clone().add(0, 10, 0));
            }
        }

        // Видати предмети спектатора в хотбар (Spectator mode не дозволяє
        // використовувати предмети,
        // але дозволяє клікати в меню, якщо це Adventure + Fly,
        // проте в чистому Spectator mode тільки хотбар для teleport to players)
        // Ванільний Spectator mode має своє меню телепортації на цифри 1-9 або клік
        // мишкою.
        // Тому кастомні предмети можуть бути зайві, якщо ми використовуємо чистий GM 3.
        // Але ми можемо дати компас для свого GUI, який відкривається на ПКМ.
        giveSpectatorItems(player);

        // Повідомлення
        player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.spectator.join"));

        // Сховати від живих (хоча GM 3 робить це автоматично, але для надійності)
        // Та показати іншим спектаторам
        updateVisibility(game);
    }

    public void removeSpectator(Game game, Player player) {
        // Видалити зі списку спектаторів
        game.removeSpectator(player.getUniqueId());

        // Відновити видимість
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(plugin, player);
        }

        // Подальші дії (телепорт в лобі, очистка інвентарю тощо)
        // зазвичай виконуються в GameManager.leaveGame
    }

    private void giveSpectatorItems(Player player) {
        // Компас для меню гравців
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.items.spectator_menu.name"));
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager().getComponent("game.items.spectator_menu.description"));
            meta.lore(lore);
            compass.setItemMeta(meta);
        }
        player.getInventory().setItem(0, compass);

        // Ліжко для виходу в лобі
        ItemStack bed = new ItemStack(Material.RED_BED);
        ItemMeta bedMeta = bed.getItemMeta();
        if (bedMeta != null) {
            bedMeta.displayName(plugin.getMessageManager().getComponent("game.items.leave_game.name"));
            bed.setItemMeta(bedMeta);
        }
        player.getInventory().setItem(8, bed);
    }

    private void updateVisibility(Game game) {
        for (UUID specId : game.getSpectators()) {
            Player spec = Bukkit.getPlayer(specId);
            if (spec == null)
                continue;

            // Спектатор бачить всіх живих
            for (UUID aliveId : game.getAlivePlayers()) {
                Player alive = Bukkit.getPlayer(aliveId);
                if (alive != null) {
                    spec.showPlayer(plugin, alive);
                    // Живі не бачать спектатора (автоматично в GM 3, але можна форсувати)
                    alive.hidePlayer(plugin, spec);
                }
            }

            // Спектатор бачить інших спектаторів
            for (UUID otherSpecId : game.getSpectators()) {
                if (specId.equals(otherSpecId))
                    continue;
                Player otherSpec = Bukkit.getPlayer(otherSpecId);
                if (otherSpec != null) {
                    spec.showPlayer(plugin, otherSpec);
                }
            }
        }
    }
}
