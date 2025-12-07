package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * Status bar panel displaying file name and environment count.
 */
public final class StatusBar extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String NO_ENVS = "No environments";
    private static final int STATUS_BAR_HEIGHT = 25;
    private static final int SINGLE_ENV = 1;

    private final JLabel fileNameLabel;
    private final JLabel environmentCountLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        setName("statusBar");

        // Left: File name (70%)
        fileNameLabel = new JLabel(UIConstants.NO_FILE);
        fileNameLabel.setName("fileNameLabel");
        fileNameLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Right: Environment count (30%)
        environmentCountLabel = new JLabel(NO_ENVS, SwingConstants.RIGHT);
        environmentCountLabel.setName("environmentCountLabel");
        environmentCountLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Use nested panels for 70/30 split
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(fileNameLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.add(environmentCountLabel, BorderLayout.CENTER);

        // Wrapper for separator + environment count
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setOpaque(false);
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, STATUS_BAR_HEIGHT - 4));
        rightWrapper.add(separator, BorderLayout.WEST);
        rightWrapper.add(rightPanel, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.CENTER);
        add(rightWrapper, BorderLayout.EAST);

        // Set preferred sizes for 70/30 ratio
        setPreferredSize(new Dimension(0, STATUS_BAR_HEIGHT));
    }

    /**
     * Updates the file name display.
     *
     * @param fileName the file name to display, or null for default text
     */
    public void setFileName(String fileName) {
        fileNameLabel.setText(fileName != null && !fileName.isEmpty() ? fileName : UIConstants.NO_FILE);
    }

    /**
     * Updates the environment count display.
     *
     * @param count the number of environments
     */
    public void setEnvironmentCount(int count) {
        if (count == 0) {
            environmentCountLabel.setText(NO_ENVS);
        } else if (count == SINGLE_ENV) {
            environmentCountLabel.setText("1 Env");
        } else {
            environmentCountLabel.setText(count + " Envs");
        }
    }
}
