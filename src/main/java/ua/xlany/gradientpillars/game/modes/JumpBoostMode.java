package ua.xlany.gradientpillars.game.modes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ua.xlany.gradientpillars.models.Game;

import java.util.UUID;

public class JumpBoostMode implements GameModeHandler {

    @Override
    public void apply(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false));
            }
        }
    }
}
