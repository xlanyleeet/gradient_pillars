package ua.xlany.gradientpillars.game.modes;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameMode;

import java.util.UUID;

public class TwoRowsHeartsMode implements GameModeHandler {

    @Override
    public void apply(Game game) {
        double health = GameMode.TWO_ROWS_HEARTS.getHealth();
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
                player.setHealth(health);
            }
        }
    }
}
