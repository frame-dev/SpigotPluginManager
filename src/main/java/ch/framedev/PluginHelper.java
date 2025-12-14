package ch.framedev;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import org.yaml.snakeyaml.Yaml;

public class PluginHelper {

    private static final Logger LOGGER = Logger.getLogger(PluginHelper.class.getName());

    /**
     * Reads the plugin.yml file from the given plugin JAR file and returns its contents as a Map.
     *
     * @param pluginFile The plugin JAR file.
     * @return A Map representing the contents of plugin.yml, or null if an error occurs.
     */
    private static Map<String, Object> getPluginYml(File pluginFile) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            ZipEntry entry = jarFile.getEntry("plugin.yml");
            if (entry == null) {
                return null;
            }
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                Yaml yaml = new Yaml();
                return yaml.load(inputStream);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading plugin.yml from " + pluginFile.getName() + ": " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves the name of the plugin from its plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return The name of the plugin, or null if not found.
     */
    public static String getPluginName(File pluginFile) {
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null) {
            return (String) pluginYml.get("name");
        }
        return null;
    }

    /**
     * Retrieves the version of the plugin from its plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return The version of the plugin, or null if not found.
     */
    public static String getPluginVersion(File pluginFile) {
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null) {
            return (String) pluginYml.get("version");
        }
        return null;
    }

    /**
     * Retrieves the description of the plugin from its plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return The description of the plugin, or null if not found.
     */
    public static String getPluginDescription(File pluginFile) {
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null) {
            return (String) pluginYml.get("description");
        }
        return null;
    }

    /**
     * Retrieves the commands defined in the plugin's plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return A Map of command names to their definitions.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getCommands(File pluginFile) {
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null && pluginYml.containsKey("commands")) {
            commands = (Map<String, Object>) pluginYml.get("commands");
        }
        return commands;
    }

    /**
     * Retrieves the authors of the plugin from its plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return A List of authors.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getPluginAuthors(File pluginFile) {
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null && pluginYml.containsKey("authors")) {
            Object authorsObj = pluginYml.get("authors");
            if (authorsObj instanceof List) {
                return (List<String>) authorsObj;
            } else if (authorsObj instanceof String) {
                return List.of((String) authorsObj);
            }
        } else if (pluginYml != null && pluginYml.containsKey("author")) {
            String author = (String) pluginYml.get("author");
            return List.of(author);
        }
        return List.of();
    }

    /**
     * Retrieves the API version of the plugin from its plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return The API version of the plugin, or 0.0 if not found.
     */
    public static double getPluginAPIVersion(File pluginFile) {
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null) {
            return (double) pluginYml.get("api-version");
        }
        return 0.0;
    }

    /**
     * Retrieves the main class of the plugin from its plugin.yml file.
     *
     * @param pluginFile The plugin JAR file.
     * @return The main class of the plugin, or null if not found.
     */
    public static String getPluginMainClass(File pluginFile) {
        Map<String, Object> pluginYml = getPluginYml(pluginFile);
        if (pluginYml != null) {
            return (String) pluginYml.get("main");
        }
        return null;
    }
}