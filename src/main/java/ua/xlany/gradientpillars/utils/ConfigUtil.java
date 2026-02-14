package ua.xlany.gradientpillars.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigUtil {

    /**
     * Updates the given configuration with missing keys from the internal resource
     * file.
     *
     * @param plugin       The plugin instance
     * @param resourceName The name of the resource file in the JAR (e.g.,
     *                     "config.yml")
     * @param targetConfig The loaded FileConfiguration to update
     * @param fileOnDisk   The file on disk to save changes to
     * @return true if changes were made and saved, false otherwise
     */
    public static boolean updateConfig(JavaPlugin plugin, String resourceName, FileConfiguration targetConfig,
            File fileOnDisk) {
        InputStream defStream = plugin.getResource(resourceName);
        if (defStream == null) {
            return false;
        }

        try (InputStreamReader reader = new InputStreamReader(defStream, StandardCharsets.UTF_8)) {
            YamlConfiguration internalConfig = YamlConfiguration.loadConfiguration(reader);
            boolean changesMade = false;

            for (String key : internalConfig.getKeys(true)) {
                if (internalConfig.isConfigurationSection(key)) {
                    continue;
                }

                // Use isSet instead of contains to ignore default values and check actual file
                // content
                if (!targetConfig.isSet(key)) {
                    targetConfig.set(key, internalConfig.get(key));
                    changesMade = true;
                    plugin.getLogger().info("[" + resourceName + "] Adding missing key: " + key);
                }
            }

            if (changesMade) {
                try {
                    targetConfig.save(fileOnDisk);
                    plugin.getLogger().info(resourceName + " updated with new values based on internal resource.");
                    return true;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Could not save updated configuration to " + fileOnDisk.getName(), e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update configuration " + resourceName, e);
        }
        return false;
    }
}
