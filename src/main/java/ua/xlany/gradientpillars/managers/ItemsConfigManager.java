package ua.xlany.gradientpillars.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.utils.ConfigUtil;

import java.io.File;
import java.util.logging.Level;

public class ItemsConfigManager {

    private final GradientPillars plugin;
    private final File configFile;
    private FileConfiguration config;

    public ItemsConfigManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "items.yml");
        ensureFile();
        load();
    }

    private void ensureFile() {
        if (!configFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
    }

    private void load() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        updateConfig(); // Перевіряємо наявність нових ключів
    }

    private void updateConfig() {
        if (ConfigUtil.updateConfig(plugin, "items.yml", config, configFile)) {
            // Перезавантажуємо, якщо були зміни
            reload();
        }
    }

    public void reload() {
        load();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save items.yml", ex);
        }
    }
}
