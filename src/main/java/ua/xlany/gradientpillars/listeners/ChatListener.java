package ua.xlany.gradientpillars.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import ua.xlany.gradientpillars.GradientPillars;

public class ChatListener implements Listener {

    private final GradientPillars plugin;

    public ChatListener(GradientPillars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Отримуємо текст повідомлення
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Скасовуємо стандартний чат
        event.setCancelled(true);

        // Обробляємо через ChatManager (використовуємо синхронний шедулер для безпеки)
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getChatManager().handleArenaChat(player, message);
        });
    }
}
