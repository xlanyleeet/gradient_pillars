package ua.xlany.gradientpillars.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.xlany.gradientpillars.GradientPillars;

import ua.xlany.gradientpillars.utils.ConfigUtil;

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
        if (ConfigUtil.updateConfig(plugin, fileName, messages, messagesFile)) {
            // Перезавантажуємо, якщо були зміни
            messages = YamlConfiguration.loadConfiguration(messagesFile);

            // Якщо ще раз потрібні defaults, можна встановити їх
            InputStream defStream = plugin.getResource(fileName);
            if (defStream != null) {
                try (InputStreamReader reader = new InputStreamReader(defStream, StandardCharsets.UTF_8)) {
                    messages.setDefaults(YamlConfiguration.loadConfiguration(reader));
                } catch (Exception ignored) {
                }
            }
        } else {
            // Якщо оновлення не було (ConfigUtil повернув false), просто встановлюємо
            // дефолтні значення для пам'яті
            InputStream defStream = plugin.getResource(fileName);
            if (defStream != null) {
                try (InputStreamReader reader = new InputStreamReader(defStream, StandardCharsets.UTF_8)) {
                    messages.setDefaults(YamlConfiguration.loadConfiguration(reader));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to set default messages: " + e.getMessage());
                }
            }
        }
    }

    public String getMessage(String path) {
        return messages.getString(path, "<red>Message not found: " + path);
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
        return MiniMessage.miniMessage().deserialize(getMessage(path));
    }

    public Component getComponent(String path, String... replacements) {
        return MiniMessage.miniMessage().deserialize(getMessage(path, replacements));
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public Component getPrefixedComponent(String path) {
        // Concatenate prefix string + message string, thendeserialize
        // This assumes both are in MiniMessage format
        return MiniMessage.miniMessage().deserialize(getPrefix() + getMessage(path));
    }

    public Component getPrefixedComponent(String path, String... replacements) {
        return MiniMessage.miniMessage()
                .deserialize(getPrefix() + getMessage(path, replacements));
    }
}
