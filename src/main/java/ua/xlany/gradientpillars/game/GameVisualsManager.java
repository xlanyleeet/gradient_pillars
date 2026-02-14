package ua.xlany.gradientpillars.game;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Arena;
import ua.xlany.gradientpillars.models.Game;

import java.util.UUID;

public class GameVisualsManager {

    private final GradientPillars plugin;

    public GameVisualsManager(GradientPillars plugin) {
        this.plugin = plugin;
    }

    public void updateWaitingBossBar(Game game) {
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

    public void updateCountdownBossBar(Game game, int timeLeft) {
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

    public void updateGameBossBar(Game game, long remaining) {
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

    public void updateExpBar(Game game) {
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

    public void spawnFireworksAroundWinner(Player winner) {
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
            Firework firework = world.spawn(fireworkLoc, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();

            // Випадковий колір
            Color[] colors = {
                    Color.RED,
                    Color.YELLOW,
                    Color.LIME,
                    Color.AQUA,
                    Color.FUCHSIA,
                    Color.ORANGE,
                    Color.WHITE
            };
            Color color1 = colors[(int) (Math.random() * colors.length)];
            Color color2 = colors[(int) (Math.random() * colors.length)];

            // Випадковий тип ефекту
            FireworkEffect.Type[] types = {
                    FireworkEffect.Type.BALL,
                    FireworkEffect.Type.BALL_LARGE,
                    FireworkEffect.Type.STAR,
                    FireworkEffect.Type.BURST
            };
            FireworkEffect.Type type = types[(int) (Math.random() * types.length)];

            // Створити ефект
            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(color1, color2)
                    .withFade(Color.WHITE)
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

    public void showWinnerTitle(Player player, Player winner) {
        Component winnerTitle = plugin.getMessageManager().getComponent("game.end.winner-title");
        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text(winner.getName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                winnerTitle.color(NamedTextColor.YELLOW),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(1000))));
    }
}