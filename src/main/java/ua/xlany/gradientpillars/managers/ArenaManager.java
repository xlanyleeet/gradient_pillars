package ua.xlany.gradientpillars.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final GradientPillars plugin;
    private final Map<String, Arena> arenas;
    private final Map<String, Arena> arenaCache;

    public ArenaManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenaCache = new HashMap<>();
        loadArenas();
    }

    public void reload() {
        arenas.clear();
        loadArenas();
    }

    public void loadArenas() {
        arenas.clear();

        File arenasFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
            return;
        }

        File[] directories = arenasFolder.listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            plugin.getLogger().warning("Не знайдено жодної арени!");
            return;
        }

        for (File arenaDir : directories) {
            String arenaName = arenaDir.getName();
            File configFile = new File(arenaDir, "config.yml");

            if (configFile.exists()) {
                Arena arena = loadArena(configFile, arenaName);
                if (arena != null) {
                    arenas.put(arenaName, arena);
                    plugin.getLogger().info("Завантажено арену: " + arenaName);
                }
            }
        }
    }

    public boolean createArena(String name) {
        File arenaDir = new File(plugin.getDataFolder(), "arenas/" + name);
        if (arenaDir.exists()) {
            return false;
        }

        arenaDir.mkdirs();
        Arena arena = new Arena(name);
        arenaCache.put(name, arena);

        plugin.getLogger().info("Створено нову арену: " + name);
        return true;
    }

    private Arena loadArena(File file, String name) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        Arena arena = new Arena(name);

        String worldName = config.getString("arena.world");
        if (worldName != null) {
            arena.setWorldName(worldName);
        }

        World world = worldName != null ? Bukkit.getWorld(worldName) : null;

        Location lobby = loadLocation(config, "arena.lobby");
        if (lobby != null) {
            if (world != null) {
                lobby.setWorld(world);
            }
            arena.setLobby(lobby);
        }

        Location spectator = loadLocation(config, "arena.spectator");
        if (spectator != null) {
            if (world != null) {
                spectator.setWorld(world);
            }
            arena.setSpectator(spectator);
        }

        ConfigurationSection pillarsSection = config.getConfigurationSection("arena.pillars");
        if (pillarsSection != null) {
            for (String key : pillarsSection.getKeys(false)) {
                Location pillar = loadLocation(config, "arena.pillars." + key);
                if (pillar != null) {
                    if (world != null) {
                        pillar.setWorld(world);
                    }
                    arena.addPillar(pillar);
                }
            }
        }

        // Завантажити налаштування гравців
        arena.setMinPlayers(config.getInt("arena.min-players", 2));
        arena.setMaxPlayers(config.getInt("arena.max-players", 16));

        return arena;
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.contains(path)) {
            return null;
        }

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");

        return new Location(null, x, y, z, yaw, pitch);
    }

    public void saveArena(Arena arena) {
        File arenaDir = new File(plugin.getDataFolder(), "arenas/" + arena.getName());
        if (!arenaDir.exists()) {
            arenaDir.mkdirs();
        }

        File configFile = new File(arenaDir, "config.yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("arena.world", arena.getWorldName());

        if (arena.getLobby() != null) {
            saveLocation(config, "arena.lobby", arena.getLobby());
        }

        if (arena.getSpectator() != null) {
            saveLocation(config, "arena.spectator", arena.getSpectator());
        }

        List<Location> pillars = arena.getPillars();
        for (int i = 0; i < pillars.size(); i++) {
            Location pillar = pillars.get(i);
            if (pillar != null) {
                saveLocation(config, "arena.pillars." + i, pillar);
            }
        }

        // Зберегти налаштування гравців
        config.set("arena.min-players", arena.getMinPlayers());
        config.set("arena.max-players", arena.getMaxPlayers());

        try {
            config.save(configFile);

            // Створити ZIP-бекап світу при збереженні арени
            if (arena.getWorldName() != null && !arena.getWorldName().isEmpty()) {
                File backupZip = new File(arenaDir, "world_backup.zip");
                plugin.getWorldManager().createBackup(arena.getWorldName(), backupZip);
            }

            arenas.put(arena.getName(), arena);
            // Видалити з кешу після збереження
            arenaCache.remove(arena.getName());

            // Логування деталей збереження
            plugin.getLogger().info("Збережено арену: " + arena.getName());
            plugin.getLogger().info("  Світ: " + arena.getWorldName());
            plugin.getLogger().info("  Лобі: " + (arena.getLobby() != null ? "встановлено" : "не встановлено"));
            plugin.getLogger()
                    .info("  Спектатор: " + (arena.getSpectator() != null ? "встановлено" : "не встановлено"));
            plugin.getLogger().info("  Стовпів: " + arena.getPillarCount());
            plugin.getLogger().info("  Мін. гравців: " + arena.getMinPlayers());
            plugin.getLogger().info("  Макс. гравців: " + arena.getMaxPlayers());
            plugin.getLogger().info("  Налаштована: " + (arena.isSetup() ? "ТАК" : "НІ"));
        } catch (IOException e) {
            plugin.getLogger().severe("Помилка при збереженні арени: " + e.getMessage());
        }
    }

    private void saveLocation(FileConfiguration config, String path, Location loc) {
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    public Arena getArena(String name) {
        // Спочатку перевіряємо кеш (для арен в процесі налаштування)
        if (arenaCache.containsKey(name)) {
            return arenaCache.get(name);
        }
        return arenas.get(name);
    }

    public void cacheArena(Arena arena) {
        arenaCache.put(arena.getName(), arena);
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getFirstAvailableArena() {
        return arenas.values().stream()
                .filter(Arena::isSetup)
                .findFirst()
                .orElse(null);
    }

    /**
     * Видалити арену
     * 
     * @param name назва арени
     * @return true якщо успішно видалено
     */
    public boolean deleteArena(String name) {
        Arena arena = getArena(name);
        if (arena == null) {
            return false;
        }

        try {
            // Видалити з мапи
            arenas.remove(name);
            arenaCache.remove(name);

            // Видалити папку арени повністю (включаючи всі файли)
            File arenaDir = new File(plugin.getDataFolder(), "arenas/" + name);
            if (arenaDir.exists()) {
                deleteDirectory(arenaDir);
            }

            plugin.getLogger().info("Видалено арену: " + name);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Помилка при видаленні арени " + name + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Рекурсивно видалити директорію з усім вмістом
     * 
     * @param directory папка для видалення
     * @return true якщо успішно
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    // Відновлення світу
    public boolean restoreWorld(String arenaName) {
        if (!plugin.getConfigManager().isAutoRestore()) {
            return true;
        }

        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            plugin.getLogger().warning("Арену не знайдено: " + arenaName);
            return false;
        }

        String worldName = arena.getWorldName();
        File arenaDir = new File(plugin.getDataFolder(), "arenas/" + arenaName);
        File backupZip = new File(arenaDir, "world_backup.zip");

        // Використовуємо метод відновлення з ZIP
        plugin.getWorldManager().restoreFromBackup(worldName, backupZip);
        return true;
    }
}
