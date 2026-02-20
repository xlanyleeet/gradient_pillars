package ua.xlany.gradientpillars.managers;

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
import ua.xlany.gradientpillars.game.GameVisualsManager;
import ua.xlany.gradientpillars.game.GameMechanicsService;
import ua.xlany.gradientpillars.game.modes.GameModeRegistry;
import ua.xlany.gradientpillars.game.modes.GameModeHandler;

import java.util.*;

public class GameManager {

    private final GradientPillars plugin;
    private final GameVisualsManager visualsManager;
    private final GameMechanicsService mechanicsService;
    private final GameModeRegistry modeRegistry;
    private final Map<UUID, Game> playerGames;
    private final Map<String, Game> games;

    public GameManager(GradientPillars plugin) {
        this.plugin = plugin;
        this.visualsManager = new GameVisualsManager(plugin);
        this.mechanicsService = new GameMechanicsService(plugin);
        this.modeRegistry = new GameModeRegistry(plugin);
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

            // Якщо гра активна - дозволити приєднатись як глядач
            if (game.getState() == GameState.ACTIVE) {
                if (arena.getSpectator() == null) {
                    player.sendMessage(
                            plugin.getMessageManager().getPrefixedComponent("errors.spectator-spawn-not-set"));
                    return false;
                }

                joinAsSpectator(player, game);
                return true;
            }

            // Заборонити вхід під час ENDING, RESTORING
            if (game.getState() == GameState.ENDING) {
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

    private void joinAsSpectator(Player player, Game game) {
        playerGames.put(player.getUniqueId(), game);
        game.addSpectator(player.getUniqueId());

        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());

        // Очистити інвентар та ефекти
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setHealth(20.0);
        player.setFoodLevel(20);

        // Телепортувати до точки спостерігача
        if (arena.getSpectator() != null) {
            Location specLoc = arena.getSpectator().clone();
            // Ensure world is set if missing (can happen with file loading)
            if (specLoc.getWorld() == null && arena.getWorldName() != null) {
                specLoc.setWorld(Bukkit.getWorld(arena.getWorldName()));
            }

            if (specLoc.getWorld() != null) {
                player.teleport(specLoc);
            }
        }

        // Встановити режим спостерігача
        player.setGameMode(GameMode.SPECTATOR);

        // Дати предмет для виходу
        giveLeaveItem(player);

        player.sendMessage(plugin.getMessageManager().getPrefixedComponent("game.spectator.now-spectating"));
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

    public Game findGameByArena(String arenaName) {
        return games.values().stream()
                .filter(g -> g.getArenaName().equals(arenaName))
                .findFirst()
                .orElse(null);
    }

    private void startCountdown(Game game) {
        game.setState(GameState.COUNTDOWN);

        // Встановлюємо правила гри для світу арени
        Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
        if (arena != null && arena.getWorldName() != null) {
            World world = Bukkit.getWorld(arena.getWorldName());
            if (world != null) {
                world.setGameRule(org.bukkit.GameRule.DO_IMMEDIATE_RESPAWN, true);
                world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            }
        }

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
                    game.getCountdownTask().cancel();
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

        game.setCountdownTask(task);
    }

    public void skipCountdown(Game game) {
        if (game.getState() != GameState.COUNTDOWN) {
            return;
        }

        // Встановити таймер на 10 секунд
        game.setCountdownTimeLeft(10);
    }

    private void cancelCountdown(Game game) {
        if (game.getCountdownTask() != null) {
            game.getCountdownTask().cancel();
            game.setCountdownTask(null);
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

        // Запустити обраний режим гри
        GameModeHandler modeHandler = modeRegistry.getHandler(selectedMode);
        if (modeHandler != null) {
            modeHandler.apply(game);
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

        game.setGameTask(task);
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

        game.setItemTask(task);
    }

    private void giveItemsToPlayers(Game game) {
        for (UUID playerId : game.getAlivePlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                ItemStack item = plugin.getItemManager().getRandomItem();
                player.getInventory().addItem(item);
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
        try {
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
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save game stats! Skipping...", e);
        }

        // Скасувати всі таймери
        if (game.getCountdownTask() != null) {
            game.getCountdownTask().cancel();
        }
        if (game.getGameTask() != null) {
            game.getGameTask().cancel();
        }
        if (game.getItemTask() != null) {
            game.getItemTask().cancel();
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
                    visualsManager.showWinnerTitle(player, winner);
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

                // Видалити всі ефекти зілля (Jump Boost, Blindness тощо)
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
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
        visualsManager.updateWaitingBossBar(game);
    }

    private void updateCountdownBossBar(Game game, int timeLeft) {
        visualsManager.updateCountdownBossBar(game, timeLeft);
    }

    private void updateGameBossBar(Game game, long remaining) {
        visualsManager.updateGameBossBar(game, remaining);
    }

    private void updateExpBar(Game game) {
        visualsManager.updateExpBar(game);
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
        return findGameByArena(arenaName);
    }

    private void giveLeaveItem(Player player) {
        ItemStack leaveItem = new ItemStack(Material.RED_BED);
        ItemMeta meta = leaveItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.waiting.leave-item"));
            leaveItem.setItemMeta(meta);
        }
        player.getInventory().setItem(8, leaveItem); // Останній слот
    }

    private void giveModeSelectionItem(Player player) {
        ItemStack modeItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = modeItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.waiting.mode-selection-item"));
            modeItem.setItemMeta(meta);
        }
        player.getInventory().setItem(0, modeItem); // Перший слот
    }

    private void giveSkipWaitItem(Player player) {
        ItemStack skipItem = new ItemStack(Material.CLOCK);
        ItemMeta meta = skipItem.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageManager().getComponent("game.waiting.skip-wait-item"));
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

    private void createGlassCage(Game game, UUID playerId, Location center) {
        mechanicsService.createGlassCage(game, playerId, center);
    }

    private void spawnFireworksAroundWinner(Player winner) {
        visualsManager.spawnFireworksAroundWinner(winner);
    }

    public void shutdown() {
        for (Game game : new ArrayList<>(games.values())) {
            endGame(game, null);
        }
    }
}
