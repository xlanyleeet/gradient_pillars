package ua.xlany.gradientpillars;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ua.xlany.gradientpillars.chat.ChatManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import ua.xlany.gradientpillars.commands.GPCommand;

import ua.xlany.gradientpillars.integration.GradientPillarsPlaceholders;
import ua.xlany.gradientpillars.listeners.*;
import ua.xlany.gradientpillars.managers.*;
import ua.xlany.gradientpillars.utils.LibraryUtil;
import xyz.xenondevs.invui.InvUI;

public class GradientPillars extends JavaPlugin {

    private static GradientPillars instance;

    // Managers
    private LibraryUtil libraryUtil;
    private ConfigManager configManager;
    private ItemsConfigManager itemsConfigManager;
    private MessageManager messageManager;
    private WorldManager worldManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private ItemManager itemManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        instance = this;
        InvUI.getInstance().setPlugin(this);

        // Завантаження бібліотек
        libraryUtil = new LibraryUtil(this);
        libraryUtil.loadLibraries();

        getLogger().info("Ініціалізація Gradient Pillars...");

        // Ініціалізація менеджерів
        configManager = new ConfigManager(this);
        itemsConfigManager = new ItemsConfigManager(this);
        messageManager = new MessageManager(this);

        // Ініціалізація бази даних
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Stats will not work.");
        }
        statsManager = new StatsManager(this, databaseManager);

        worldManager = new WorldManager(this);
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this);
        itemManager = new ItemManager(this);
        chatManager = new ChatManager(this);

        // Реєстрація команд (New Command API)
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new GPCommand(this).register(event.registrar());
        });

        // Реєстрація слухачів подій
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // PlaceholderAPI інтеграція
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GradientPillarsPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        getLogger().info("Gradient Pillars успішно увімкнено!");
    }

    @Override
    public void onDisable() {
        // Завершення всіх активних ігор
        if (gameManager != null) {
            gameManager.shutdown();
        }

        // Закриття підключення до БД
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        getLogger().info("Gradient Pillars вимкнено!");
    }

    public static GradientPillars getInstance() {
        return instance;
    }

    public LibraryUtil getLibraryUtil() {
        return libraryUtil;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemsConfigManager getItemsConfigManager() {
        return itemsConfigManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void reload() {
        configManager.reload();
        itemsConfigManager.reload();
        messageManager.reload();
        arenaManager.reload();
        itemManager.reload();
    }
}
