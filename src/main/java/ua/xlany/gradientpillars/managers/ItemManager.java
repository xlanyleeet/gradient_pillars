package ua.xlany.gradientpillars.managers;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class ItemManager {

    private final GradientPillars plugin;
    private final ItemsConfigManager itemsConfigManager;
    private final List<ItemStack> weapons = new ArrayList<>();
    private final List<ItemStack> armor = new ArrayList<>();
    private final List<ItemStack> food = new ArrayList<>();
    private final List<ItemStack> blocks = new ArrayList<>();
    private final List<ItemStack> potions = new ArrayList<>();
    private final List<ItemStack> tools = new ArrayList<>();
    private final Random random = new Random();

    private static final Set<Material> GLOBAL_EXCLUDE = EnumSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.BEDROCK,
            Material.BARRIER,
            Material.LIGHT,
            Material.SPAWNER,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK,
            Material.END_PORTAL,
            Material.END_PORTAL_FRAME,
            Material.NETHER_PORTAL,
            Material.END_GATEWAY,
            Material.END_CRYSTAL,
            Material.BUNDLE);

    public ItemManager(GradientPillars plugin) {
        this.plugin = plugin;
        ItemsConfigManager manager = plugin.getItemsConfigManager();
        if (manager == null) {
            throw new IllegalStateException("ItemsConfigManager must be initialized before ItemManager");
        }
        this.itemsConfigManager = manager;
        initializeItems();
    }

    public void reload() {
        weapons.clear();
        armor.clear();
        food.clear();
        blocks.clear();
        potions.clear();
        tools.clear();
        initializeItems();
    }

    private void initializeItems() {
        FileConfiguration config = itemsConfigManager.getConfig();

        loadCategory(config, "weapons", weapons, this::isWeapon, 1);
        loadCategory(config, "armor", armor, this::isArmor, 1);
        loadCategory(config, "food", food, this::isFood, 1);
        loadCategory(config, "blocks", blocks, this::isBlockItem, 1);
        loadCategory(config, "tools", tools, this::isTool, 1);
        loadPotions(config);
    }

    private void initializeDefaultPotions() {
        potions.clear();
        potions.add(createPotion(PotionEffectType.SPEED, 600, 1));
        potions.add(createPotion(PotionEffectType.STRENGTH, 400, 1));
        potions.add(createPotion(PotionEffectType.INSTANT_HEALTH, 1, 1));
        potions.add(createPotion(PotionEffectType.JUMP_BOOST, 600, 2));
        potions.add(createPotion(PotionEffectType.REGENERATION, 200, 1));
        potions.add(createPotion(PotionEffectType.FIRE_RESISTANCE, 600, 0));
        potions.add(createPotion(PotionEffectType.INVISIBILITY, 400, 0));
    }

    private void loadCategory(FileConfiguration config, String key, List<ItemStack> target, Predicate<Material> filter,
            int defaultAmount) {
        target.clear();

        ConfigurationSection section = config.getConfigurationSection("categories." + key);
        if (section == null || section.getBoolean("dynamic", true)) {
            generateCategoryItems(target, filter, defaultAmount);
            return;
        }

        List<?> entries = section.getList("items");
        if (entries != null) {
            for (Object entryObj : entries) {
                Material material;
                int amount = defaultAmount;
                String debugName;

                if (entryObj instanceof String materialName) {
                    material = parseMaterial(materialName);
                    debugName = materialName;
                } else if (entryObj instanceof Map<?, ?> entry) {
                    Object materialValue = entry.get("material");
                    material = parseMaterial(materialValue);
                    amount = getInt(entry.get("amount"), defaultAmount);
                    debugName = String.valueOf(materialValue);
                } else {
                    continue;
                }

                if (material == null) {
                    logConfigIssue(key, "Unknown material: " + debugName);
                    continue;
                }
                if (GLOBAL_EXCLUDE.contains(material) || !material.isItem()) {
                    logConfigIssue(key, "Unsupported material: " + material.name());
                    continue;
                }
                if (!filter.test(material)) {
                    logConfigIssue(key, "Material does not match category: " + material.name());
                    continue;
                }

                ItemStack stack = new ItemStack(material);
                amount = Math.max(1, Math.min(amount, stack.getMaxStackSize()));
                stack.setAmount(amount);
                target.add(stack);
            }
        }

        if (target.isEmpty()) {
            generateCategoryItems(target, filter, defaultAmount);
        }
    }

    private void loadPotions(FileConfiguration config) {
        potions.clear();

        ConfigurationSection section = config.getConfigurationSection("categories.potions");
        if (section == null || section.getBoolean("dynamic", false)) {
            initializeDefaultPotions();
            return;
        }

        List<?> entries = section.getList("items");
        if (entries == null || entries.isEmpty()) {
            initializeDefaultPotions();
            return;
        }

        for (Object entryObj : entries) {
            if (!(entryObj instanceof Map<?, ?> entry)) {
                continue;
            }

            Material material = parsePotionMaterial(entry.get("material"));
            if (material == null) {
                logConfigIssue("potions", "Unknown material: " + entry.get("material"));
                continue;
            }
            if (!material.isItem()) {
                logConfigIssue("potions", "Unsupported material: " + material.name());
                continue;
            }

            ItemStack potion = new ItemStack(material);
            int amount = getInt(entry.get("amount"), 1);
            amount = Math.max(1, Math.min(amount, potion.getMaxStackSize()));
            potion.setAmount(amount);

            if (!(potion.getItemMeta() instanceof PotionMeta meta)) {
                logConfigIssue("potions", "Material requires potion meta: " + material.name());
                continue;
            }

            Color color = parseColor(entry.get("color"));
            if (color != null) {
                meta.setColor(color);
            }

            Object effectsObj = entry.get("effects");
            if (effectsObj instanceof List<?>) {
                for (Object effectObj : (List<?>) effectsObj) {
                    if (!(effectObj instanceof Map<?, ?> effectMap)) {
                        continue;
                    }

                    PotionEffectType type = parseEffectType(effectMap.get("type"));
                    if (type == null) {
                        logConfigIssue("potions", "Unknown effect type: " + effectMap.get("type"));
                        continue;
                    }

                    int duration = getInt(effectMap.get("duration"), 200);
                    int amplifier = getInt(effectMap.get("amplifier"), 0);
                    boolean ambient = getBoolean(effectMap.get("ambient"), false);
                    boolean particles = getBoolean(effectMap.get("particles"), true);
                    boolean icon = getBoolean(effectMap.get("icon"), true);

                    meta.addCustomEffect(new PotionEffect(type, duration, amplifier, ambient, particles, icon), true);
                }
            }

            potion.setItemMeta(meta);
            potions.add(potion);
        }

        if (potions.isEmpty()) {
            initializeDefaultPotions();
        }
    }

    private Material parseMaterial(Object value) {
        if (value == null) {
            return null;
        }

        String name = value.toString().trim();
        if (name.isEmpty()) {
            return null;
        }

        Material material = Material.matchMaterial(name);
        if (material == null) {
            material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        }
        return material;
    }

    private Material parsePotionMaterial(Object value) {
        if (value == null) {
            return Material.POTION;
        }
        return parseMaterial(value);
    }

    @SuppressWarnings("deprecation")
    private PotionEffectType parseEffectType(Object value) {
        if (value == null) {
            return null;
        }

        String name = value.toString().trim();
        if (name.isEmpty()) {
            return null;
        }

        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        if (type == null) {
            // Fallback for legacy names or unexpected casing.
            type = PotionEffectType.getByName(name.toUpperCase(Locale.ROOT));
        }
        return type;
    }

    private Color parseColor(Object value) {
        if (!(value instanceof String str)) {
            return null;
        }

        String input = str.trim();
        if (input.isEmpty()) {
            return null;
        }

        if (input.startsWith("#")) {
            input = input.substring(1);
        }

        if (input.length() != 6) {
            return null;
        }

        try {
            int rgb = Integer.parseInt(input, 16);
            return Color.fromRGB(rgb);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int getInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        return fallback;
    }

    private boolean getBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof String str) {
            return Boolean.parseBoolean(str.trim());
        }

        return fallback;
    }

    private void logConfigIssue(String category, String message) {
        plugin.getLogger().log(Level.WARNING, "[items.yml] {0}: {1}", new Object[] { category, message });
    }

    private void generateCategoryItems(List<ItemStack> target, Predicate<Material> filter, int defaultAmount) {
        Set<Material> added = EnumSet.noneOf(Material.class);

        for (Material material : Material.values()) {
            if (material.name().startsWith("LEGACY_")) {
                continue;
            }
            if (GLOBAL_EXCLUDE.contains(material) || material.isAir() || !material.isItem()) {
                continue;
            }

            if (!filter.test(material)) {
                continue;
            }

            if (!added.add(material)) {
                continue;
            }

            ItemStack stack = new ItemStack(material);
            int maxStack = stack.getMaxStackSize();
            int amount = Math.min(Math.max(1, defaultAmount), maxStack);

            if (material == Material.ARROW || material == Material.SPECTRAL_ARROW
                    || material == Material.TIPPED_ARROW) {
                amount = Math.min(4, maxStack);
            } else if (material.isEdible()) {
                amount = Math.min(4, maxStack);
            } else if (material.isBlock()) {
                amount = 1;
            }

            stack.setAmount(amount);
            target.add(stack);
        }

        if (target.isEmpty()) {
            target.add(new ItemStack(Material.STICK));
        }
    }

    private boolean isWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_TRIDENT")
                || material == Material.BOW
                || material == Material.CROSSBOW
                || material == Material.MACE
                || material == Material.ARROW
                || material == Material.SPECTRAL_ARROW
                || material == Material.TIPPED_ARROW
                || material == Material.WIND_CHARGE;
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || material == Material.SHIELD
                || material == Material.TURTLE_HELMET
                || material == Material.ELYTRA;
    }

    private boolean isFood(Material material) {
        return material == Material.GOLDEN_APPLE;
    }

    private boolean isBlockItem(Material material) {
        if (!material.isBlock()) {
            return false;
        }
        String name = material.name();
        return !name.contains("PORTAL") && !name.contains("INFESTED") && !name.contains("VOID");
    }

    private boolean isTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || material == Material.SHEARS
                || material == Material.FISHING_ROD
                || material == Material.FLINT_AND_STEEL
                || material == Material.BRUSH;
    }

    private ItemStack createPotion(PotionEffectType type, int duration, int amplifier) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    public ItemStack getRandomItem() {
        List<String> weightedCategories = new ArrayList<>();

        if (plugin.getConfigManager().isWeaponsEnabled()) {
            int weight = plugin.getConfigManager().getWeaponsWeight();
            for (int i = 0; i < weight; i++) {
                weightedCategories.add("weapons");
            }
        }
        if (plugin.getConfigManager().isArmorEnabled()) {
            int weight = plugin.getConfigManager().getArmorWeight();
            for (int i = 0; i < weight; i++) {
                weightedCategories.add("armor");
            }
        }
        if (plugin.getConfigManager().isFoodEnabled()) {
            int weight = plugin.getConfigManager().getFoodWeight();
            for (int i = 0; i < weight; i++) {
                weightedCategories.add("food");
            }
        }
        if (plugin.getConfigManager().isBlocksEnabled()) {
            int weight = plugin.getConfigManager().getBlocksWeight();
            for (int i = 0; i < weight; i++) {
                weightedCategories.add("blocks");
            }
        }
        if (plugin.getConfigManager().isPotionsEnabled()) {
            int weight = plugin.getConfigManager().getPotionsWeight();
            for (int i = 0; i < weight; i++) {
                weightedCategories.add("potions");
            }
        }
        if (plugin.getConfigManager().isToolsEnabled()) {
            int weight = plugin.getConfigManager().getToolsWeight();
            for (int i = 0; i < weight; i++) {
                weightedCategories.add("tools");
            }
        }

        if (weightedCategories.isEmpty()) {
            return new ItemStack(Material.STICK);
        }

        String category = weightedCategories.get(random.nextInt(weightedCategories.size()));

        List<ItemStack> categoryItems = switch (category) {
            case "weapons" -> weapons;
            case "armor" -> armor;
            case "food" -> food;
            case "blocks" -> blocks;
            case "potions" -> potions;
            case "tools" -> tools;
            default -> new ArrayList<>();
        };

        if (categoryItems.isEmpty()) {
            return new ItemStack(Material.STICK);
        }

        ItemStack item = categoryItems.get(random.nextInt(categoryItems.size())).clone();

        if (random.nextDouble() < 0.15 && canEnchant(item)) {
            enchantItem(item);
        }

        return item;
    }

    private boolean canEnchant(ItemStack item) {
        Material type = item.getType();
        return type.name().contains("SWORD") ||
                type.name().contains("AXE") ||
                type.name().contains("PICKAXE") ||
                type.name().contains("BOW") ||
                type.name().contains("HELMET") ||
                type.name().contains("CHESTPLATE") ||
                type.name().contains("LEGGINGS") ||
                type.name().contains("BOOTS");
    }

    private void enchantItem(ItemStack item) {
        List<Enchantment> possibleEnchants = new ArrayList<>();

        @SuppressWarnings("deprecation")
        Iterable<Enchantment> enchantments = org.bukkit.Registry.ENCHANTMENT;
        for (Enchantment enchantment : enchantments) {
            if (enchantment.canEnchantItem(item)) {
                possibleEnchants.add(enchantment);
            }
        }

        if (!possibleEnchants.isEmpty()) {
            Enchantment enchantment = possibleEnchants.get(random.nextInt(possibleEnchants.size()));
            int level = random.nextInt(enchantment.getMaxLevel()) + 1;
            item.addUnsafeEnchantment(enchantment, level);
        }
    }
}
