package ua.xlany.gradientpillars.models;

import net.kyori.adventure.bossbar.BossBar;
import ua.xlany.gradientpillars.managers.GameManager;

import java.util.*;

public class Game {

    private final String id;
    private final GameManager gameManager;
    private final String arenaName;
    private final Set<UUID> players;
    private final Set<UUID> alivePlayers;
    private final Set<UUID> spectators;
    private final Map<UUID, Integer> pillarAssignments;

    private GameState state;
    private BossBar bossBar;
    private int countdownTask;
    private int gameTask;
    private int itemTask;
    private long gameStartTime;
    private int itemCooldown;
    private boolean wasActive; // Чи гра була активною (для визначення чи треба регенерувати світ)

    public Game(String id, GameManager gameManager, String arenaName) {
        this.id = id;
        this.gameManager = gameManager;
        this.arenaName = arenaName;
        this.players = new HashSet<>();
        this.alivePlayers = new HashSet<>();
        this.spectators = new HashSet<>();
        this.pillarAssignments = new HashMap<>();
        this.state = GameState.WAITING;
        this.itemCooldown = 0;
        this.wasActive = false;
    }

    public String getId() {
        return id;
    }

    public String getArenaName() {
        return arenaName;
    }

    public Set<UUID> getPlayers() {
        return new HashSet<>(players);
    }

    public Set<UUID> getAlivePlayers() {
        return new HashSet<>(alivePlayers);
    }

    public Set<UUID> getSpectators() {
        return new HashSet<>(spectators);
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public boolean addPlayer(UUID playerId) {
        Arena arena = gameManager.getPlugin().getArenaManager().getArena(arenaName);
        if (arena == null || players.size() >= arena.getMaxPlayers()) {
            return false;
        }
        players.add(playerId);
        alivePlayers.add(playerId);
        return true;
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
        alivePlayers.remove(playerId);
        spectators.remove(playerId);
        pillarAssignments.remove(playerId);
    }

    public void eliminatePlayer(UUID playerId) {
        alivePlayers.remove(playerId);
        spectators.add(playerId);
    }

    public boolean isPlayerAlive(UUID playerId) {
        return alivePlayers.contains(playerId);
    }

    public boolean isPlayerInGame(UUID playerId) {
        return players.contains(playerId);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }

    public void assignPillar(UUID playerId, int pillarIndex) {
        pillarAssignments.put(playerId, pillarIndex);
    }

    public Integer getPillarAssignment(UUID playerId) {
        return pillarAssignments.get(playerId);
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public int getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(int countdownTask) {
        this.countdownTask = countdownTask;
    }

    public int getGameTask() {
        return gameTask;
    }

    public void setGameTask(int gameTask) {
        this.gameTask = gameTask;
    }

    public int getItemTask() {
        return itemTask;
    }

    public void setItemTask(int itemTask) {
        this.itemTask = itemTask;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }

    public int getItemCooldown() {
        return itemCooldown;
    }

    public void setItemCooldown(int itemCooldown) {
        this.itemCooldown = itemCooldown;
    }

    public void decrementItemCooldown() {
        if (itemCooldown > 0) {
            itemCooldown--;
        }
    }

    public void resetItemCooldown() {
        this.itemCooldown = gameManager.getPlugin().getConfigManager().getItemInterval();
    }

    public boolean wasActive() {
        return wasActive;
    }

    public void setWasActive(boolean wasActive) {
        this.wasActive = wasActive;
    }

    /**
     * Скинути гру до початкового стану (для перевикористання)
     */
    public void reset() {
        // Очистити всіх гравців
        players.clear();
        alivePlayers.clear();
        spectators.clear();
        pillarAssignments.clear();

        // Скинути стан
        state = GameState.WAITING;

        // Видалити BossBar якщо існує
        if (bossBar != null) {
            bossBar = null;
        }

        // Скинути таски
        countdownTask = 0;
        gameTask = 0;
        itemTask = 0;

        // Скинути час та кулдаун
        gameStartTime = 0;
        itemCooldown = 0;

        // Скинути прапорець активності
        wasActive = false;
    }
}