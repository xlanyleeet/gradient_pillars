package ua.xlany.gradientpillars.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import ua.xlany.gradientpillars.GradientPillars;

public class LibraryUtil {

    private final GradientPillars plugin;
    private final File libsFolder;
    private URLClassLoader libraryClassLoader;

    public LibraryUtil(GradientPillars plugin) {
        this.plugin = plugin;
        this.libsFolder = new File(plugin.getDataFolder(), "libs");
    }

    public void loadLibraries() {
        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
        }

        Map<String, String> libraries = new HashMap<>();
        // SQLite
        libraries.put("sqlite-jdbc-3.45.1.0.jar",
                "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar");
        // MariaDB
        libraries.put("mariadb-java-client-3.3.2.jar",
                "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.3.2/mariadb-java-client-3.3.2.jar");

        java.util.List<URL> urls = new java.util.ArrayList<>();

        for (Map.Entry<String, String> entry : libraries.entrySet()) {
            File libraryFile = new File(libsFolder, entry.getKey());
            if (!libraryFile.exists()) {
                downloadLibrary(entry.getValue(), libraryFile);
            }
            try {
                urls.add(libraryFile.toURI().toURL());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to resolve library URL: " + libraryFile.getName(), e);
            }
        }

        // Create a dedicated ClassLoader for these libraries
        this.libraryClassLoader = new URLClassLoader(urls.toArray(new URL[0]), plugin.getClass().getClassLoader());
        plugin.getLogger().info("Libraries initialized in separate ClassLoader.");
    }

    public ClassLoader getClassLoader() {
        return libraryClassLoader;
    }

    private void downloadLibrary(String urlString, File destination) {
        plugin.getLogger().info("Downloading library: " + destination.getName());
        try (BufferedInputStream in = new BufferedInputStream(URI.create(urlString).toURL().openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            plugin.getLogger().info("Downloaded " + destination.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download library: " + destination.getName(), e);
        }
    }
}
