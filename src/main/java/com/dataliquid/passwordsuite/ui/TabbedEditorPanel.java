package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * Panel containing a tabbed editor for managing multiple open files. Each tab
 * contains a FileTab instance with its own editor and configuration tree.
 */
public final class TabbedEditorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Client property key linking a tab's scroll pane to its FileTab. */
    private static final String FILE_TAB_PROPERTY = "passwordsuite.fileTab";
    /** Client property key storing the stable tab id (used for component names). */
    private static final String TAB_ID_PROPERTY = "passwordsuite.tabId";

    private final JTabbedPane tabbedPane;
    private int nextTabId;

    public TabbedEditorPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Editor"));

        tabbedPane = new JTabbedPane();
        tabbedPane.setName("editorTabbedPane");
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Adds a new empty tab.
     */
    public FileTab addNewTab() {
        int tabId = nextTabId;
        nextTabId++;
        FileTab fileTab = new FileTab();

        JComponent scrollPane = fileTab.getScrollPane();
        scrollPane.putClientProperty(FILE_TAB_PROPERTY, fileTab);
        scrollPane.putClientProperty(TAB_ID_PROPERTY, tabId);

        // Add tab with close button
        int index = tabbedPane.getTabCount();
        tabbedPane.addTab(fileTab.getTabTitle(), scrollPane);
        tabbedPane.setTabComponentAt(index, createTabComponent(tabId, fileTab));

        // Select the new tab
        tabbedPane.setSelectedIndex(index);

        return fileTab;
    }

    /**
     * Creates a tab component with title and close button.
     */
    private JPanel createTabComponent(int tabId, FileTab fileTab) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setName("tabComponent-" + tabId);
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(fileTab.getTabTitle());
        titleLabel.setName("tabTitle-" + tabId);
        panel.add(titleLabel);

        // Close button
        JButton closeButton = new JButton("×");
        closeButton.setName("closeTab-" + tabId);
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setFocusable(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(e -> closeTab(fileTab));

        panel.add(closeButton);

        return panel;
    }

    /**
     * Closes the tab containing the given FileTab.
     */
    private void closeTab(FileTab fileTab) {
        // Check if modified
        if (fileTab.isModified()) {
            int result = JOptionPane
                    .showConfirmDialog(this,
                            "The file \"" + fileTab.getTabTitle()
                                    + "\" has unsaved changes.\nDo you want to save before closing?",
                            "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.CANCEL_OPTION) {
                return; // Don't close
            }
            // Note: YES_OPTION proceeds without saving - user has been warned and confirmed
        }

        int index = tabbedPane.indexOfComponent(fileTab.getScrollPane());
        if (index >= 0) {
            // Cleanup resources before removing
            fileTab.cleanup();
            tabbedPane.removeTabAt(index);
        }

        // If no tabs left, create a new empty one
        if (tabbedPane.getTabCount() == 0) {
            addNewTab();
        }
    }

    /**
     * Returns the currently active FileTab, or null if none.
     */
    public FileTab getActiveTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return null;
        }

        return getFileTabAt(selectedIndex);
    }

    /**
     * Returns the FileTab at the specified tab index.
     */
    private FileTab getFileTabAt(int index) {
        return (FileTab) ((JComponent) tabbedPane.getComponentAt(index)).getClientProperty(FILE_TAB_PROPERTY);
    }

    /**
     * Returns the number of open tabs.
     */
    public int getTabCount() {
        return tabbedPane.getTabCount();
    }

    /**
     * Adds a change listener to the tabbed pane for tab selection changes.
     */
    public void addTabChangeListener(ChangeListener listener) {
        tabbedPane.addChangeListener(listener);
    }

    /**
     * Updates the tab title for the active tab.
     */
    public void updateActiveTabTitle() {
        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return;
        }

        FileTab activeTab = getFileTabAt(index);
        if (activeTab != null) {
            JComponent scrollPane = (JComponent) tabbedPane.getComponentAt(index);
            int tabId = (Integer) scrollPane.getClientProperty(TAB_ID_PROPERTY);
            tabbedPane.setTabComponentAt(index, createTabComponent(tabId, activeTab));
        }
    }
}
