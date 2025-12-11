package ch.framedev;

import javax.swing.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PluginManagerGUI extends JFrame {

    private File pluginDirectory;

    private final JLabel selectedDirLabel;
    private final JList<String> installedPluginsList;
    private final JList<String> availablePluginsList;
    private final DefaultListModel<String> availableModel;
    private final DefaultListModel<String> installedModel;

    public PluginManagerGUI() {
        setTitle("Spigot Plugin Manager");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        availableModel = new DefaultListModel<>();
        installedModel = new DefaultListModel<>();

        JLabel label = new JLabel("Welcome to Spigot Plugin Manager", SwingConstants.CENTER);
        if (Main.config.containsKey("plugin-directory")) {
            this.pluginDirectory = new File(Main.config.getString("plugin-directory"));
            if (this.pluginDirectory.isDirectory()) {
                this.selectedDirLabel = new JLabel("Selected Directory: " + this.pluginDirectory.getAbsolutePath(), SwingConstants.CENTER);
                loadAvailablePlugins();
                loadInstalledPlugins();
            } else {
                this.selectedDirLabel = new JLabel("No directory selected", SwingConstants.CENTER);
            }
        } else
            this.selectedDirLabel = new JLabel("No directory selected", SwingConstants.CENTER);

        this.availablePluginsList = new JList<>(availableModel);
        this.installedPluginsList = new JList<>(installedModel);

        setupJMenu();

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(label);
        add(selectedDirLabel);
        add(new JLabel("Available Plugins:"));
        add(new JScrollPane(availablePluginsList));
        add(new JLabel("Installed Plugins (.jar):"));
        add(new JScrollPane(installedPluginsList));

        JButton disableButton = getDisableButton();
        add(disableButton);

        JButton enableButton = getEnableButton();
        add(enableButton);
        JButton refreshButton = new JButton("Refresh Plugin Lists");
        refreshButton.addActionListener(e -> {
            loadAvailablePlugins();
            loadInstalledPlugins();
        });
        add(refreshButton);
        JButton installButton = getInstallButton();
        add(installButton);
        JButton installFromURLButton = getInstallFromURLButton();
        add(installFromURLButton);
        JButton uninstallButton = getUninstallButton();
        add(uninstallButton);

        if(!Main.config.containsKey("first-run")) {
            Main.config.set("first-run", true);
            Main.config.save();
            JOptionPane.showMessageDialog(this, "Welcome to Spigot Plugin Manager!\nPlease select your plugin directory from the File menu.", "Welcome", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JButton getUninstallButton() {
        JButton uninstallButton = new JButton("Uninstall Selected Plugin");
        uninstallButton.addActionListener(e -> {
            String selected = installedModel.getSize() > 0 ? installedModel.getElementAt(installedPluginsList.getSelectedIndex()) : null;
            if (selected != null && pluginDirectory != null) {
                File pluginFile = new File(pluginDirectory, selected);
                if (pluginFile.delete()) {
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin uninstalled: " + selected);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to uninstall plugin: " + selected, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return uninstallButton;
    }

    private JButton getInstallFromURLButton() {
        JButton installFromURLButton = new JButton("Install Plugin from URL");
        installFromURLButton.addActionListener(e -> {
            String url = JOptionPane.showInputDialog(this, "Enter P1lugin URL:");
            if (url != null && !url.trim().isEmpty() && pluginDirectory != null) {
                try (java.io.InputStream in = new URI(url).toURL().openStream()) {
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    File destFile = new File(pluginDirectory, fileName);
                    Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin installed from URL: " + fileName);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to install plugin from URL.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return installFromURLButton;
    }

    private JButton getInstallButton() {
        JButton installButton = new JButton("Install Plugin");
        installButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (selectedFile != null && pluginDirectory != null) {
                    File destFile = new File(pluginDirectory, selectedFile.getName());
                    try {
                        Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        loadAvailablePlugins();
                        loadInstalledPlugins();
                        JOptionPane.showMessageDialog(this, "Plugin installed: " + selectedFile.getName());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Failed to install plugin: " + selectedFile.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        return installButton;
    }

    private JButton getEnableButton() {
        JButton enableButton = new JButton("Enable Selected Plugin");
        enableButton.addActionListener(e -> {
            String selected = availablePluginsList.getSelectedValue();
            if (selected != null && pluginDirectory != null && selected.endsWith(".disabled")) {
                File disabledFile = new File(pluginDirectory, selected);
                String restoredName = selected.replaceFirst("\\.disabled$", "");
                File pluginFile = new File(pluginDirectory, restoredName);
                if (disabledFile.renameTo(pluginFile)) {
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin enabled: " + restoredName);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to enable plugin: " + selected, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return enableButton;
    }

    private JButton getDisableButton() {
        JButton disableButton = new JButton("Disable Selected Plugin");
        disableButton.addActionListener(e -> {
            String selected = availablePluginsList.getSelectedValue();
            if (selected != null && pluginDirectory != null && selected.endsWith(".jar")) {
                File pluginFile = new File(pluginDirectory, selected);
                File disabledFile = new File(pluginDirectory, selected + ".disabled");
                if (pluginFile.renameTo(disabledFile)) {
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin disabled: " + selected);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to disable plugin: " + selected, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return disableButton;
    }

    private void setupJMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem selectItem = new JMenuItem("Select Plugin Folders");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        selectItem.addActionListener(listener -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = fileChooser.getSelectedFile();
                if (selectedDirectory != null && selectedDirectory.isDirectory()) {
                    this.pluginDirectory = selectedDirectory;
                    this.selectedDirLabel.setText("Selected Directory: " + selectedDirectory.getAbsolutePath());
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    Main.config.set("plugin-directory", selectedDirectory.getAbsolutePath());
                    Main.config.save();
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a valid directory.", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        fileMenu.add(selectItem);
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void loadAvailablePlugins() {
        availableModel.clear();
        if (pluginDirectory == null) return;
        File[] files = pluginDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory())
                    availableModel.addElement(file.getName());
            }
        }
    }

    private void loadInstalledPlugins() {
        installedModel.clear();
        if (pluginDirectory == null) return;
        File[] files = pluginDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory())
                    installedModel.addElement(file.getName());
            }
        }
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}