package ua.xlany.gradientpillars.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

public class ChatFormatter {

    private final PrefixProvider prefixProvider;

    public ChatFormatter(GradientPillars plugin) {
        this.prefixProvider = new PrefixProvider(plugin);
    }

    /**
     * Форматує повідомлення для арени
     * Формат: [Arena] [Prefix] PlayerName: message
     */
    public Component formatArenaMessage(Player sender, String message, Game game) {
        Component arenaTag = createArenaTag(game);
        Component prefix = prefixProvider.getPlayerPrefix(sender);
        Component playerName = Component.text(sender.getName())
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false);
        Component messageText = Component.text(": " + message)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);

        return Component.empty()
                .append(arenaTag)
                .append(Component.space())
                .append(prefix)
                .append(Component.space())
                .append(playerName)
                .append(messageText);
    }

    /**
     * Форматує глобальне повідомлення (для гравців поза ареною)
     * Формат: [Prefix] PlayerName: message
     */
    public Component formatGlobalMessage(Player sender, String message) {
        Component prefix = prefixProvider.getPlayerPrefix(sender);
        Component playerName = Component.text(sender.getName())
                .color(NamedTextColor.WHITE);
        Component messageText = Component.text(": " + message)
                .color(NamedTextColor.GRAY);

        return Component.empty()
                .append(prefix)
                .append(Component.space())
                .append(playerName)
                .append(messageText);
    }

    /**
     * Створює тег арени з кольором залежно від стану гри
     */
    private Component createArenaTag(Game game) {
        String arenaName = game.getArenaName();
        GameState state = game.getState();

        NamedTextColor color = switch (state) {
            case WAITING -> NamedTextColor.GREEN;
            case COUNTDOWN -> NamedTextColor.YELLOW;
            case ACTIVE -> NamedTextColor.RED;
            case ENDING -> NamedTextColor.GOLD;
            default -> NamedTextColor.GRAY;
        };

        return Component.text("[" + arenaName + "]")
                .color(color)
                .decoration(TextDecoration.BOLD, true);
    }
}
