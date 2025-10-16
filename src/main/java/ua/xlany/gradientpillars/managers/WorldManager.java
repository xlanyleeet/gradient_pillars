package ua.xlany.gradientpillars.managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import ua.xlany.gradientpillars.GradientPillars;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class WorldManager {

    private final GradientPillars plugin;

    public WorldManager(GradientPillars plugin) {
        this.plugin = plugin;
    }

    /**
     * Створити бекап світу
     */
    public boolean createBackup(String worldName, File backupFolder) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Світ не знайдено для бекапу: " + worldName);
            return false;
        }

        try {
            // Зберегти світ перед копіюванням
            world.save();
            plugin.getLogger().info("Створюю бекап світу " + worldName + "...");

            File worldFolder = world.getWorldFolder();

            // Видалити старий бекап якщо існує
            if (backupFolder.exists()) {
                deleteDirectory(backupFolder.toPath());
                plugin.getLogger().info("Видалено старий бекап");
            }

            // Створити новий бекап
            copyDirectory(worldFolder.toPath(), backupFolder.toPath());

            long fileCount = countFiles(backupFolder.toPath());
            plugin.getLogger().info("Створено бекап світу " + worldName + " (файлів: " + fileCount + ")");

            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("Помилка при створенні бекапу світу: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Відновити світ з бекапу
     */
    public boolean restoreFromBackup(String worldName, File backupFolder) {
        if (!backupFolder.exists()) {
            plugin.getLogger().warning("Бекап не знайдено для світу: " + worldName);
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Світ не знайдено: " + worldName);
            return false;
        }

        try {
            plugin.getLogger().info("Відновлюю світ " + worldName + "...");

            // Вивантажити світ
            Bukkit.unloadWorld(world, false);

            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);

            // Видалити поточний світ
            deleteDirectory(worldFolder.toPath());

            // Відновити з бекапу
            copyDirectory(backupFolder.toPath(), worldFolder.toPath());

            // Завантажити світ знову
            Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));

            plugin.getLogger().info("Відновлено світ " + worldName);
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("Помилка при відновленні світу: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Копіювати директорію
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Пропустити uid.dat та session.lock
                String dirName = dir.getFileName().toString();
                if (dirName.equals("session.lock") || dirName.equals("uid.dat")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = target.resolve(source.relativize(dir));
                try {
                    Files.createDirectories(targetDir);
                } catch (IOException e) {
                    plugin.getLogger().warning("Не вдалося створити папку: " + targetDir + " - " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Пропустити session.lock та uid.dat
                String fileName = file.getFileName().toString();
                if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    Path targetFile = target.resolve(source.relativize(file));
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException e) {
                    plugin.getLogger()
                            .warning("Не вдалося скопіювати файл: " + file.getFileName() + " - " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                plugin.getLogger().warning("Не вдалося відвідати файл: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Видалити директорію
     */
    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Порахувати кількість файлів в директорії
     */
    private long countFiles(Path directory) throws IOException {
        return Files.walk(directory).filter(Files::isRegularFile).count();
    }
}
