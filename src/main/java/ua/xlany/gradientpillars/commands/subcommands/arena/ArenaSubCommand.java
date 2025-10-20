package ua.xlany.gradientpillars.commands.subcommands.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Інтерфейс для підкоманд arena
 */
public interface ArenaSubCommand {

    /**
     * Виконати підкоманду arena
     * 
     * @param player гравець що виконує команду
     * @param args   аргументи команди (БЕЗ "arena" та назви підкоманди)
     * @return true якщо команда виконана успішно
     */
    boolean execute(Player player, String[] args);

    /**
     * Автодоповнення для підкоманди
     * 
     * @param sender відправник команди
     * @param args   аргументи для автодоповнення
     * @return список варіантів автодоповнення
     */
    @Nullable
    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Назва підкоманди
     */
    String getName();

    /**
     * Опис підкоманди
     */
    String getDescription();

    /**
     * Використання підкоманди
     */
    String getUsage();
}
