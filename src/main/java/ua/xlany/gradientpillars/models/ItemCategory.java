package ua.xlany.gradientpillars.models;

public enum ItemCategory {
    WEAPONS("weapons"),
    ARMOR("armor"),
    FOOD("food"),
    BLOCKS("blocks"),
    POTIONS("potions"),
    TOOLS("tools");

    private final String key;

    ItemCategory(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
