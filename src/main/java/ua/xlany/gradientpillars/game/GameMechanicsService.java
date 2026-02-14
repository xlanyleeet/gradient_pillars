package ua.xlany.gradientpillars.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Game;

import java.util.UUID;

public class GameMechanicsService {

    public GameMechanicsService(GradientPillars plugin) {
    }

    /**
     * Створює клітку зі скла навколо гравця (3x3x3 по периметру)
     * 
     * @param game     Гра до якої відноситься клітка
     * @param playerId UUID гравця
     * @param center   Центр клітки (позиція гравця)
     */
    public void createGlassCage(Game game, UUID playerId, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Створюємо клітку 3x3x3
        // Підлога (Y-1): всі 9 блоків
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = new Location(world, centerX + x, centerY - 1, centerZ + z);
                world.getBlockAt(blockLoc).setType(Material.GLASS);
                game.addCageBlock(playerId, blockLoc);
            }
        }

        // Стіни (Y та Y+1): тільки по периметру
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    // Пропускаємо центральний блок (там де гравець)
                    if (x != 0 || z != 0) {
                        Location blockLoc = new Location(world, centerX + x, centerY + y, centerZ + z);
                        world.getBlockAt(blockLoc).setType(Material.GLASS);
                        game.addCageBlock(playerId, blockLoc);
                    }
                }
            }
        }

        // Дах (Y+2): всі 9 блоків
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = new Location(world, centerX + x, centerY + 2, centerZ + z);
                world.getBlockAt(blockLoc).setType(Material.GLASS);
                game.addCageBlock(playerId, blockLoc);
            }
        }
    }
}