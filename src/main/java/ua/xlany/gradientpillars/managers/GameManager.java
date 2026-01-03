package ua.xlany.gradientpillars.managers;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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

        // Знайти вільний стовп для гравця
        List<Location> pillars = arena.getPillars();
        int pillarIndex = game.getPlayerCount() - 1; // Індекс для нового гравця
        
        if (pillarIndex >= pillars.size()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.no-free-pillar"));
            game.removePlayer(player.getUniqueId());
            playerGames.remove(player.getUniqueId());
            return false;
        }

        Location pillar = pillars.get(pillarIndex);
        if (pillar == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.pillar-not-set"));
            game.removePlayer(player.getUniqueId());
            playerGames.remove(player.getUniqueId());
            return false;
        }

        // Переконатись що світ встановлено
        World arenaWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
        if (arenaWorld == null) {
            plugin.getLogger().severe("Світ арени не знайдено: " + arena.getWorldName());
            player.sendMessage(plugin.getMessageManager().getPrefixedComponent("errors.world-not-found"));
            game.removePlayer(player.getUniqueId());
            playerGames.remove(player.getUniqueId());
            return false;
        }

        if (pillar.getWorld() == null) {
            pillar.setWorld(arenaWorld);
        }

        // Призначити стовп гравцю
        game.assignPillar(player.getUniqueId(), pillarIndex);

        // Телепортувати на стовп (+3 блоки вгору) в клітці зі скла
        Location spawnLocation = pillar.clone().add(0, 3, 0);
        player.teleport(spawnLocation);

        // Очистити інвентар
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);

        // Створити клітку зі скла навколо гравця
        createGlassCage(game, player.getUniqueId(), spawnLocation);

        // Дати предмет для виходу з гри
        giveLeaveItem(player);

        // Дати предмет для вибору режиму гри
        giveModeSelectionItem(player);

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

        // Видалити клітку гравця
        game.removeCageBlocks(player.getUniqueId());

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

        // Визначити переможний режим гри на основі голосування
        ua.xlany.gradientpillars.models.GameMode selectedMode = game.calculateWinningMode();
        game.setSelectedMode(selectedMode);

        // Гравці вже на стовпах в клітках - тільки змінити режим гри та видалити клітки
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear(); // Очистити інвентар (прибрати предмети очікування)

                // Встановити HP відповідно до режиму
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(selectedMode.getHealth());
                player.setHealth(selectedMode.getHealth());
            }
        }

        // Видалити всі клітки через 1 тік
        Bukkit.getScheduler().runTaskLater(plugin, () -> game.clearAllCageBlocks(), 1L);

        // Повідомлення про початок та режим гри
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.start.started"));

                // Повідомити про обраний режим
                if (selectedMode != ua.xlany.gradientpillars.models.GameMode.NORMAL) {
                    String modeName = plugin.getMessageManager().getMessage(selectedMode.getTranslationKey() + ".name");
                    String modeDesc = plugin.getMessageManager()
                            .getMessage(selectedMode.getTranslationKey() + ".description");
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                            "game.mode.active",
                            "mode", modeName,
                            "description", modeDesc));
                }
            }
        }

        // Запустити таймер гри
        startGameTimer(game);

        // Запустити таймер предметів
        startItemTimer(game);

        // Запустити режим лави якщо обрано
        if (selectedMode == ua.xlany.gradientpillars.models.GameMode.RISING_LAVA) {
            startRisingLava(game, arena);
        }

        // Застосувати Jump Boost якщо обрано
        if (selectedMode == ua.xlany.gradientpillars.models.GameMode.JUMP_BOOST) {
            applyJumpBoost(game);
        }

        // Застосувати No Jump якщо обрано
        if (selectedMode == ua.xlany.gradientpillars.models.GameMode.NO_JUMP) {
            applyNoJump(game);
        }

        // Застосувати Darkness якщо обрано
        if (selectedMode == ua.xlany.gradientpillars.models.GameMode.DARKNESS) {
            applyDarkness(game);
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
        if (game.getLavaTask() != 0) {
            Bukkit.getScheduler().cancelTask(game.getLavaTask());
        }

        // Повідомлення про переможця
        for (UUID playerId : game.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (winner != null) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                            "game.end.winner", "player", winner.getName()));

                    // Показати title/subtitle
                    Component winnerTitle = plugin.getMessageManager().getComponent("game.end.winner-title");
                    player.showTitle(net.kyori.adventure.title.Title.title(
                            Component.text(winner.getName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                            winnerTitle.color(NamedTextColor.YELLOW),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3000),
                                    java.time.Duration.ofMillis(1000))));
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

                // Відновити стандартні атрибути
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
                player.setHealth(20.0);
                player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.42); // Стандартне значення
            }
        }

        // Феєрверки навколо переможця
        if (winner != null && winner.isOnline()) {
            spawnFireworksAroundWinner(winner);
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

    private void giveModeSelectionItem(Player player) {
        ItemStack modeItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = modeItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.lobby.mode-selection-item"));
            modeItem.setItemMeta(meta);
        }
        player.getInventory().setItem(0, modeItem); // Перший слот
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

    /**
     * Створює клітку зі скла навколо гравця (3x3x3 по периметру)
     * 
     * @param game     Гра до якої відноситься клітка
     * @param playerId UUID гравця
     * @param center   Центр клітки (позиція гравця)
     */
    private void createGlassCage(Game game, UUID playerId, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Створюємо клітку 3x3x3
        // Підлога (Y-1): всі 9 блоків
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = new Location(world, centerX + x, centerY - 1, centerZ + z);
                world.getBlockAt(blockLoc).setType(Material.GLASS);
                game.addCageBlock(playerId, blockLoc);
            }
        }

        // Стіни (Y та Y+1): тільки по периметру
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    // Пропускаємо центральний блок (там де гравець)
                    if (x != 0 || z != 0) {
                        Location blockLoc = new Location(world, centerX + x, centerY + y, centerZ + z);
                        world.getBlockAt(blockLoc).setType(Material.GLASS);
                        game.addCageBlock(playerId, blockLoc);
                    }
                }
            }
        }

        // Дах (Y+2): всі 9 блоків
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = new Location(world, centerX + x, centerY + 2, centerZ + z);
                world.getBlockAt(blockLoc).setType(Material.GLASS);
                game.addCageBlock(playerId, blockLoc);
            }
        }
    }

    /**
     * Запустити режим підняття лави
     */
    private void startRisingLava(Game game, Arena arena) {
        // Встановити початкову та максимальну висоту лави
        game.setCurrentLavaY(arena.getMinY());
        game.setMaxLavaY(arena.getMaxY());

        // Отримати тривалість гри
        long maxGameDuration = plugin.getConfigManager().getMaxGameDuration(); // в секундах

        // Обчислити швидкість підняття (блоків за секунду)
        int totalHeight = arena.getMaxY() - arena.getMinY();

        // Підняття лави кожні N тіків
        // Ми хочемо, щоб лава піднялась на всю висоту за час гри
        // тіків на гру = maxGameDuration * 20
        // тіків між підняттями = (maxGameDuration * 20) / totalHeight
        long ticksBetweenRises = (maxGameDuration * 20) / Math.max(totalHeight, 1);

        // Мінімум 20 тіків (1 секунда) між підняттями
        ticksBetweenRises = Math.max(20, ticksBetweenRises);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (game.getState() != GameState.ACTIVE) {
                return;
            }

            int currentY = game.getCurrentLavaY();
            int maxY = game.getMaxLavaY();

            // Перевірити чи лава досягла максимальної висоти
            if (currentY >= maxY) {
                return;
            }

            // Підняти лаву на 1 блок
            currentY++;
            game.setCurrentLavaY(currentY);

            // Заповнити шар лави
            Location lobby = arena.getLobby();
            if (lobby == null || lobby.getWorld() == null) {
                return;
            }

            World world = lobby.getWorld();

            // Визначити область для заповнення лавою (навколо всіх стовпів)
            List<Location> pillars = arena.getPillars();
            if (pillars.isEmpty()) {
                return;
            }

            // Знайти межі арени на основі стовпів
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (Location pillar : pillars) {
                if (pillar != null) {
                    minX = Math.min(minX, pillar.getBlockX());
                    maxX = Math.max(maxX, pillar.getBlockX());
                    minZ = Math.min(minZ, pillar.getBlockZ());
                    maxZ = Math.max(maxZ, pillar.getBlockZ());
                }
            }

            // Додати відступ
            int padding = 20;
            minX -= padding;
            maxX += padding;
            minZ -= padding;
            maxZ += padding;

            // Заповнити шар лави
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location blockLoc = new Location(world, x, currentY, z);
                    if (world.getBlockAt(blockLoc).getType() == Material.AIR) {
                        world.getBlockAt(blockLoc).setType(Material.LAVA);
                    }
                }
            }

        }, 0L, ticksBetweenRises);

        game.setLavaTask(task.getTaskId());
    }

    /**
     * Застосувати ефект Jump Boost I до всіх гравців
     */
    private void applyJumpBoost(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false));
            }
        }
    }

    /**
     * Застосувати ефект No Jump до всіх гравців (блокування стрибків)
     */
    private void applyNoJump(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Використовуємо атрибут Jump Strength замість ефекту - набагато оптимальніше
                player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).setBaseValue(0.0);
            }
        }
    }

    /**
     * Застосувати ефект Darkness до всіх гравців (обмежена видимість)
     */
    private void applyDarkness(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Ефект сліпоти для обмеженої видимості
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false));
            }
        }
    }

    /**
     * Запустити феєрверки навколо переможця
     */
    private void spawnFireworksAroundWinner(Player winner) {
        Location winnerLoc = winner.getLocation();
        World world = winnerLoc.getWorld();

        if (world == null) {
            return;
        }

        // Запускати феєрверки протягом 5 секунд (100 тіків)
        final int[] count = { 0 };
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (count[0] >= 10 || !winner.isOnline()) { // 10 феєрверків (по 2 на секунду)
                return;
            }

            // Випадкова позиція навколо гравця (радіус 3-5 блоків)
            double angle = Math.random() * 2 * Math.PI;
            double radius = 3 + Math.random() * 2;
            double x = winnerLoc.getX() + Math.cos(angle) * radius;
            double z = winnerLoc.getZ() + Math.sin(angle) * radius;
            double y = winnerLoc.getY() + 1;

            Location fireworkLoc = new Location(world, x, y, z);

            // Створити феєрверк
            org.bukkit.entity.Firework firework = world.spawn(fireworkLoc, org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();

            // Випадковий колір
            org.bukkit.Color[] colors = {
                    org.bukkit.Color.RED,
                    org.bukkit.Color.YELLOW,
                    org.bukkit.Color.LIME,
                    org.bukkit.Color.AQUA,
                    org.bukkit.Color.FUCHSIA,
                    org.bukkit.Color.ORANGE,
                    org.bukkit.Color.WHITE
            };
            org.bukkit.Color color1 = colors[(int) (Math.random() * colors.length)];
            org.bukkit.Color color2 = colors[(int) (Math.random() * colors.length)];

            // Випадковий тип ефекту
            org.bukkit.FireworkEffect.Type[] types = {
                    org.bukkit.FireworkEffect.Type.BALL,
                    org.bukkit.FireworkEffect.Type.BALL_LARGE,
                    org.bukkit.FireworkEffect.Type.STAR,
                    org.bukkit.FireworkEffect.Type.BURST
            };
            org.bukkit.FireworkEffect.Type type = types[(int) (Math.random() * types.length)];

            // Створити ефект
            org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                    .withColor(color1, color2)
                    .withFade(org.bukkit.Color.WHITE)
                    .with(type)
                    .trail(true)
                    .flicker(Math.random() > 0.5)
                    .build();

            meta.addEffect(effect);
            meta.setPower(0); // Швидкий вибух
            firework.setFireworkMeta(meta);

            count[0]++;
        }, 0L, 10L); // Кожні 0.5 секунди

        // Скасувати таск через 5 секунд
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(task.getTaskId());
        }, 100L);
    }

    public void shutdown() {
        for (Game game : new ArrayList<>(games.values())) {
            endGame(game, null);
        }
    }
}
