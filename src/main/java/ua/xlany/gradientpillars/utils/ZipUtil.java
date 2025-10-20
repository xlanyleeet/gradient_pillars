package ua.xlany.gradientpillars.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Утиліта для роботи з ZIP-архівами світів
 * Базується на підході BedWars2023
 */
public class ZipUtil {

    /**
     * Запакувати папку світу в ZIP-архів
     */
    public static void zipWorld(File worldFolder, File zipFile) throws IOException {
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            throw new IOException("Папка світу не існує: " + worldFolder);
        }

        // Створити батьківську папку для ZIP якщо не існує
        File parent = zipFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {

            Path worldPath = worldFolder.toPath();

            Files.walkFileTree(worldPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Пропустити файли блокування
                    String fileName = file.getFileName().toString();
                    if (fileName.equals("session.lock") || fileName.equals("uid.dat")) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Додати файл до архіву
                    String zipEntryName = worldPath.relativize(file).toString().replace("\\", "/");
                    zos.putNextEntry(new ZipEntry(zipEntryName));

                    Files.copy(file, zos);
                    zos.closeEntry();

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Пропустити папки блокування
                    String dirName = dir.getFileName().toString();
                    if (dirName.equals("session.lock") || dirName.equals("uid.dat")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    // Додати папку до архіву (якщо це не корінь)
                    if (!dir.equals(worldPath)) {
                        String zipEntryName = worldPath.relativize(dir).toString().replace("\\", "/") + "/";
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        zos.closeEntry();
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Розпакувати ZIP-архів світу в папку
     */
    public static void unzipWorld(File zipFile, File targetFolder) throws IOException {
        if (!zipFile.exists()) {
            throw new IOException("ZIP-файл не існує: " + zipFile);
        }

        // Видалити існуючу папку якщо є
        if (targetFolder.exists()) {
            deleteDirectory(targetFolder);
        }

        // Створити цільову папку
        targetFolder.mkdirs();

        byte[] buffer = new byte[8192];

        try (FileInputStream fis = new FileInputStream(zipFile);
                ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(targetFolder, entry.getName());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    // Створити батьківські папки
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    // Записати файл
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }

    /**
     * Видалити директорію рекурсивно
     */
    private static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
