package ua.xlany.gradientpillars.models;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
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
    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private BukkitTask itemTask;
    private long gameStartTime;
    private int itemCooldown;
    private boolean wasActive; // Чи гра була активною (для визначення чи треба регенерувати світ)
    private int countdownTimeLeft; // Час що залишився до початку гри
    private final Map<UUID, List<Location>> playerCageBlocks; // Блоки клітки для кожного гравця

    // Голосування за режим гри
    private final Map<UUID, GameMode> modeVotes; // Голоси гравців за режим
    private GameMode selectedMode; // Обраний режим після голосування

    // Режим підняття лави
    private int lavaTask; // ID таска для підняття лави
    private int currentLavaY; // Поточна висота лави
    private int maxLavaY; // Максимальна висота лави

    public Game(String id, GameManager gameManager, String arenaName) {
        this.id = id;
        this.gameManager = gameManager;
        this.arenaName = arenaName;
        this.players = new HashSet<>();
        this.alivePlayers = new HashSet<>();
        this.spectators = new HashSet<>();
        this.pillarAssignments = new HashMap<>();
        this.playerCageBlocks = new HashMap<>();
        this.modeVotes = new HashMap<>();
        this.state = GameState.WAITING;
        this.itemCooldown = 0;
        this.wasActive = false;
        this.selectedMode = GameMode.NORMAL; // За замовчуванням звичайний режим
        this.lavaTask = 0;
        this.currentLavaY = 0;
        this.maxLavaY = 0;
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

    public void addSpectator(UUID playerId) {
        players.add(playerId); // Add to players list to track them in game
        spectators.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
        alivePlayers.remove(playerId);
        spectators.remove(playerId);
        pillarAssignments.remove(playerId);
    }

    public void removeSpectator(UUID playerId) {
        spectators.remove(playerId);
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

    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(BukkitTask countdownTask) {
        this.countdownTask = countdownTask;
    }

    public BukkitTask getGameTask() {
        return gameTask;
    }

    public void setGameTask(BukkitTask gameTask) {
        this.gameTask = gameTask;
    }

    public BukkitTask getItemTask() {
        return itemTask;
    }

    public void setItemTask(BukkitTask itemTask) {
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

    public int getCountdownTimeLeft() {
        return countdownTimeLeft;
    }

    public void setCountdownTimeLeft(int countdownTimeLeft) {
        this.countdownTimeLeft = countdownTimeLeft;
    }

    public Map<UUID, List<Location>> getPlayerCageBlocks() {
        return playerCageBlocks;
    }

    public void addCageBlock(UUID playerId, Location location) {
        playerCageBlocks.computeIfAbsent(playerId, k -> new ArrayList<>()).add(location);
    }

    public void removeCageBlocks(UUID playerId) {
        List<Location> blocks = playerCageBlocks.remove(playerId);
        if (blocks != null && !blocks.isEmpty()) {
            for (Location loc : blocks) {
                if (loc.getWorld() != null) {
                    loc.getWorld().getBlockAt(loc).setType(org.bukkit.Material.AIR);
                }
            }
        }
    }

    public void clearAllCageBlocks() {
        for (UUID playerId : new ArrayList<>(playerCageBlocks.keySet())) {
            removeCageBlocks(playerId);
        }
        playerCageBlocks.clear();
    }

    // ========================================
    // Методи для голосування за режим
    // ========================================

    /**
     * Проголосувати за режим гри
     * 
     * @param playerId UUID гравця
     * @param mode     Режим за який голосує гравець
     */
    public void voteForMode(UUID playerId, GameMode mode) {
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            modeVotes.put(playerId, mode);
        }
    }

    /**
     * Отримати голос гравця
     * 
     * @param playerId UUID гравця
     * @return Режим за який проголосував гравець, або null
     */
    public GameMode getPlayerVote(UUID playerId) {
        return modeVotes.get(playerId);
    }

    /**
     * Підрахувати голоси та визначити переможний режим
     * 
     * @return Режим з найбільшою кількістю голосів
     */
    public GameMode calculateWinningMode() {
        if (modeVotes.isEmpty()) {
            return GameMode.NORMAL;
        }

        Map<GameMode, Integer> voteCounts = new HashMap<>();
        for (GameMode mode : modeVotes.values()) {
            voteCounts.put(mode, voteCounts.getOrDefault(mode, 0) + 1);
        }

        GameMode winningMode = GameMode.NORMAL;
        int maxVotes = 0;

        for (Map.Entry<GameMode, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winningMode = entry.getKey();
            }
        }

        return winningMode;
    }

    /**
     * Отримати кількість голосів за кожен режим
     * 
     * @return Мапа режимів та їх кількості голосів
     */
    public Map<GameMode, Integer> getVoteCounts() {
        Map<GameMode, Integer> voteCounts = new HashMap<>();
        for (GameMode mode : modeVotes.values()) {
            voteCounts.put(mode, voteCounts.getOrDefault(mode, 0) + 1);
        }
        return voteCounts;
    }

    public GameMode getSelectedMode() {
        return selectedMode;
    }

    public void setSelectedMode(GameMode selectedMode) {
        this.selectedMode = selectedMode;
    }

    // ========================================
    // Методи для режиму підняття лави
    // ========================================

    public int getLavaTask() {
        return lavaTask;
    }

    public void setLavaTask(int lavaTask) {
        this.lavaTask = lavaTask;
    }

    public int getCurrentLavaY() {
        return currentLavaY;
    }

    public void setCurrentLavaY(int currentLavaY) {
        this.currentLavaY = currentLavaY;
    }

    public int getMaxLavaY() {
        return maxLavaY;
    }

    public void setMaxLavaY(int maxLavaY) {
        this.maxLavaY = maxLavaY;
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
        clearAllCageBlocks();
        modeVotes.clear();

        // Скинути стан
        state = GameState.WAITING;

        // Видалити BossBar якщо існує
        if (bossBar != null) {
            bossBar = null;
        }

        // Скинути таски
        countdownTask = null;
        gameTask = null;
        itemTask = null;
        lavaTask = 0;

        // Скинути час та кулдаун
        gameStartTime = 0;
        itemCooldown = 0;

        // Скинути прапорець активності
        wasActive = false;

        // Скинути режим
        selectedMode = GameMode.NORMAL;
        currentLavaY = 0;
        maxLavaY = 0;
    }
}