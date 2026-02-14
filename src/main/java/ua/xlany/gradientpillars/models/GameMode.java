package ua.xlany.gradientpillars.models;

import org.bukkit.Material;

/**
 * Enum для різних режимів гри
 */
public enum GameMode {

    NORMAL("mode.normal", Material.GRASS_BLOCK, 20.0),
    RISING_LAVA("mode.rising-lava", Material.LAVA_BUCKET, 20.0),
    TWO_ROWS_HEARTS("mode.two-rows-hearts", Material.GOLDEN_APPLE, 40.0),
    ONE_HEART("mode.one-heart", Material.RED_DYE, 2.0),
    JUMP_BOOST("mode.jump-boost", Material.FEATHER, 20.0),
    NO_JUMP("mode.no-jump", Material.IRON_BOOTS, 20.0),
    DARKNESS("mode.darkness", Material.COAL, 20.0);

    private final String translationKey;
    private final Material icon;
    private final double health;

    GameMode(String translationKey, Material icon, double health) {
        this.translationKey = translationKey;
        this.icon = icon;
        this.health = health;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Material getIcon() {
        return icon;
    }

    public double getHealth() {
        return health;
    }
}
