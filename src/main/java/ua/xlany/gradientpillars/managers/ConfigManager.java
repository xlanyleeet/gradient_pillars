package ua.xlany.gradientpillars.managers;

import org.bukkit.configuration.file.FileConfiguration;
import ua.xlany.gradientpillars.GradientPillars;

public class ConfigManager {

    private final GradientPillars plugin;
    private FileConfiguration config;

    public ConfigManager(GradientPillars plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getLanguage() {
        return config.getString("language", "uk");
    }

    // Hub settings
    public boolean isHubEnabled() {
        return config.getBoolean("hub.enabled", false);
    }

    public String getHubWorld() {
        return config.getString("hub.world", "world");
    }

    public double getHubX() {
        return config.getDouble("hub.x", 0.0);
    }

    public double getHubY() {
        return config.getDouble("hub.y", 64.0);
    }

    public double getHubZ() {
        return config.getDouble("hub.z", 0.0);
    }

    public float getHubYaw() {
        return (float) config.getDouble("hub.yaw", 0.0);
    }

    public float getHubPitch() {
        return (float) config.getDouble("hub.pitch", 0.0);
    }

    public void setHub(String world, double x, double y, double z, float yaw, float pitch) {
        config.set("hub.world", world);
        config.set("hub.x", x);
        config.set("hub.y", y);
        config.set("hub.z", z);
        config.set("hub.yaw", yaw);
        config.set("hub.pitch", pitch);
        config.set("hub.enabled", true);
        plugin.saveConfig();
    }

    // Game settings
    public int getCountdownTime() {
        return config.getInt("game.countdown-time", 10);
    }

    public int getMaxGameDuration() {
        return config.getInt("game.max-game-duration", 600);
    }

    public int getItemInterval() {
        return config.getInt("game.item-interval", 5);
    }

    public boolean isAutoBackup() {
        return config.getBoolean("backup.auto-backup", true);
    }

    public boolean isAutoRestore() {
        return config.getBoolean("backup.auto-restore", true);
    }

    public String getBackupFolder() {
        return config.getString("backup.backup-folder", "backups");
    }

    public boolean isUseBossBar() {
        return config.getBoolean("ui.use-bossbar", true);
    }

    public boolean isUseExpBar() {
        return config.getBoolean("ui.use-exp-bar", true);
    }

    public boolean isUseActionBar() {
        return config.getBoolean("ui.use-actionbar", true);
    }

    public boolean isWeaponsEnabled() {
        return config.getBoolean("items.enable-weapons", true);
    }

    public boolean isArmorEnabled() {
        return config.getBoolean("items.enable-armor", true);
    }

    public boolean isFoodEnabled() {
        return config.getBoolean("items.enable-food", true);
    }

    public boolean isBlocksEnabled() {
        return config.getBoolean("items.enable-blocks", true);
    }

    public boolean isPotionsEnabled() {
        return config.getBoolean("items.enable-potions", true);
    }

    public boolean isToolsEnabled() {
        return config.getBoolean("items.enable-tools", true);
    }

    // Item weights
    public int getWeaponsWeight() {
        return config.getInt("items.weights.weapons", 10);
    }

    public int getArmorWeight() {
        return config.getInt("items.weights.armor", 10);
    }

    public int getFoodWeight() {
        return config.getInt("items.weights.food", 8);
    }

    public int getBlocksWeight() {
        return config.getInt("items.weights.blocks", 25);
    }

    public int getPotionsWeight() {
        return config.getInt("items.weights.potions", 5);
    }

    public int getToolsWeight() {
        return config.getInt("items.weights.tools", 7);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
