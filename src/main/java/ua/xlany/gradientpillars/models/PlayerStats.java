package ua.xlany.gradientpillars.models;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private String playerName;
    private int wins;
    private int losses;

    public PlayerStats(UUID uuid, String playerName, int wins, int losses) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.wins = wins;
        this.losses = losses;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void addWin() {
        this.wins++;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void addLoss() {
        this.losses++;
    }

    public int getTotalGames() {
        return wins + losses;
    }

    public double getWinRate() {
        int total = getTotalGames();
        if (total == 0) {
            return 0.0;
        }
        return (double) wins / total * 100.0;
    }
}
