package ua.xlany.gradientpillars.models;

public enum GameState {
    WAITING, // Очікування гравців
    COUNTDOWN, // Відлік до початку
    ACTIVE, // Гра активна
    ENDING, // Гра завершується
    RESTORING // Світ відновлюється після гри
}
