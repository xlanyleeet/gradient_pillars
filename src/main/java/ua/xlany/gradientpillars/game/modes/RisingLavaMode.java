package ua.xlany.gradientpillars.game.modes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

public class RisingLavaMode implements GameModeHandler {

    private final GradientPillars plugin;

    public RisingLavaMode(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @Override
    public void apply(Game game) {
        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena == null)
            return;

        game.setCurrentLavaY(arena.getMinY());
        game.setMaxLavaY(arena.getMaxY());

        long maxGameDuration = plugin.getConfigManager().getMaxGameDuration();
        int totalHeight = arena.getMaxY() - arena.getMinY();
        long ticksBetweenRises = (maxGameDuration * 20) / Math.max(totalHeight, 1);

        ticksBetweenRises = Math.max(20, ticksBetweenRises);

        Location center = arena.getSpectator();
        if (center == null && !arena.getPillars().isEmpty()) {
            center = arena.getPillars().get(0);
        }

        if (center == null) {
            return; // Cannot determine center
        }

        final int centerX = center.getBlockX();
        final int centerZ = center.getBlockZ();

        final int radius = 15; // Фіксований радіус 15 блоків
        final int radiusSquared = radius * radius;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (game.getState() != GameState.ACTIVE) {
                return;
            }

            int currentY = game.getCurrentLavaY();
            int maxY = game.getMaxLavaY();

            // Перевірити чи лава досягла максимальної висоти
            if (currentY >= maxY) {
                return;
            }

            // Підняти лаву на 1 блок
            currentY++;
            game.setCurrentLavaY(currentY);

            // Отримати світ
            if (arena.getWorldName() == null) {
                return;
            }
            World world = Bukkit.getWorld(arena.getWorldName());
            if (world == null) {
                return;
            }

            // Оптимізоване заповнення колом
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    // Перевірка дистанції (використовуємо квадрат радіуса для уникнення повільного
                    // Math.sqrt)
                    if (Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2) <= radiusSquared) {
                        Location blockLoc = new Location(world, x, currentY, z);
                        // Перевіряємо чи чанк завантажений, щоб не створювати лаги
                        if (world.isChunkLoaded(x >> 4, z >> 4)) {
                            if (world.getBlockAt(blockLoc).getType() == Material.AIR) {
                                world.getBlockAt(blockLoc).setType(Material.LAVA);
                            }
                        }
                    }
                }
            }

        }, 0L, ticksBetweenRises);

        game.setLavaTask(task.getTaskId());
    }

    @Override
    public void cleanup(Game game) {
        if (game.getLavaTask() != 0) {
            Bukkit.getScheduler().cancelTask(game.getLavaTask());
        }
    }
}
