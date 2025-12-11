package ch.framedev;

import ch.framedev.simplejavautils.SimpleJavaUtils;
import ch.framedev.yamlutils.FileConfiguration;

import java.awt.*;
import java.io.File;

public class Main {

    public static FileConfiguration config;
    public static SimpleJavaUtils utils = new SimpleJavaUtils();

    public static void main(String[] args) {
        // Setup configuration
        setupConfig();

        // Check if Desktop is supported
        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported on this system.");
            return;
        }

        // Launch the Plugin Manager GUI
        PluginManagerGUI gui = new PluginManagerGUI();
        gui.display();
    }

    /**
     * Sets up the configuration by loading it from the resource file and saving it to the local file system.
     */
    private static void setupConfig() {
        config = new FileConfiguration(utils.getFromResourceFile("config.yml", Main.class), new File(
                utils.getFilePath(Main.class), "config.yml")
        );
    }
}