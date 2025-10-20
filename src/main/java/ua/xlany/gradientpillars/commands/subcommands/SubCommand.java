package ua.xlany.gradientpillars.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Базовий інтерфейс для підкоманд
 */
public interface SubCommand {

    /**
     * Виконати команду
     * 
     * @param sender Відправник команди
     * @param args   Аргументи команди (без назви підкоманди)
     * @return true якщо команда виконана успішно
     */
    boolean execute(CommandSender sender, String[] args);

    /**
     * Отримати автодоповнення для команди
     * 
     * @param sender Відправник команди
     * @param args   Аргументи команди (без назви підкоманди)
     * @return Список варіантів автодоповнення
     */
    @Nullable
    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * Назва команди
     */
    String getName();

    /**
     * Опис команди
     */
    String getDescription();

    /**
     * Синтаксис використання
     */
    String getUsage();
}
