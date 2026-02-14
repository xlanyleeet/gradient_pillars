package ua.xlany.gradientpillars.game.modes;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.models.Game;

import java.util.UUID;

public class NoJumpMode implements GameModeHandler {

    @Override
    public void apply(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Використовуємо атрибут Jump Strength замість ефекту - набагато оптимальніше
                player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.0);
            }
        }
    }

    @Override
    public void cleanup(Game game) {
        // Reset jump strength
        for (UUID playerId : game.getPlayers()) { // Get all players even dead ones to reset
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Default jump strength is usually around 0.42 but let's check or assume
                // default
                // Actually default base value for horses is 0.7, for players it is 0.42
                // But safest way is to just remove modifier if we added one,
                // but here we setBaseValue directly.
                // Correct logic would be to restore default.
                // The default for generic.jump_strength is 0.41999998688697815
                player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.42);
            }
        }
    }
}
