package ua.xlany.gradientpillars.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Game;

import java.util.UUID;

public class ChatManager {

    private final GradientPillars plugin;
    private final ChatFormatter formatter;

    public ChatManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.formatter = new ChatFormatter(plugin);
    }

    /**
     * Обробляє повідомлення гравця в чаті арени
     */
    public void handleArenaChat(Player sender, String message) {
        Game game = plugin.getGameManager().getPlayerGame(sender.getUniqueId());
        
        if (game == null) {
            // Якщо гравець не в грі, відправити глобальний чат
            sendGlobalChat(sender, message);
            return;
        }

        // Форматування повідомлення
        Component formattedMessage = formatter.formatArenaMessage(sender, message, game);

        // Відправити повідомлення всім гравцям арени
        sendToArena(game, formattedMessage);
    }

    /**
     * Відправляє повідомлення всім гравцям на арені
     */
    private void sendToArena(Game game, Component message) {
        for (UUID playerId : game.getPlayers()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Відправляє глобальний чат (для гравців не в грі)
     */
    private void sendGlobalChat(Player sender, String message) {
        Component formattedMessage = formatter.formatGlobalMessage(sender, message);
        plugin.getServer().broadcast(formattedMessage);
    }

    public ChatFormatter getFormatter() {
        return formatter;
    }
}
