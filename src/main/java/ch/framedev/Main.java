package ch.framedev;

import ch.framedev.simplejavautils.SimpleJavaUtils;
import ch.framedev.yamlutils.FileConfiguration;

import java.awt.*;
import java.io.File;

public class Main {

    public static FileConfiguration config;
    public static SimpleJavaUtils utils = new SimpleJavaUtils();

    public static void main(String[] args) {
        setupConfig();

        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported on this system.");
            return;
        }

        PluginManagerGUI gui = new PluginManagerGUI();
        gui.display();
    }

    private static void setupConfig() {
        config = new FileConfiguration(utils.getFromResourceFile("config.yml", Main.class), new File(
                utils.getFilePath(Main.class), "config.yml")
        );
    }
}