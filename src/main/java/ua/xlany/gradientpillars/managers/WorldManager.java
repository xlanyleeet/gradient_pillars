package ua.xlany.gradientpillars.managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.utils.ZipUtil;

import java.io.File;

public class WorldManager {
    private final GradientPillars plugin;

    public WorldManager(GradientPillars plugin) {
        this.plugin = plugin;
    }

    public boolean createBackup(String worldName, File backupZipFile) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Світ не знайдено для бекапу: " + worldName);
            return false;
        }

        try {
            // Зберегти світ перед архівацією
            world.save();
            File worldFolder = world.getWorldFolder();
            ZipUtil.zipWorld(worldFolder, backupZipFile);
            plugin.getLogger().info("Створено ZIP-бекап: " + backupZipFile.getName()
                    + " (" + (backupZipFile.length() / 1024) + " KB)");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Помилка при створенні ZIP-бекапу: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void restoreFromBackup(String worldName, File backupZipFile) {
        if (!backupZipFile.exists()) {
            plugin.getLogger().warning("❌ ZIP-бекап не знайдено: " + backupZipFile);
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("❌ Світ не завантажено: " + worldName);
            return;
        }

        // === КРОК 1: Телепортація та вивантаження (СИНХРОННО) ===
        plugin.getLogger().info("▶ Крок 1: Телепортація гравців...");
        World defaultWorld = Bukkit.getWorlds().get(0);

        for (org.bukkit.entity.Player player : world.getPlayers()) {
            player.teleport(defaultWorld.getSpawnLocation());
        }
        // Зберегти та вивантажити світ
        world.save();

        boolean unloaded = Bukkit.unloadWorld(world, false);
        if (!unloaded) {
            plugin.getLogger().severe("❌ НЕ ВДАЛОСЯ ВИВАНТАЖИТИ СВІТ!");
            return;
        }

        // === КРОК 2: Розпакування ZIP (АСИНХРОННО) ===
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                long startTime = System.currentTimeMillis();
                ZipUtil.unzipWorld(backupZipFile, worldFolder);
                long time = System.currentTimeMillis() - startTime;
                plugin.getLogger().info("✓ Розпаковано за " + time + " мс");
                deleteWorldTrash(worldName);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("▶ Крок 4: Завантаження світу...");

                    WorldCreator wc = new WorldCreator(worldName);
                    wc.generateStructures(false);

                    World newWorld = Bukkit.createWorld(wc);
                    if (newWorld == null) {
                        plugin.getLogger().severe("❌ НЕ ВДАЛОСЯ СТВОРИТИ СВІТ!");
                        return;
                    }

                    newWorld.setAutoSave(true);
                    plugin.getArenaManager().rebindArenaWorld(worldName, newWorld);
                    plugin.getLogger().info("✓ Світ завантажено");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        newWorld.getChunkAt(newWorld.getSpawnLocation());
                    }, 20L);
                });

            } catch (Exception e) {
                plugin.getLogger().severe("Помилка: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void deleteWorldTrash(String worldName) {
        for (File file : new File[] {
                new File(Bukkit.getWorldContainer(), worldName + "/level.dat_old"),
                new File(Bukkit.getWorldContainer(), worldName + "/session.lock"),
                new File(Bukkit.getWorldContainer(), worldName + "/uid.dat")
        }) {
            if (file.exists()) {
                if (!file.delete()) {
                    plugin.getLogger().warning("Не вдалося видалити: " + file.getName());
                }
            }
        }
    }
}
