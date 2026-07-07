package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.dataliquid.passwordsuite.crypto.config.AlgorithmConfig;
import com.dataliquid.passwordsuite.crypto.factory.CipherRegistry;
import com.dataliquid.passwordsuite.environment.EnvironmentConfig;

/**
 * Dialog for configuring multiple environments with master passwords and format
 * settings.
 * <p>
 * Supports adding, editing, and deleting environments.
 * </p>
 */
public final class EnvironmentsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JComboBox<String> environmentComboBox;
    private final JTextField environmentNameField;
    private final JPasswordField masterPasswordField;
    private final JTextField formatPrefixField;
    private final JTextField formatSuffixField;
    private final JTextField patternField;
    private final JComboBox<String> algorithmComboBox;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton saveButton;

    private final Map<String, EnvironmentConfig> environments;
    private final List<String> availableAlgorithms;
    private final transient CipherRegistry cipherRegistry;
    private boolean confirmed;

    /**
     * Creates a new EnvironmentsDialog with existing environments map.
     *
     * @param parent               the parent frame
     * @param existingEnvironments map of environment names to configurations
     * @param activeEnvironment    the currently active environment name
     * @param cipherRegistry       the cipher registry for algorithm metadata
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public EnvironmentsDialog(Frame parent, Map<String, EnvironmentConfig> existingEnvironments,
            String activeEnvironment, CipherRegistry cipherRegistry) {
        super(parent, "Environments", true);

        this.cipherRegistry = cipherRegistry;
        this.availableAlgorithms = cipherRegistry != null ? cipherRegistry.getAvailableAlgorithms() : List.of();

        setLayout(new BorderLayout(10, 10));
        setSize(500, 470);
        setLocationRelativeTo(parent);

        // Deep copy existing environments
        this.environments = new ConcurrentHashMap<>();
        if (existingEnvironments != null) {
            for (Map.Entry<String, EnvironmentConfig> entry : existingEnvironments.entrySet()) {
                EnvironmentConfig config = entry.getValue();
                EnvironmentConfig configCopy = new EnvironmentConfig(config.getPassword(), config.getFormatPrefix(),
                        config.getFormatSuffix(), config.getDefaultAlgorithm(), config.getPattern());
                this.environments.put(entry.getKey(), configCopy);
            }
        }

        // Top panel - Environment selection with Edit and Delete buttons
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Select Environment"));

        JLabel envSelectLabel = new JLabel("Environment:");
        envSelectLabel.setName("envSelectLabel");
        selectionPanel.add(envSelectLabel);

        environmentComboBox = new JComboBox<>();
        environmentComboBox.setName("environmentComboBox");
        environmentComboBox.setPreferredSize(new Dimension(200, 25));
        environmentComboBox.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                updateButtonStates();
            }
        });
        selectionPanel.add(environmentComboBox);

        editButton = new JButton("Edit");
        editButton.setName("editButton");
        editButton.setMnemonic('E');
        editButton.addActionListener(e -> handleEdit());
        selectionPanel.add(editButton);

        deleteButton = new JButton("Delete");
        deleteButton.setName("deleteButton");
        deleteButton.setMnemonic('D');
        deleteButton.addActionListener(e -> handleDelete());
        selectionPanel.add(deleteButton);

        add(selectionPanel, BorderLayout.NORTH);

        // Center panel - Form fields and Save button
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Environment Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel envNameLabel = new JLabel("Environment Name:");
        envNameLabel.setName("envNameLabel");
        formPanel.add(envNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        environmentNameField = new JTextField(20);
        environmentNameField.setName("environmentNameField");
        environmentNameField.setToolTipText("e.g., production, staging, development");
        formPanel.add(environmentNameField, gbc);

        // Master Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel passwordLabel = new JLabel("Master Password:");
        passwordLabel.setName("passwordLabel");
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        masterPasswordField = new JPasswordField(20);
        masterPasswordField.setName("masterPasswordField");
        masterPasswordField.setToolTipText("Password for encryption/decryption operations");
        masterPasswordField.addActionListener(e -> handleSave()); // Enter triggers Save (KeePass Auto-Type)
        formPanel.add(masterPasswordField, gbc);

        // Default Algorithm field (moved here, before format fields)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel algoLabel = new JLabel("Default Algorithm:");
        algoLabel.setName("algoLabel");
        formPanel.add(algoLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        algorithmComboBox = new JComboBox<>();
        algorithmComboBox.setName("algorithmComboBox");
        for (String algo : this.availableAlgorithms) {
            algorithmComboBox.addItem(algo);
        }
        algorithmComboBox.setSelectedItem(EnvironmentConfig.DEFAULT_ALGORITHM);
        algorithmComboBox.setToolTipText("Default encryption algorithm for this environment");
        formPanel.add(algorithmComboBox, gbc);

        // Format Prefix field
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        JLabel prefixLabel = new JLabel("Format Prefix:");
        prefixLabel.setName("prefixLabel");
        formPanel.add(prefixLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formatPrefixField = new JTextField(EnvironmentConfig.DEFAULT_FORMAT_PREFIX, 20);
        formatPrefixField.setName("formatPrefixField");
        formatPrefixField.setToolTipText("Prefix for encrypted values (e.g., ENC[, SEC[)");
        formPanel.add(formatPrefixField, gbc);

        // Format Suffix field
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        JLabel suffixLabel = new JLabel("Format Suffix:");
        suffixLabel.setName("suffixLabel");
        formPanel.add(suffixLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formatSuffixField = new JTextField(EnvironmentConfig.DEFAULT_FORMAT_SUFFIX, 20);
        formatSuffixField.setName("formatSuffixField");
        formatSuffixField.setToolTipText("Suffix for encrypted values (e.g., ], })");
        formPanel.add(formatSuffixField, gbc);

        // Filename Pattern field
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        JLabel patternLabel = new JLabel("Filename Pattern:");
        patternLabel.setName("patternLabel");
        patternLabel.setToolTipText("Optional regex pattern to auto-detect this environment");
        formPanel.add(patternLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        patternField = new JTextField(20);
        patternField.setName("patternField");
        patternField.setToolTipText("Example: .*-prod\\.properties or .*/production/.*");
        formPanel.add(patternField, gbc);

        // ItemListener: Update format fields when algorithm changes
        algorithmComboBox.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                updateFormatFieldsFromAlgorithm();
            }
        });

        centerPanel.add(formPanel, BorderLayout.NORTH);

        // Save button panel
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        saveButton = new JButton("Save");
        saveButton.setName("saveButton");
        saveButton.setMnemonic('S');
        saveButton.addActionListener(e -> handleSave());
        savePanel.add(saveButton);

        centerPanel.add(savePanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - OK/Cancel buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.setName("okButton");
        okButton.setMnemonic('O');
        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setName("cancelButton");
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set default button
        getRootPane().setDefaultButton(okButton);

        // ESC key closes dialog (KeePass Auto-Type support)
        getRootPane()
                .registerKeyboardAction(e -> dispose(),
                        javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                        javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Initialize combo box with existing environments
        refreshComboBox();

        // Select active environment if exists
        if (activeEnvironment != null && environments.containsKey(activeEnvironment)) {
            environmentComboBox.setSelectedItem(activeEnvironment);
            loadSelectedEnvironment();
        } else if (environmentComboBox.getItemCount() > 0) {
            environmentComboBox.setSelectedIndex(0);
            loadSelectedEnvironment();
        }

        // Update button states after selection is complete
        updateButtonStates();

        // Set initial focus on environment name field (KeePass Auto-Type support)
        javax.swing.SwingUtilities.invokeLater(() -> environmentNameField.requestFocusInWindow());
    }

    /**
     * Refreshes the combo box with current environments.
     */
    private void refreshComboBox() {
        String currentSelection = (String) environmentComboBox.getSelectedItem();
        environmentComboBox.removeAllItems();

        for (String envName : environments.keySet()) {
            environmentComboBox.addItem(envName);
        }

        // Restore selection if still exists
        if (currentSelection != null && environments.containsKey(currentSelection)) {
            environmentComboBox.setSelectedItem(currentSelection);
        } else if (environmentComboBox.getItemCount() > 0) {
            // Auto-select first item if no previous selection exists
            environmentComboBox.setSelectedIndex(0);
        }
    }

    /**
     * Loads the selected environment into the form fields.
     */
    private void loadSelectedEnvironment() {
        String selectedEnv = (String) environmentComboBox.getSelectedItem();
        if (selectedEnv != null && environments.containsKey(selectedEnv)) {
            EnvironmentConfig config = environments.get(selectedEnv);
            environmentNameField.setText(selectedEnv);
            masterPasswordField.setText(new String(config.getPassword()));
            formatPrefixField.setText(config.getFormatPrefix());
            formatSuffixField.setText(config.getFormatSuffix());
            algorithmComboBox.setSelectedItem(config.getDefaultAlgorithm());
            patternField.setText(config.getPattern() != null ? config.getPattern() : "");
        } else {
            environmentNameField.setText("");
            masterPasswordField.setText("");
            formatPrefixField.setText(EnvironmentConfig.DEFAULT_FORMAT_PREFIX);
            formatSuffixField.setText(EnvironmentConfig.DEFAULT_FORMAT_SUFFIX);
            algorithmComboBox.setSelectedItem(EnvironmentConfig.DEFAULT_ALGORITHM);
            patternField.setText("");
        }
    }

    /**
     * Handles Edit button - loads selected environment into fields.
     */
    private void handleEdit() {
        loadSelectedEnvironment();
        environmentNameField.requestFocus();
    }

    /**
     * Updates format prefix/suffix fields based on selected algorithm's defaults.
     */
    private void updateFormatFieldsFromAlgorithm() {
        String selectedAlgo = (String) algorithmComboBox.getSelectedItem();
        if (selectedAlgo != null && cipherRegistry != null && cipherRegistry.hasAlgorithm(selectedAlgo)) {
            AlgorithmConfig config = cipherRegistry.getAlgorithm(selectedAlgo);
            formatPrefixField.setText(config.getDefaultFormatPrefix());
            formatSuffixField.setText(config.getDefaultFormatSuffix());
        }
    }

    /**
     * Handles Save button - saves values from fields as new or updated environment.
     */
    private void handleSave() {
        String newName = environmentNameField.getText().trim();
        char[] newPassword = masterPasswordField.getPassword();
        String prefix = formatPrefixField.getText();
        String suffix = formatSuffixField.getText();
        String algorithm = (String) algorithmComboBox.getSelectedItem();
        String pattern = patternField.getText().trim();

        if (!pattern.isEmpty()) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                JOptionPane
                        .showMessageDialog(this, "Invalid filename pattern: " + e.getDescription(), "Validation Error",
                                JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (newName.isEmpty()) {
            JOptionPane
                    .showMessageDialog(this, "Environment name cannot be empty!", "Validation Error",
                            JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate password length for algorithms with constraints (e.g., MuleSoft)
        if (algorithm != null && cipherRegistry != null && cipherRegistry.hasAlgorithm(algorithm)) {
            AlgorithmConfig algoConfig = cipherRegistry.getAlgorithm(algorithm);
            int pwLength = newPassword.length;
            if (!algoConfig.isValidPasswordLength(pwLength)) {
                JOptionPane
                        .showMessageDialog(this,
                                "Password for " + algorithm + " must be " + algoConfig.getPasswordRequirement()
                                        + " characters.\nCurrent length: " + pwLength,
                                "Password Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Simple logic: Add if doesn't exist, Update if exists
        if (environments.containsKey(newName)) {
            // Update existing: Clean up old config
            environments.get(newName).cleanup();
        }

        // Save environment with full configuration
        EnvironmentConfig config = new EnvironmentConfig(newPassword, prefix, suffix, algorithm,
                pattern.isEmpty() ? null : pattern);
        environments.put(newName, config);
        refreshComboBox();

        // Select the saved environment
        environmentComboBox.setSelectedItem(newName);
        updateButtonStates();

        // Clear fields for next entry
        environmentNameField.setText("");
        masterPasswordField.setText("");
        formatPrefixField.setText(EnvironmentConfig.DEFAULT_FORMAT_PREFIX);
        formatSuffixField.setText(EnvironmentConfig.DEFAULT_FORMAT_SUFFIX);
        algorithmComboBox.setSelectedItem(EnvironmentConfig.DEFAULT_ALGORITHM);
        patternField.setText("");
        environmentNameField.requestFocus();
    }

    /**
     * Handles deleting the selected environment.
     */
    private void handleDelete() {
        String selectedEnv = (String) environmentComboBox.getSelectedItem();
        if (selectedEnv == null) {
            return;
        }

        int result = JOptionPane
                .showConfirmDialog(this, "Are you sure you want to delete environment '" + selectedEnv + "'?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // Clear password from memory
            EnvironmentConfig config = environments.get(selectedEnv);
            if (config != null) {
                config.cleanup();
            }

            environments.remove(selectedEnv);
            refreshComboBox();

            if (environmentComboBox.getItemCount() > 0) {
                environmentComboBox.setSelectedIndex(0);
                loadSelectedEnvironment();
            } else {
                environmentNameField.setText("");
                masterPasswordField.setText("");
                formatPrefixField.setText(EnvironmentConfig.DEFAULT_FORMAT_PREFIX);
                formatSuffixField.setText(EnvironmentConfig.DEFAULT_FORMAT_SUFFIX);
                algorithmComboBox.setSelectedItem(EnvironmentConfig.DEFAULT_ALGORITHM);
            }
        }

        updateButtonStates();
    }

    /**
     * Updates button enabled/disabled states based on selection.
     */
    private void updateButtonStates() {
        boolean hasSelection = environmentComboBox.getSelectedItem() != null;
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }

    /**
     * Returns true if user clicked OK.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Returns the map of all environments with their configurations.
     */
    public Map<String, EnvironmentConfig> getEnvironments() {
        return environments;
    }

    /**
     * Returns the currently selected environment name.
     */
    public String getSelectedEnvironment() {
        return (String) environmentComboBox.getSelectedItem();
    }

    /**
     * Disposes the dialog and clears sensitive data from memory.
     */
    @Override
    public void dispose() {
        // Clear password field from memory
        char[] password = masterPasswordField.getPassword();
        java.util.Arrays.fill(password, '\0');
        masterPasswordField.setText("");
        super.dispose();
    }
}
