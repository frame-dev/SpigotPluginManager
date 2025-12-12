package ch.framedev;

import ch.framedev.yamlutils.FileConfiguration;

import javax.swing.*;
import java.awt.*;

public class SettingsGUI extends JFrame {

    private final FileConfiguration config = Main.config;

    public SettingsGUI() {
        setTitle("Spigot Plugin Manager Settings");
        setSize(450, 220);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Start with latest folder
        JLabel startWithLatestFolderLabel = new JLabel("Start with latest folder:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(startWithLatestFolderLabel, gbc);

        JCheckBox startWithLatestFolderCheckbox = new JCheckBox();
        startWithLatestFolderCheckbox.setSelected(config.getBoolean("start-with-latest-folder", true));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panel.add(startWithLatestFolderCheckbox, gbc);

        // Suffix for disabled plugins
        JLabel suffixForDisabledPluginsLabel = new JLabel("Suffix for disabled plugins:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(suffixForDisabledPluginsLabel, gbc);

        JTextField suffixForDisabledPluginsField = new JTextField(config.getString("suffix-for-disabled-plugins", ".disabled"));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        panel.add(suffixForDisabledPluginsField, gbc);

        // Save and Cancel buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);
        buttonRow.setOpaque(false);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buttonRow, gbc);

        saveButton.addActionListener(e -> {
            config.set("start-with-latest-folder", startWithLatestFolderCheckbox.isSelected());
            String suffix = suffixForDisabledPluginsField.getText().trim();
            if (suffix.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Suffix for disabled plugins cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (suffix.contains(" ")) {
                JOptionPane.showMessageDialog(this, "Suffix for disabled plugins cannot contain spaces.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!suffix.startsWith(".")) {
                suffix = "." + suffix;
            }
            suffixForDisabledPluginsField.setText(suffix);
            config.set("suffix-for-disabled-plugins", suffix);
            config.save();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!");
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        setContentPane(panel);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}