package ua.xlany.gradientpillars.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.xlany.gradientpillars.GradientPillars;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageManager {

    private final GradientPillars plugin;
    private FileConfiguration messages;
    private String currentLanguage;

    public MessageManager(GradientPillars plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currentLanguage = plugin.getConfigManager().getLanguage();
        loadMessages();
    }

    private void loadMessages() {
        String fileName = "messages_" + currentLanguage + ".yml";
        File messagesFile = new File(plugin.getDataFolder(), fileName);

        if (!messagesFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Завантажити дефолтні повідомлення та злити їх з існуючими
        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messages.setDefaults(defConfig);

            // Автоматичне злиття: додати нові ключі з JAR файлу, якщо їх немає в файлі
            if (mergeMessages(messagesFile, defConfig)) {
                plugin.getLogger().info("Додано нові ключі перекладів до " + fileName);
            }
        }
    }

    /**
     * Злиття нових ключів з JAR файлу в існуючий файл перекладів
     * 
     * @param messagesFile  Файл перекладів на диску
     * @param defaultConfig Конфігурація з JAR файлу
     * @return true якщо були додані нові ключі
     */
    private boolean mergeMessages(File messagesFile, YamlConfiguration defaultConfig) {
        boolean hasChanges = false;

        try {
            FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(messagesFile);

            // Перевірити всі ключі з JAR файлу
            for (String key : defaultConfig.getKeys(true)) {
                // Пропустити секції (не листові вузли)
                if (defaultConfig.isConfigurationSection(key)) {
                    continue;
                }

                // Якщо ключ відсутній у файлі - додати його
                if (!existingConfig.contains(key)) {
                    existingConfig.set(key, defaultConfig.get(key));
                    hasChanges = true;

                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Додано новий ключ: " + key);
                    }
                }
            }

            // Зберегти файл, якщо були зміни
            if (hasChanges) {
                existingConfig.save(messagesFile);
                // Перезавантажити конфігурацію
                messages = YamlConfiguration.loadConfiguration(messagesFile);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Помилка при злитті перекладів: " + e.getMessage());
            if (plugin.getConfigManager().isDebugEnabled()) {
                e.printStackTrace();
            }
        }

        return hasChanges;
    }

    public String getMessage(String path) {
        return messages.getString(path, "&cMessage not found: " + path);
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }

    public Component getComponent(String path) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage(path));
    }

    public Component getComponent(String path, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage(path, replacements));
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public Component getPrefixedComponent(String path) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getPrefix() + getMessage(path));
    }

    public Component getPrefixedComponent(String path, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getPrefix() + getMessage(path, replacements));
    }
}
