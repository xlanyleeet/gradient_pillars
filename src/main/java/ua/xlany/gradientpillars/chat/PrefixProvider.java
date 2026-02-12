package ua.xlany.gradientpillars.chat;

import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ua.xlany.gradientpillars.GradientPillars;

public class PrefixProvider {

    private final GradientPillars plugin;
    private Chat vaultChat;

    public PrefixProvider(GradientPillars plugin) {
        this.plugin = plugin;
        setupVaultChat();
    }

    /**
     * Налаштовує Vault Chat для отримання префіксів
     */
    private void setupVaultChat() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Player prefixes will not be displayed.");
            return;
        }

        RegisteredServiceProvider<Chat> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Chat.class);

        if (rsp != null) {
            vaultChat = rsp.getProvider();
            plugin.getLogger().info("Vault Chat hooked successfully!");
        } else {
            plugin.getLogger().warning("Vault Chat not found! Player prefixes will not be displayed.");
        }
    }

    /**
     * Отримує префікс гравця через Vault
     */
    public Component getPlayerPrefix(Player player) {
        if (vaultChat == null) {
            return Component.empty();
        }

        String prefix = vaultChat.getPlayerPrefix(player);

        if (prefix == null || prefix.isEmpty()) {
            return Component.empty();
        }

        // Use MessageManager to deserialize legacy prefix from Vault
        return plugin.getMessageManager().deserializeLegacy(prefix);
    }

    /**
     * Перевіряє чи Vault Chat доступний
     */
    public boolean isVaultChatAvailable() {
        return vaultChat != null;
    }
}
