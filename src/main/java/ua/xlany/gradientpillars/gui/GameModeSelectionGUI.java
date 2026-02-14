package ua.xlany.gradientpillars.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.xlany.gradientpillars.GradientPillars;
import ua.xlany.gradientpillars.models.Game;
import ua.xlany.gradientpillars.models.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI для вибору режиму гри
 */
public class GameModeSelectionGUI {

    private final GradientPillars plugin;
    private final Game game;

    public GameModeSelectionGUI(GradientPillars plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    /**
     * Відкрити GUI для гравця
     */
    public void open(Player player) {
        Component title = plugin.getMessageManager().getComponent("gui.mode-selection.title");

        ChestGui gui = new ChestGui(3, LegacyComponentSerializer.legacySection().serialize(title.color(NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD)));
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 1, 9, 1); // Row 2 (index 1)

        // Отримати підрахунок голосів
        Map<GameMode, Integer> voteCounts = game.getVoteCounts();

        // Розмістити режими в один ряд по центру (зсув на 1 слот, щоб почати з 2-го
        // слота рядка, як у старому коді (слот 10))
        // Старий код: slot = 10 -> це x=1, y=1
        int x = 1;
        for (GameMode mode : GameMode.values()) {
            ItemStack item = createModeItem(mode, voteCounts.getOrDefault(mode, 0), player);

            GuiItem guiItem = new GuiItem(item, event -> {
                // Зареєструвати голос
                game.voteForMode(player.getUniqueId(), mode);

                // Повідомити гравця
                String modeName = plugin.getMessageManager().getMessage(mode.getTranslationKey() + ".name");
                player.sendMessage(plugin.getMessageManager().getPrefixedComponent(
                        "game.mode.voted",
                        "mode", modeName));

                // Оновити GUI для відображення нового голосу
                // Re-opening triggers a new inventory, effectively "updating" it
                new GameModeSelectionGUI(plugin, game).open(player);
            });

            pane.addItem(guiItem, x, 0);
            x++;
        }

        gui.addPane(pane);
        gui.show(player);
    }

    /**
     * Створити предмет для режиму
     */
    private ItemStack createModeItem(GameMode mode, int votes, Player player) {
        ItemStack item = new ItemStack(mode.getIcon());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Назва режиму з перекладу
            String modeName = plugin.getMessageManager().getMessage(mode.getTranslationKey() + ".name");
            meta.displayName(Component.text(modeName, NamedTextColor.GOLD, TextDecoration.BOLD));

            // Лор (опис)
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            // Опис режиму
            String modeDesc = plugin.getMessageManager().getMessage(mode.getTranslationKey() + ".description");
            lore.add(Component.text(modeDesc, NamedTextColor.GRAY));

            lore.add(Component.empty());

            // Голоси
            String votesText = plugin.getMessageManager().getMessage("gui.mode-selection.votes");
            lore.add(Component.text(votesText, NamedTextColor.YELLOW)
                    .append(Component.text(votes, NamedTextColor.WHITE)));

            // Якщо гравець вже проголосував за цей режим
            if (game.getPlayerVote(player.getUniqueId()) == mode) {
                lore.add(Component.empty());
                String votedText = plugin.getMessageManager().getMessage("gui.mode-selection.voted");
                lore.add(Component.text(votedText, NamedTextColor.GREEN));
            } else {
                lore.add(Component.empty());
                String clickText = plugin.getMessageManager().getMessage("gui.mode-selection.click-to-select");
                lore.add(Component.text(clickText, NamedTextColor.AQUA));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}
