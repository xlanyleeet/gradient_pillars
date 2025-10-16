package ua.xlany.gradientpillars.managers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.xlany.gradientpillars.GradientPillars;

import java.util.*;

public class ItemManager {

    private final GradientPillars plugin;
    private final List<ItemStack> weapons;
    private final List<ItemStack> armor;
    private final List<ItemStack> food;
    private final List<ItemStack> blocks;
    private final List<ItemStack> potions;
    private final List<ItemStack> tools;
    private final Random random;

    public ItemManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.weapons = new ArrayList<>();
        this.armor = new ArrayList<>();
        this.food = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.potions = new ArrayList<>();
        this.tools = new ArrayList<>();
        this.random = new Random();

        initializeItems();
    }

    private void initializeItems() {
        // Зброя
        weapons.add(new ItemStack(Material.WOODEN_SWORD));
        weapons.add(new ItemStack(Material.STONE_SWORD));
        weapons.add(new ItemStack(Material.IRON_SWORD));
        weapons.add(new ItemStack(Material.DIAMOND_SWORD));
        weapons.add(new ItemStack(Material.BOW));
        weapons.add(new ItemStack(Material.CROSSBOW));
        weapons.add(new ItemStack(Material.TRIDENT));
        weapons.add(new ItemStack(Material.ARROW, 16));

        // Броня
        armor.add(new ItemStack(Material.LEATHER_HELMET));
        armor.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        armor.add(new ItemStack(Material.LEATHER_LEGGINGS));
        armor.add(new ItemStack(Material.LEATHER_BOOTS));
        armor.add(new ItemStack(Material.CHAINMAIL_HELMET));
        armor.add(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        armor.add(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        armor.add(new ItemStack(Material.CHAINMAIL_BOOTS));
        armor.add(new ItemStack(Material.IRON_HELMET));
        armor.add(new ItemStack(Material.IRON_CHESTPLATE));
        armor.add(new ItemStack(Material.IRON_LEGGINGS));
        armor.add(new ItemStack(Material.IRON_BOOTS));
        armor.add(new ItemStack(Material.DIAMOND_HELMET));
        armor.add(new ItemStack(Material.DIAMOND_CHESTPLATE));
        armor.add(new ItemStack(Material.DIAMOND_LEGGINGS));
        armor.add(new ItemStack(Material.DIAMOND_BOOTS));
        armor.add(new ItemStack(Material.SHIELD));

        // Їжа
        food.add(new ItemStack(Material.APPLE, 3));
        food.add(new ItemStack(Material.BREAD, 3));
        food.add(new ItemStack(Material.COOKED_BEEF, 5));
        food.add(new ItemStack(Material.COOKED_CHICKEN, 5));
        food.add(new ItemStack(Material.GOLDEN_APPLE));
        food.add(new ItemStack(Material.COOKED_PORKCHOP, 5));
        food.add(new ItemStack(Material.BAKED_POTATO, 5));
        food.add(new ItemStack(Material.COOKIE, 8));

        // Блоки
        blocks.add(new ItemStack(Material.COBBLESTONE, 32));
        blocks.add(new ItemStack(Material.STONE, 32));
        blocks.add(new ItemStack(Material.OAK_PLANKS, 32));
        blocks.add(new ItemStack(Material.DIRT, 32));
        blocks.add(new ItemStack(Material.SAND, 16));
        blocks.add(new ItemStack(Material.GRAVEL, 16));
        blocks.add(new ItemStack(Material.GLASS, 16));
        blocks.add(new ItemStack(Material.OBSIDIAN, 8));
        blocks.add(new ItemStack(Material.TNT, 4));
        blocks.add(new ItemStack(Material.LADDER, 16));
        blocks.add(new ItemStack(Material.COBWEB, 4));

        // Зілля
        potions.add(createPotion(PotionEffectType.SPEED, 600, 1));
        potions.add(createPotion(PotionEffectType.STRENGTH, 400, 1));
        potions.add(createPotion(PotionEffectType.INSTANT_HEALTH, 1, 1));
        potions.add(createPotion(PotionEffectType.JUMP_BOOST, 600, 2));
        potions.add(createPotion(PotionEffectType.REGENERATION, 200, 1));
        potions.add(createPotion(PotionEffectType.FIRE_RESISTANCE, 600, 0));
        potions.add(createPotion(PotionEffectType.INVISIBILITY, 400, 0));

        // Інструменти
        tools.add(new ItemStack(Material.WOODEN_PICKAXE));
        tools.add(new ItemStack(Material.STONE_PICKAXE));
        tools.add(new ItemStack(Material.IRON_PICKAXE));
        tools.add(new ItemStack(Material.WOODEN_AXE));
        tools.add(new ItemStack(Material.STONE_AXE));
        tools.add(new ItemStack(Material.IRON_AXE));
        tools.add(new ItemStack(Material.FISHING_ROD));
        tools.add(new ItemStack(Material.FLINT_AND_STEEL));
        tools.add(new ItemStack(Material.SHEARS));
    }

    private ItemStack createPotion(PotionEffectType type, int duration, int amplifier) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        potion.setItemMeta(meta);
        return potion;
    }

    public ItemStack getRandomItem() {
        // Створюємо weighted список категорій
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

        // Вибираємо категорію з урахуванням ваг
        String category = weightedCategories.get(random.nextInt(weightedCategories.size()));

        // Вибираємо предмет з обраної категорії
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

        // Рандомний шанс на зачарування
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
