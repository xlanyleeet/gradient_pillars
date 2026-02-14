package ua.xlany.gradientpillars.game.modes;

import ua.xlany.gradientpillars.models.Game;

public interface GameModeHandler {
    void apply(Game game);

    // Optional: method to cleanup if needed when game ends
    default void cleanup(Game game) {
    }
}
