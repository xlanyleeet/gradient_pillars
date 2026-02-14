package ua.xlany.gradientpillars.game.modes;

import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.GameMode;

import java.util.EnumMap;
import java.util.Map;

public class GameModeRegistry {

    private final Map<GameMode, GameModeHandler> handlers;

    public GameModeRegistry(GradientPillars plugin) {
        this.handlers = new EnumMap<>(GameMode.class);
        registerHandlers(plugin);
    }

    private void registerHandlers(GradientPillars plugin) {
        handlers.put(GameMode.RISING_LAVA, new RisingLavaMode(plugin));
        handlers.put(GameMode.JUMP_BOOST, new JumpBoostMode());
        handlers.put(GameMode.NO_JUMP, new NoJumpMode());
        handlers.put(GameMode.DARKNESS, new DarknessMode());
        handlers.put(GameMode.TWO_ROWS_HEARTS, new TwoRowsHeartsMode());
        handlers.put(GameMode.ONE_HEART, new OneHeartMode());
        handlers.put(GameMode.NORMAL, new NormalMode());
    }

    public GameModeHandler getHandler(GameMode mode) {
        return handlers.get(mode);
    }
}
