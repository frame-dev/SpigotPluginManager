package ch.framedev;

import ch.framedev.simplejavautils.SimpleJavaUtils;
import ch.framedev.yamlutils.FileConfiguration;

import java.io.File;

public class Main {

    public static FileConfiguration config;
    public static SimpleJavaUtils utils = new SimpleJavaUtils();

    public static void main(String[] args) {

        setupConfig();

        PluginManagerGUI gui = new PluginManagerGUI();
        gui.display();
    }

    private static void setupConfig() {
        config = new FileConfiguration(utils.getFromResourceFile("config.yml", Main.class), new File(
                utils.getFilePath(Main.class), "config.yml")
        );
    }
}