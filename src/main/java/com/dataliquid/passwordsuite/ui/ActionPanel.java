package com.dataliquid.passwordsuite.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 * Panel containing action controls (dropdowns) for the application.
 */
@SuppressWarnings("PMD.UnusedAssignment")
public final class ActionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * Custom renderer for algorithm dropdown that shows placeholder text for null
     * selection.
     */
    private static final class AlgorithmComboBoxRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final String PLACEHOLDER_TEXT = "-- Select Algorithm --";

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value == null) {
                setText(PLACEHOLDER_TEXT);
                setForeground(Color.GRAY);
            }

            return this;
        }
    }

    private final JComboBox<String> algorithmComboBox;
    private final JComboBox<String> environmentComboBox;
    private boolean suppressAlgorithmEvents;
    private boolean suppressEnvironmentEvents;
    private ActionListener algorithmChangeListener;
    private ActionListener environmentChangeListener;
    private ActionListener algorithmComboBoxListener;
    private ActionListener environmentComboBoxListener;

    public ActionPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createTitledBorder("Actions"));
        setName("actionPanel");

        // Algorithm selection
        JLabel algorithmLabel = new JLabel("Algorithm:");
        algorithmLabel.setName("algorithmLabel");
        add(algorithmLabel);

        algorithmComboBox = new JComboBox<>();
        algorithmComboBox.setName("algorithmComboBox");
        algorithmComboBox.setRenderer(new AlgorithmComboBoxRenderer());
        algorithmComboBox.setToolTipText("Select encryption algorithm");
        add(algorithmComboBox);

        // Environment selection
        add(Box.createHorizontalStrut(10));

        JLabel environmentLabel = new JLabel("Environment:");
        environmentLabel.setName("environmentLabel");
        add(environmentLabel);

        environmentComboBox = new JComboBox<>(new String[] { UIConstants.NO_ENVIRONMENT });
        environmentComboBox.setName("environmentComboBox");
        environmentComboBox.setToolTipText("Select environment for encryption");
        add(environmentComboBox);
    }

    /**
     * Returns the currently selected encryption algorithm.
     */
    public String getSelectedAlgorithm() {
        return (String) algorithmComboBox.getSelectedItem();
    }

    /**
     * Sets the selected algorithm.
     */
    public void setSelectedAlgorithm(String algorithm) {
        if (algorithm != null) {
            suppressAlgorithmEvents = true;
            try {
                algorithmComboBox.setSelectedItem(algorithm);
            } finally {
                suppressAlgorithmEvents = false;
            }
        }
    }

    /**
     * Clears the algorithm selection (sets to null/placeholder).
     */
    public void clearAlgorithmSelection() {
        suppressAlgorithmEvents = true;
        try {
            algorithmComboBox.setSelectedItem(null);
        } finally {
            suppressAlgorithmEvents = false;
        }
    }

    /**
     * Sets the action listener for algorithm selection changes. The listener will
     * not be called during programmatic updates.
     */
    public void setAlgorithmChangeListener(ActionListener listener) {
        this.algorithmChangeListener = listener;
        // Remove old listener to prevent memory leak
        if (algorithmComboBoxListener != null) {
            algorithmComboBox.removeActionListener(algorithmComboBoxListener);
        }
        algorithmComboBoxListener = e -> {
            if (!suppressAlgorithmEvents && algorithmChangeListener != null) {
                algorithmChangeListener.actionPerformed(e);
            }
        };
        algorithmComboBox.addActionListener(algorithmComboBoxListener);
    }

    /**
     * Updates the environment dropdown with a list of environment names.
     */
    public void updateEnvironments(java.util.List<String> environmentNames) {
        suppressEnvironmentEvents = true;
        try {
            environmentComboBox.removeAllItems();

            if (environmentNames == null || environmentNames.isEmpty()) {
                environmentComboBox.addItem(UIConstants.NO_ENVIRONMENT);
            } else {
                for (String envName : environmentNames) {
                    environmentComboBox.addItem(envName);
                }
            }
        } finally {
            suppressEnvironmentEvents = false;
        }
    }

    /**
     * Updates the algorithm dropdown with a list of algorithm names. Adds a null
     * placeholder item and does not auto-select any algorithm.
     */
    public void updateAlgorithms(java.util.List<String> algorithmNames) {
        algorithmComboBox.removeAllItems();

        // Add null placeholder as first item
        algorithmComboBox.addItem(null);

        if (algorithmNames != null && !algorithmNames.isEmpty()) {
            for (String algoName : algorithmNames) {
                algorithmComboBox.addItem(algoName);
            }
        }

        // Set to null (placeholder) by default
        algorithmComboBox.setSelectedItem(null);
    }

    /**
     * Returns the currently selected environment.
     */
    public String getSelectedEnvironment() {
        Object selected = environmentComboBox.getSelectedItem();
        return selected != null ? selected.toString() : null;
    }

    /**
     * Sets the selected environment.
     */
    public void setSelectedEnvironment(String environment) {
        if (environment != null) {
            suppressEnvironmentEvents = true;
            try {
                environmentComboBox.setSelectedItem(environment);
            } finally {
                suppressEnvironmentEvents = false;
            }
        }
    }

    /**
     * Sets the action listener for environment selection changes. The listener will
     * not be called during programmatic updates.
     */
    public void setEnvironmentChangeListener(ActionListener listener) {
        this.environmentChangeListener = listener;
        // Remove old listener to prevent memory leak
        if (environmentComboBoxListener != null) {
            environmentComboBox.removeActionListener(environmentComboBoxListener);
        }
        environmentComboBoxListener = e -> {
            if (!suppressEnvironmentEvents && environmentChangeListener != null) {
                environmentChangeListener.actionPerformed(e);
            }
        };
        environmentComboBox.addActionListener(environmentComboBoxListener);
    }
}
