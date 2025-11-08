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
        return joinGame(player, null);
    }

    public boolean joinGame(Player player, String targetArenaName) {
        if (playerGames.containsKey(player.getUniqueId())) {
            return false;
        }

        Arena arena;

        if (targetArenaName != null && !targetArenaName.trim().isEmpty()) {
            arena = plugin.getArenaManager().getArenas().stream()
                    .filter(a -> a != null && a.getName() != null
                            && a.getName().equalsIgnoreCase(targetArenaName))
                    .findFirst()
                    .orElse(null);

            if (arena == null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-found"));
                return false;
            }

            if (!arena.isSetup()) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-setup"));
                return false;
            }
        } else {
            arena = plugin.getArenaManager().getFirstAvailableArena();

            if (arena == null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.arena-not-found"));
                return false;
            }
        }

        // Перевірити чи вже існує гра на цій арені
        Game game = findGameByArena(arena.getName());

        // Якщо гри немає - створити нову
        if (game == null) {
            game = createGame(arena.getName());
        } else {
            // Якщо гра існує - перевірити чи можна до неї приєднатись

            // Заборонити вхід під час ACTIVE, ENDING, RESTORING
            if (game.getState() == GameState.ACTIVE || game.getState() == GameState.ENDING) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.already-started"));
                return false;
            }

            if (game.getState() == GameState.RESTORING) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.world-restoring"));
                return false;
            }

            // Перевірити чи не повна гра
            if (game.getPlayerCount() >= arena.getMaxPlayers()) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.full"));
                return false;
            }
        }

        if (!game.addPlayer(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.game-full"));
            return false;
        }

        playerGames.put(player.getUniqueId(), game);

        // Телепортувати в лобі
        if (arena.getLobby() != null) {
            Location lobby = arena.getLobby().clone();

            // Переконатись що світ встановлено
            if (lobby.getWorld() == null && arena.getWorldName() != null) {
                World world = Bukkit.getWorld(arena.getWorldName());
                if (world != null) {
                    lobby.setWorld(world);
                } else {
                    plugin.getLogger().severe("Світ арени не знайдено: " + arena.getWorldName());
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.world-not-found"));
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

        // Дати годинник пропуску очікування для гравців з правом
        if (player.hasPermission("gradientpillars.skipwait")) {
            giveSkipWaitItem(player);
        }

        // Повідомлення
        player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.join.success"));

        // Оновити BossBar
        updateWaitingBossBar(game);

        // Перевірити чи можна почати
        // Перезавантажити арену для актуальних налаштувань
        arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena != null && game.getPlayerCount() >= arena.getMinPlayers()
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
        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena != null && game.getState() == GameState.COUNTDOWN &&
                game.getPlayerCount() < arena.getMinPlayers()) {
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

    private Game findGameByArena(String arenaName) {
        return games.values().stream()
                .filter(g -> g.getArenaName().equals(arenaName))
                .findFirst()
                .orElse(null);
    }

    private void startCountdown(Game game) {
        game.setState(GameState.COUNTDOWN);

        int countdownTime = plugin.getConfigManager().getCountdownTime();
        game.setCountdownTimeLeft(countdownTime);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                int timeLeft = game.getCountdownTimeLeft();

                Arena currentArena = plugin.getArenaManager().getArena(game.getArenaName());
                if (currentArena == null || game.getPlayerCount() < currentArena.getMinPlayers()) {
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

                // Видалити годинник пропуску очікування коли залишилось 10 секунд
                if (timeLeft == 10) {
                    removeSkipWaitItems(game);
                }

                // Оновити BossBar
                updateCountdownBossBar(game, timeLeft);

                game.setCountdownTimeLeft(timeLeft - 1);
            }
        }, 0L, 20L);

        game.setCountdownTask(task.getTaskId());
    }

    public void skipCountdown(Game game) {
        if (game.getState() != GameState.COUNTDOWN) {
            return;
        }

        // Встановити таймер на 10 секунд
        game.setCountdownTimeLeft(10);
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
        game.setWasActive(true); // Позначити що гра почалась (для регенерації світу)
        game.setGameStartTime(System.currentTimeMillis());
        game.resetItemCooldown();

        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena == null) {
            return;
        }

        // Видалити блоки лобі очікування в радіусі 10 блоків від спавну лобі
        removeLobbyWaitingBlocks(arena);

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

    private void removeLobbyWaitingBlocks(Arena arena) {
        Location lobby = arena.getLobby();
        if (lobby == null || lobby.getWorld() == null) {
            return;
        }

        World world = lobby.getWorld();
        int centerX = lobby.getBlockX();
        int centerY = lobby.getBlockY();
        int centerZ = lobby.getBlockZ();
        int radius = 10;

        // Видалити всі блоки в радіусі 10 блоків від спавну лобі
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    // Перевірка відстані (сферичний радіус)
                    double distance = Math.sqrt(
                        Math.pow(x - centerX, 2) +
                        Math.pow(y - centerY, 2) +
                        Math.pow(z - centerZ, 2)
                    );

                    if (distance <= radius) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
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

        // Зберегти статистику
        if (winner != null) {
            // Переможець отримує перемогу
            plugin.getStatsManager().addWin(winner.getUniqueId(), winner.getName());

            // Всі інші отримують поразку
            for (UUID playerId : game.getPlayers()) {
                if (!playerId.equals(winner.getUniqueId())) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        plugin.getStatsManager().addLoss(playerId, player.getName());
                    }
                }
            }
        }

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
        // Перевіряємо чи плагін ще активний перед створенням таску
        if (!plugin.isEnabled()) {
            // Плагін вимикається - просто телепортуємо гравців і скидаємо гру
            for (UUID playerId : game.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    teleportToHub(player);
                    playerGames.remove(playerId);
                }
            }
            game.reset();
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Телепортувати всіх гравців в хаб
            for (UUID playerId : game.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    teleportToHub(player);
                    playerGames.remove(playerId);
                }
            }

            // Перевірити чи гра була активною (чи треба регенерувати світ)
            boolean needsRegeneration = game.wasActive();

            // СПОЧАТКУ скидаємо гру (очищуємо гравців, стан → WAITING)
            game.reset();

            if (needsRegeneration) {
                // Гра була активною - відновлюємо світ
                plugin.getLogger()
                        .info("Гру на арені '" + game.getArenaName() + "' скинуто. Починаю відновлення світу...");
                plugin.getArenaManager().restoreWorld(game.getArenaName());
            } else {
                // Гра не почалась (був тільки лобі) - світ чистий, регенерація не потрібна
                plugin.getLogger().info("Гру на арені '" + game.getArenaName()
                        + "' скинуто. Світ не змінювався, регенерація пропущена.");
            }

            // Гра готова приймати нових гравців!
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

        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena == null) {
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
                "max", String.valueOf(arena.getMaxPlayers()));

        game.getBossBar().name(Component.text(message));
        game.getBossBar().progress(
                Math.min(1.0f, (float) game.getPlayerCount() / arena.getMaxPlayers()));

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

    /**
     * Отримати гру по імені арени
     * 
     * @param arenaName назва арени
     * @return гра або null якщо не знайдено
     */
    public Game getGameByArena(String arenaName) {
        return games.get(arenaName);
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

    private void giveSkipWaitItem(Player player) {
        ItemStack skipItem = new ItemStack(Material.CLOCK);
        ItemMeta meta = skipItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.lobby.skip-wait-item"));
            skipItem.setItemMeta(meta);
        }
        player.getInventory().setItem(4, skipItem); // Центральний слот
    }

    private void removeSkipWaitItems(Game game) {
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                ItemStack itemInSlot = player.getInventory().getItem(4);
                if (itemInSlot != null && itemInSlot.getType() == Material.CLOCK) {
                    player.getInventory().setItem(4, null);
                }
            }
        }
    }

    public void shutdown() {
        for (Game game : new ArrayList<>(games.values())) {
            endGame(game, null);
        }
    }
}
