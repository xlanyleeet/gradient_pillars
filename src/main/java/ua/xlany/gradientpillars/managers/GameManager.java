package ua.xlany.gradientpillars.managers;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameState;

import java.util.*;

public class GameManager {

    private final GradientPillars plugin;
    private final Map<UUID, Game> playerGames;
    private final Map<String, Game> games;

    public GameManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.playerGames = new HashMap<>();
        this.games = new HashMap<>();
    }

    public GradientPillars getPlugin() {
        return plugin;
    }

    public Game createGame(String arenaName) {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game(gameId, this, arenaName);
        games.put(gameId, game);
        return game;
    }

    public boolean joinGame(Player player) {
        if (playerGames.containsKey(player.getUniqueId())) {
            return false;
        }

        // Знайти доступну гру або створити нову
        Game game = findAvailableGame();
        if (game == null) {
            Arena arena = plugin.getArenaManager().getFirstAvailableArena();
            if (arena == null || !arena.isSetup()) {
                return false;
            }
            game = createGame(arena.getName());
        }

        if (!game.addPlayer(player.getUniqueId())) {
            return false;
        }

        playerGames.put(player.getUniqueId(), game);

        // Телепортувати в лобі
        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena != null && arena.getLobby() != null) {
            Location lobby = arena.getLobby().clone();

            // Переконатись що світ встановлено
            if (lobby.getWorld() == null && arena.getWorldName() != null) {
                World world = Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    lobby.setWorld(world);
                } else {
                    plugin.getLogger().severe("Світ арени не знайдено: " + arena.getWorldName());
                    return false;
                }
            }

            player.teleport(lobby);
        }

        // Очистити інвентар
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);

        // Дати предмет для виходу з гри
        giveLeaveItem(player);

        // Повідомлення
        player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.success"));

        // Оновити BossBar
        updateWaitingBossBar(game);

        // Перевірити чи можна почати
        if (game.getPlayerCount() >= plugin.getConfigManager().getMinPlayers()
                && game.getState() == GameState.WAITING) {
            startCountdown(game);
        }

        return true;
    }

    public void leaveGame(Player player) {
        Game game = playerGames.remove(player.getUniqueId());
        if (game == null) {
            return;
        }

        game.removePlayer(player.getUniqueId());

        // Видалити з BossBar
        if (game.getBossBar() != null) {
            game.getBossBar().removeViewer(player);
        }

        // Очистити інвентар та телепортувати в хаб
        player.getInventory().clear();
        teleportToHub(player);

        player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.leave.success"));

        // Якщо гра ще не почалася і гравців недостатньо
        if (game.getState() == GameState.COUNTDOWN &&
                game.getPlayerCount() < plugin.getConfigManager().getMinPlayers()) {
            cancelCountdown(game);
        }

        // Якщо гра активна, перевірити переможця
        if (game.getState() == GameState.ACTIVE) {
            checkWinner(game);
        }

        // Якщо гра порожня, видалити
        if (game.getPlayerCount() == 0) {
            endGame(game, null);
        }
    }

    private Game findAvailableGame() {
        return games.values().stream()
                .filter(g -> g.getState() == GameState.WAITING || g.getState() == GameState.COUNTDOWN)
                .filter(g -> g.getPlayerCount() < plugin.getConfigManager().getMaxPlayers())
                .findFirst()
                .orElse(null);
    }

    private void startCountdown(Game game) {
        game.setState(GameState.COUNTDOWN);

        int countdownTime = plugin.getConfigManager().getCountdownTime();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (game.getPlayerCount() < plugin.getConfigManager().getMinPlayers()) {
                    cancelCountdown(game);
                    return;
                }

                if (timeLeft <= 0) {
                    startGame(game);
                    Bukkit.getScheduler().cancelTask(game.getCountdownTask());
                    return;
                }

                // Повідомлення
                if (timeLeft <= 5 || timeLeft % 10 == 0) {
                    for (UUID playerId : game.getPlayers()) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                                    "game.start.countdown", "time", String.valueOf(timeLeft)));
                        }
                    }
                }

                // Оновити BossBar
                updateCountdownBossBar(game, timeLeft);

                timeLeft--;
            }
        }, 0L, 20L);

        game.setCountdownTask(task.getTaskId());
    }

    private void cancelCountdown(Game game) {
        if (game.getCountdownTask() != 0) {
            Bukkit.getScheduler().cancelTask(game.getCountdownTask());
            game.setCountdownTask(0);
        }
        game.setState(GameState.WAITING);
        updateWaitingBossBar(game);
    }

    private void startGame(Game game) {
        game.setState(GameState.ACTIVE);
        game.setGameStartTime(System.currentTimeMillis());
        game.resetItemCooldown();

        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena == null) {
            return;
        }

        // Телепортувати гравців на стовпи
        List<Location> pillars = arena.getPillars();
        List<UUID> players = new ArrayList<>(game.getPlayers());
        Collections.shuffle(players);

        World arenaWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
        if (arenaWorld == null) {
            plugin.getLogger().severe("Світ арени не знайдено: " + arena.getWorldName());
            return;
        }

        for (int i = 0; i < players.size() && i < pillars.size(); i++) {
            UUID playerId = players.get(i);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && pillars.get(i) != null) {
                Location pillar = pillars.get(i).clone();

                // Переконатись що світ встановлено
                if (pillar.getWorld() == null) {
                    pillar.setWorld(arenaWorld);
                }

                player.teleport(pillar);
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear(); // Очистити інвентар (прибрати ліжко)
                game.assignPillar(playerId, i);
            }
        }

        // Повідомлення
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.start.started"));
            }
        }

        // Запустити таймер гри
        startGameTimer(game);

        // Запустити таймер предметів
        startItemTimer(game);
    }

    private void startGameTimer(Game game) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long elapsed = (System.currentTimeMillis() - game.getGameStartTime()) / 1000;
            long maxDuration = plugin.getConfigManager().getMaxGameDuration();
            long remaining = maxDuration - elapsed;

            if (remaining <= 0) {
                endGame(game, null);
                return;
            }

            // Оновити BossBar
            updateGameBossBar(game, remaining);

        }, 0L, 20L);

        game.setGameTask(task.getTaskId());
    }

    private void startItemTimer(Game game) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (game.getState() != GameState.ACTIVE) {
                return;
            }

            game.decrementItemCooldown();

            // Оновити експ бар
            updateExpBar(game);

            if (game.getItemCooldown() <= 0) {
                giveItemsToPlayers(game);
                game.resetItemCooldown();
            }

        }, 0L, 20L);

        game.setItemTask(task.getTaskId());
    }

    private void giveItemsToPlayers(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                ItemStack item = plugin.getItemManager().getRandomItem();
                player.getInventory().addItem(item);

                String itemName = item.getType().name().toLowerCase().replace("_", " ");
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "game.items.received", "item", itemName));
            }
        }
    }

    public void handlePlayerDeath(Player player) {
        Game game = playerGames.get(player.getUniqueId());
        if (game == null || game.getState() != GameState.ACTIVE) {
            return;
        }

        game.eliminatePlayer(player.getUniqueId());

        // Повідомлення всім гравцям
        for (UUID playerId : game.getPlayers()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                p.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "game.death.eliminated", "player", player.getName()));
                p.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "game.death.remaining", "count", String.valueOf(game.getAlivePlayerCount())));
            }
        }

        checkWinner(game);
    }

    private void checkWinner(Game game) {
        if (game.getAlivePlayerCount() <= 1) {
            UUID winnerId = game.getAlivePlayers().stream().findFirst().orElse(null);
            Player winner = winnerId != null ? Bukkit.getPlayer(winnerId) : null;
            endGame(game, winner);
        }
    }

    private void endGame(Game game, Player winner) {
        game.setState(GameState.ENDING);

        // Скасувати всі таймери
        if (game.getCountdownTask() != 0) {
            Bukkit.getScheduler().cancelTask(game.getCountdownTask());
        }
        if (game.getGameTask() != 0) {
            Bukkit.getScheduler().cancelTask(game.getGameTask());
        }
        if (game.getItemTask() != 0) {
            Bukkit.getScheduler().cancelTask(game.getItemTask());
        }

        // Повідомлення про переможця
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (winner != null) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                            "game.end.winner", "player", winner.getName()));
                } else {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.end.no-winner"));
                }

                // Видалити з BossBar
                if (game.getBossBar() != null) {
                    game.getBossBar().removeViewer(player);
                }

                // Очистити інвентар
                player.getInventory().clear();
                player.setGameMode(GameMode.ADVENTURE);
            }
        }

        // Затримка перед телепортацією в хаб (5 секунд)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID playerId : game.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    // Телепортувати в хаб
                    teleportToHub(player);
                    playerGames.remove(playerId);
                }
            }

            // Відновити світ після телепортації
            plugin.getArenaManager().restoreWorld(game.getArenaName());

            // Видалити гру
            games.remove(game.getId());
        }, 100L); // 5 секунд = 100 тіків
    }

    private void teleportToHub(Player player) {
        if (!plugin.getConfigManager().isHubEnabled()) {
            return;
        }

        String worldName = plugin.getConfigManager().getHubWorld();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Світ хабу '" + worldName + "' не знайдено!");
            return;
        }

        Location hubLocation = new Location(
                world,
                plugin.getConfigManager().getHubX(),
                plugin.getConfigManager().getHubY(),
                plugin.getConfigManager().getHubZ(),
                plugin.getConfigManager().getHubYaw(),
                plugin.getConfigManager().getHubPitch());

        player.teleport(hubLocation);
        player.setGameMode(GameMode.SURVIVAL); // Відновити режим виживання
    }

    private void updateWaitingBossBar(Game game) {
        if (!plugin.getConfigManager().isUseBossBar()) {
            return;
        }

        if (game.getBossBar() == null) {
            BossBar bossBar = BossBar.bossBar(
                    Component.text("Waiting..."),
                    1.0f,
                    BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS);
            game.setBossBar(bossBar);
        }

        String message = plugin.getMessageManager().getMessage("bossbar.waiting",
                "current", String.valueOf(game.getPlayerCount()),
                "max", String.valueOf(plugin.getConfigManager().getMaxPlayers()));

        game.getBossBar().name(Component.text(message));
        game.getBossBar().progress(
                (float) game.getPlayerCount() / plugin.getConfigManager().getMaxPlayers());

        // Додати всіх гравців
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                game.getBossBar().addViewer(player);
            }
        }
    }

    private void updateCountdownBossBar(Game game, int timeLeft) {
        if (!plugin.getConfigManager().isUseBossBar() || game.getBossBar() == null) {
            return;
        }

        String message = plugin.getMessageManager().getMessage("bossbar.starting",
                "time", String.valueOf(timeLeft));

        game.getBossBar().name(Component.text(message));
        game.getBossBar().progress(
                (float) timeLeft / plugin.getConfigManager().getCountdownTime());
        game.getBossBar().color(BossBar.Color.YELLOW);
    }

    private void updateGameBossBar(Game game, long remaining) {
        if (!plugin.getConfigManager().isUseBossBar() || game.getBossBar() == null) {
            return;
        }

        long minutes = remaining / 60;
        long seconds = remaining % 60;
        String timeStr = String.format("%d:%02d", minutes, seconds);

        String message = plugin.getMessageManager().getMessage("bossbar.game-time",
                "time", timeStr);

        game.getBossBar().name(Component.text(message));
        game.getBossBar().progress(
                (float) remaining / plugin.getConfigManager().getMaxGameDuration());
        game.getBossBar().color(BossBar.Color.GREEN);
    }

    private void updateExpBar(Game game) {
        if (!plugin.getConfigManager().isUseExpBar()) {
            return;
        }

        int maxCooldown = plugin.getConfigManager().getItemInterval();
        int currentCooldown = game.getItemCooldown();

        float progress = (float) currentCooldown / maxCooldown;
        int level = currentCooldown;

        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setLevel(level);
                player.setExp(progress);
            }
        }
    }

    public Game getPlayerGame(UUID playerId) {
        return playerGames.get(playerId);
    }

    public boolean isInGame(UUID playerId) {
        return playerGames.containsKey(playerId);
    }

    private void giveLeaveItem(Player player) {
        ItemStack leaveItem = new ItemStack(Material.RED_BED);
        ItemMeta meta = leaveItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.lobby.leave-item"));
            leaveItem.setItemMeta(meta);
        }
        player.getInventory().setItem(8, leaveItem); // Останній слот
    }

    public void shutdown() {
        for (Game game : new ArrayList<>(games.values())) {
            endGame(game, null);
        }
    }
}
