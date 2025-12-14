package ch.framedev;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class PluginManagerGUI extends JFrame {

    // Currently selected plugin directory (local)
    private File pluginDirectory;

    // Remote support
    private RemoteHelper remoteHelper;
    private boolean remoteMode = false;
    private String remotePluginPath; // e.g. /home/mc/server/plugins
    private String remoteConnectionLabel; // e.g. user@host:/path

    // Suffix for disabled plugins, loaded from config (default: .disabled)
    private static String DISABLED_SUFFIX = Main.config.getString("suffix-for-disabled-plugins", ".disabled");

    private final JLabel selectedDirLabel;
    private final JList<String> installedPluginsList;
    private final JList<String> availablePluginsList;
    private final DefaultListModel<String> availableModel;
    private final DefaultListModel<String> installedModel;

    private final JButton disableButton;
    private final JButton enableButton;
    private final JButton refreshButton;
    private final JButton installButton;
    private final JButton installFromURLButton;
    private final JButton uninstallButton;

    // Plugin info display area
    private final JTextArea infoArea;

    public PluginManagerGUI() {
        setTitle("Spigot Plugin Manager");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        availableModel = new DefaultListModel<>();
        installedModel = new DefaultListModel<>();

        JLabel header = new JLabel("Spigot Plugin Manager", SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        header.setBorder(new EmptyBorder(8, 8, 8, 8));

        this.availablePluginsList = new JList<>(availableModel);
        this.installedPluginsList = new JList<>(installedModel);
        availablePluginsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        installedPluginsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (Main.config.containsKey("plugin-directory") && Main.config.getBoolean("start-with-latest-folder", true)) {
            this.pluginDirectory = new File(Main.config.getString("plugin-directory"));
            if (!this.pluginDirectory.isDirectory()) {
                this.selectedDirLabel = new JLabel("No directory selected", SwingConstants.CENTER);
            } else {
                this.selectedDirLabel = new JLabel("Selected Directory: " + this.pluginDirectory.getAbsolutePath(), SwingConstants.CENTER);
                loadAvailablePlugins();
                loadInstalledPlugins();
            }
        } else {
            this.selectedDirLabel = new JLabel("No directory selected", SwingConstants.CENTER);
        }
        this.selectedDirLabel.setBorder(new EmptyBorder(4, 4, 10, 4));

        // Buttons
        disableButton = new JButton("Disable");
        enableButton = new JButton("Enable");
        refreshButton = new JButton("Refresh");
        installButton = new JButton("Install...");
        installFromURLButton = new JButton("Install from URL...");
        uninstallButton = new JButton("Uninstall");

        // Setup listeners and panels
        setupJMenu();
        setupActions();

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(header, BorderLayout.NORTH);
        topPanel.add(selectedDirLabel, BorderLayout.SOUTH);
        topPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Lists in split pane with titled borders
        JScrollPane availableScroll = new JScrollPane(availablePluginsList);
        availableScroll.setBorder(new TitledBorder("Available Plugins"));
        JScrollPane installedScroll = new JScrollPane(installedPluginsList);
        installedScroll.setBorder(new TitledBorder("Installed Plugins (.jar)"));

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        JScrollPane pluginInfoScroll = new JScrollPane(infoArea);
        pluginInfoScroll.setBorder(new TitledBorder("Plugin Info"));

        // Left split: available and installed lists
        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, availableScroll, installedScroll);
        leftSplit.setResizeWeight(0.5);

        // Main split: lists on left, info on right
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, pluginInfoScroll);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Button bar
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttonBar.add(installButton);
        buttonBar.add(installFromURLButton);
        buttonBar.add(enableButton);
        buttonBar.add(disableButton);
        buttonBar.add(uninstallButton);
        buttonBar.add(refreshButton);

        // Root layout
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);

        // Selection listeners to update button state
        availablePluginsList.addListSelectionListener(this::onSelectionChanged);
        installedPluginsList.addListSelectionListener(this::onSelectionChanged);

        // Initial button state update
        updateButtons();

        // First run welcome message and setup
        if (!Main.config.containsKey("first-run")) {
            Main.config.set("first-run", true);
            Main.config.save();
            JOptionPane.showMessageDialog(this, "Welcome to Spigot Plugin Manager!\nPlease select your plugin directory from the File menu.", "Welcome", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Handle selection changes in plugin lists
     */
    private void onSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            updateButtons();
            updatePluginInfo();
        }
    }

    /**
     * Update the plugin info area based on selection
     */
    private void updatePluginInfo() {
        String selected = availablePluginsList.getSelectedValue();
        if (selected == null) {
            infoArea.setText("");
            return;
        }

        try {
            if (remoteMode) {
                if (remoteHelper == null || !remoteHelper.isConnected() || remotePluginPath == null) {
                    infoArea.setText("Remote not connected");
                    return;
                }
                // download temporarily
                File temp = Files.createTempFile("plugin_", ".jar").toFile();
                try {
                    String remotePath = remotePluginPath.endsWith("/") ? remotePluginPath + selected : remotePluginPath + "/" + selected;
                    remoteHelper.downloadFile(remotePath, temp);
                    writePluginInfoFromFile(temp);
                } finally {
                    if(!temp.delete()) {
                        temp.deleteOnExit();
                    }
                }
            } else {
                if (pluginDirectory == null) {
                    infoArea.setText("");
                    return;
                }
                File pluginFile = new File(pluginDirectory, selected);
                if (pluginFile.exists() && (selected.endsWith(".jar") || selected.endsWith(DISABLED_SUFFIX))) {
                    writePluginInfoFromFile(pluginFile);
                } else {
                    infoArea.setText("Not a plugin file");
                }
            }
        } catch (Exception ex) {
            infoArea.setText("Failed to read plugin info: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void writePluginInfoFromFile(File pluginFile) {
        String name = PluginHelper.getPluginName(pluginFile);
        String version = PluginHelper.getPluginVersion(pluginFile);
        String mainClass = PluginHelper.getPluginMainClass(pluginFile);
        String description = PluginHelper.getPluginDescription(pluginFile);
        Map<String, Object> commands = PluginHelper.getCommands(pluginFile);
        List<String> authors = PluginHelper.getPluginAuthors(pluginFile);
        double apiVersion = PluginHelper.getPluginAPIVersion(pluginFile);
        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("Name: ").append(name != null ? name : "Unknown").append("\n");
        infoBuilder.append("Version: ").append(version != null ? version : "Unknown").append("\n\n");
        infoBuilder.append("Main Class: ").append(mainClass != null ? mainClass : "Unknown").append("\n\n");
        infoBuilder.append("Description:\n").append(description != null ? description : "No description available").append("\n\n");
        infoBuilder.append("API Version: ").append(apiVersion != 0.0 ? apiVersion : "Unknown").append("\n");
        infoBuilder.append("\nCommands:\n");
        if (commands != null && !commands.isEmpty()) {
            for (String cmd : commands.keySet()) {
                infoBuilder.append(" - ").append(cmd).append("\n");
                if (commands.get(cmd) instanceof Map) {
                    Map<String, Object> cmdDetails = (Map<String, Object>) commands.get(cmd);
                    if (cmdDetails.containsKey("description")) {
                        infoBuilder.append("     Description: ").append(cmdDetails.get("description")).append("\n");
                    }
                    if (cmdDetails.containsKey("usage")) {
                        infoBuilder.append("     Usage: ").append(cmdDetails.get("usage")).append("\n");
                    }
                    if (cmdDetails.containsKey("aliases")) {
                        infoBuilder.append("     Aliases: ").append(cmdDetails.get("aliases")).append("\n");
                    }
                    if (cmdDetails.containsKey("permission")) {
                        infoBuilder.append("     Permission: ").append(cmdDetails.get("permission")).append("\n");
                    }
                }
            }
        } else {
            infoBuilder.append("No commands available\n");
        }
        infoBuilder.append("\nAuthors:\n");
        if (!authors.isEmpty()) {
            for (String author : authors) {
                infoBuilder.append(" - ").append(author).append("\n");
            }
        } else {
            infoBuilder.append("No authors available\n");
        }
        infoArea.setText(infoBuilder.toString());
    }

    /**
     * Update button states based on current selection
     */
    private void updateButtons() {
        String avail = (availablePluginsList != null) ? availablePluginsList.getSelectedValue() : null;
        String inst = (installedPluginsList != null) ? installedPluginsList.getSelectedValue() : null;

        if (enableButton != null) enableButton.setEnabled(avail != null && avail.endsWith(DISABLED_SUFFIX));
        if (disableButton != null) disableButton.setEnabled(avail != null && avail.endsWith(".jar"));
        if (uninstallButton != null) uninstallButton.setEnabled(inst != null);
    }

    /**
     * Setup action listeners for buttons
     */
    private void setupActions() {
        installButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (selectedFile != null) {
                    try {
                        if (remoteMode) {
                            if (remoteHelper == null || !remoteHelper.isConnected()) {
                                JOptionPane.showMessageDialog(this, "Not connected to remote.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            remoteHelper.installPlugin(selectedFile, remotePluginPath);
                        } else {
                            if (pluginDirectory == null) {
                                JOptionPane.showMessageDialog(this, "No local plugin directory selected.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            File destFile = new File(pluginDirectory, selectedFile.getName());
                            Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                        loadAvailablePlugins();
                        loadInstalledPlugins();
                        JOptionPane.showMessageDialog(this, "Plugin installed: " + selectedFile.getName());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Failed to install plugin: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        installFromURLButton.addActionListener(e -> {
            String url = JOptionPane.showInputDialog(this, "Enter Plugin URL:");
            if (url != null && !url.trim().isEmpty()) {
                try {
                    // download locally first
                    File temp = Files.createTempFile("download_plugin_", ".jar").toFile();
                    try (InputStream in = new URI(url).toURL().openStream()) {
                        Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (remoteMode) {
                        if (remoteHelper == null || !remoteHelper.isConnected()) {
                            JOptionPane.showMessageDialog(this, "Not connected to remote.", "Error", JOptionPane.ERROR_MESSAGE);
                            if(!temp.delete()) {
                                temp.deleteOnExit();
                            }
                            return;
                        }
                        remoteHelper.installPlugin(temp, remotePluginPath);
                    } else {
                        if (pluginDirectory == null) {
                            JOptionPane.showMessageDialog(this, "No local plugin directory selected.", "Error", JOptionPane.ERROR_MESSAGE);
                            if(!temp.delete()) {
                                temp.deleteOnExit();
                            }
                            return;
                        }
                        String fileName = url.substring(url.lastIndexOf('/') + 1);
                        File destFile = new File(pluginDirectory, fileName);
                        Files.copy(temp.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    if(!temp.delete()) {
                        temp.deleteOnExit();
                    }
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin installed from URL");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to install plugin from URL: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        enableButton.addActionListener(e -> {
            String selected = availablePluginsList.getSelectedValue();
            if (selected != null) {
                try {
                    if (remoteMode) {
                        if (remoteHelper == null || !remoteHelper.isConnected()) {
                            JOptionPane.showMessageDialog(this, "Not connected to remote.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (!selected.endsWith(DISABLED_SUFFIX)) return;
                        remoteHelper.enablePlugin(remotePluginPath, selected, DISABLED_SUFFIX);
                    } else {
                        if (pluginDirectory == null) return;
                        if (!selected.endsWith(DISABLED_SUFFIX)) return;
                        File disabledFile = new File(pluginDirectory, selected);
                        String restoredName = selected.replaceFirst(DISABLED_SUFFIX + "$", "");
                        File pluginFile = new File(pluginDirectory, restoredName);
                        if (!disabledFile.renameTo(pluginFile)) throw new Exception("rename failed");
                    }
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin enabled: " + selected);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to enable plugin: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        disableButton.addActionListener(e -> {
            String selected = availablePluginsList.getSelectedValue();
            if (selected != null) {
                try {
                    if (remoteMode) {
                        if (remoteHelper == null || !remoteHelper.isConnected()) {
                            JOptionPane.showMessageDialog(this, "Not connected to remote.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (!selected.endsWith(".jar")) return;
                        remoteHelper.disablePlugin(remotePluginPath, selected, DISABLED_SUFFIX);
                    } else {
                        if (pluginDirectory == null) return;
                        if (!selected.endsWith(".jar")) return;
                        File pluginFile = new File(pluginDirectory, selected);
                        File disabledFile = new File(pluginDirectory, selected + DISABLED_SUFFIX);
                        if (!pluginFile.renameTo(disabledFile)) throw new Exception("rename failed");
                    }
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin disabled: " + selected);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to disable plugin: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        uninstallButton.addActionListener(e -> {
            String selected = installedPluginsList.getSelectedValue();
            if (selected != null) {
                try {
                    if (remoteMode) {
                        if (remoteHelper == null || !remoteHelper.isConnected()) {
                            JOptionPane.showMessageDialog(this, "Not connected to remote.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        remoteHelper.uninstallPlugin(remotePluginPath, selected);
                    } else {
                        if (pluginDirectory == null) return;
                        File pluginFile = new File(pluginDirectory, selected);
                        if (!pluginFile.delete()) throw new Exception("delete failed");
                    }
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    JOptionPane.showMessageDialog(this, "Plugin uninstalled: " + selected);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to uninstall plugin: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        refreshButton.addActionListener(e -> {
            loadAvailablePlugins();
            loadInstalledPlugins();
        });
    }

    /**
     * Set up the menu bar
     */
    private void setupJMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem selectItem = new JMenuItem("Select Plugin Folder ...");
        JMenuItem connectRemoteItem = new JMenuItem("Connect Remote...");
        JMenuItem disconnectRemoteItem = new JMenuItem("Disconnect Remote");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        selectItem.addActionListener(listener -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = fileChooser.getSelectedFile();
                if (selectedDirectory != null && selectedDirectory.isDirectory()) {
                    // turn off remote mode when selecting local folder
                    remoteMode = false;
                    if (remoteHelper != null) remoteHelper.disconnect();
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

        connectRemoteItem.addActionListener(e -> {
            String lastHost = Main.config.getString("last-remote-host", "");
            int lastPort = Main.config.getInt("last-remote-port", 22);
            String lastUser = Main.config.getString("last-remote-username", "");
            String lastPath = Main.config.getString("last-remote-plugin-path", "/plugins");
            JTextField hostField = new JTextField(lastHost);
            JTextField portField = new JTextField(String.valueOf(lastPort));
            JTextField userField = new JTextField(lastUser);
            JPasswordField passField = new JPasswordField();
            JTextField pathField = new JTextField(lastPath);
            Object[] inputs = {
                    "Host:", hostField,
                    "Port:", portField,
                    "Username:", userField,
                    "Password:", passField,
                    "Remote plugin path:", pathField
            };
            int result = JOptionPane.showConfirmDialog(this, inputs, "Connect Remote", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    String host = hostField.getText().trim();
                    int port = Integer.parseInt(portField.getText().trim());
                    String user = userField.getText().trim();
                    String pass = new String(passField.getPassword());
                    String path = pathField.getText().trim();
                    if (host.isEmpty() || user.isEmpty() || path.isEmpty())
                        throw new IllegalArgumentException("Missing fields");

                    if (remoteHelper != null) remoteHelper.disconnect();
                    remoteHelper = new RemoteHelper();
                    remoteHelper.connect(host, port, user, pass, 10000);
                    remoteMode = true;
                    remotePluginPath = path;
                    remoteConnectionLabel = user + "@" + host + ":" + path;
                    this.selectedDirLabel.setText("Remote: " + remoteConnectionLabel);
                    loadAvailablePlugins();
                    loadInstalledPlugins();
                    Main.config.set("last-remote-host", host);
                    Main.config.set("last-remote-port", port);
                    Main.config.set("last-remote-username", user);
                    Main.config.set("last-remote-plugin-path", path);
                    Main.config.save();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    remoteMode = false;
                    if (remoteHelper != null) remoteHelper.disconnect();
                    remoteHelper = null;
                }
            }
        });

        disconnectRemoteItem.addActionListener(e -> {
            if (remoteHelper != null) {
                remoteHelper.disconnect();
                remoteHelper = null;
            }
            remoteMode = false;
            remotePluginPath = null;
            remoteConnectionLabel = null;
            this.selectedDirLabel.setText("No directory selected");
            availableModel.clear();
            installedModel.clear();
        });

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Spigot Plugin Manager\nVersion 1.2-SNAPSHOT\nDeveloped by FrameDev", "About", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "To use this application, select your Spigot plugin directory from the File menu or connect to a remote server.\nYou can install, uninstall, enable, and disable plugins using the provided buttons.", "Help", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> {
            SettingsGUI settingsGUI = new SettingsGUI();
            settingsGUI.display();
        });

        fileMenu.add(selectItem);
        fileMenu.add(connectRemoteItem);
        fileMenu.add(disconnectRemoteItem);
        fileMenu.add(helpItem);
        fileMenu.add(aboutItem);
        fileMenu.addSeparator();
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void loadAvailablePlugins() {
        DISABLED_SUFFIX = Main.config.getString("suffix-for-disabled-plugins", ".disabled");
        availableModel.clear();
        try {
            if (remoteMode) {
                if (remoteHelper == null || !remoteHelper.isConnected() || remotePluginPath == null) return;
                for (String name : remoteHelper.listPlugins(remotePluginPath)) {
                    availableModel.addElement(name);
                }
            } else {
                if (pluginDirectory == null) return;
                File[] files = pluginDirectory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory())
                            availableModel.addElement(file.getName());
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to list available plugins: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateButtons();
    }

    private void loadInstalledPlugins() {
        installedModel.clear();
        try {
            if (remoteMode) {
                if (remoteHelper == null || !remoteHelper.isConnected() || remotePluginPath == null) return;
                for (String name : remoteHelper.listPlugins(remotePluginPath)) {
                    if (name.endsWith(".jar")) installedModel.addElement(name);
                }
            } else {
                if (pluginDirectory == null) return;
                File[] files = pluginDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory())
                            installedModel.addElement(file.getName());
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to list installed plugins: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateButtons();
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}