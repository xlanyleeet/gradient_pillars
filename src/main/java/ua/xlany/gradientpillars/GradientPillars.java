package ua.xlany.gradientpillars;

import org.bukkit.plugin.java.JavaPlugin;
import ua.xlany.gradientpillars.commands.GPCommand;
import ua.xlany.gradientpillars.listeners.GameListener;
import ua.xlany.gradientpillars.listeners.LobbyListener;
import ua.xlany.gradientpillars.listeners.PlayerListener;
import ua.xlany.gradientpillars.listeners.WorldListener;
import ua.xlany.gradientpillars.managers.*;

public class GradientPillars extends JavaPlugin {

    private static GradientPillars instance;

    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private WorldManager worldManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Ініціалізація Gradient Pillars...");

        // Ініціалізація менеджерів
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        worldManager = new WorldManager(this);
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this);
        itemManager = new ItemManager(this);

        // Реєстрація команд
        GPCommand gpCommand = new GPCommand(this);
        getCommand("gp").setExecutor(gpCommand);
        getCommand("gp").setTabCompleter(gpCommand);

        // Реєстрація слухачів подій
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        getLogger().info("Gradient Pillars успішно увімкнено!");
    }

    @Override
    public void onDisable() {
        // Завершення всіх активних ігор
        if (gameManager != null) {
            gameManager.shutdown();
        }

        getLogger().info("Gradient Pillars вимкнено!");
    }

    public static GradientPillars getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
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

    public void reload() {
        configManager.reload();
        messageManager.reload();
        arenaManager.reload();
    }
}
