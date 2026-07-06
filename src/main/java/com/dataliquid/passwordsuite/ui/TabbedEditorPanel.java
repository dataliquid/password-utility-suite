package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

/**
 * Panel containing a tabbed editor for managing multiple open files. Each tab
 * contains a FileTab instance with its own editor and configuration tree.
 */
@SuppressWarnings("PMD.AssignmentInOperand")
public final class TabbedEditorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane tabbedPane;
    private final Map<Integer, FileTab> tabMap;
    private int nextTabId;

    public TabbedEditorPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Editor"));

        tabbedPane = new JTabbedPane();
        tabbedPane.setName("editorTabbedPane");
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabMap = new ConcurrentHashMap<>();

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Adds a new empty tab.
     */
    public FileTab addNewTab() {
        int tabId = nextTabId++;
        FileTab fileTab = new FileTab();

        tabMap.put(tabId, fileTab);

        // Add tab with close button
        int index = tabbedPane.getTabCount();
        tabbedPane.addTab(fileTab.getTabTitle(), fileTab.getScrollPane());
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
        closeButton.addActionListener(e -> closeTab(tabId));

        panel.add(closeButton);

        return panel;
    }

    /**
     * Closes a tab by its ID.
     */
    private void closeTab(int tabId) {
        FileTab fileTab = tabMap.get(tabId);
        if (fileTab == null) {
            return;
        }

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

        // Find index by tab ID
        int indexToRemove = -1;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (Objects.equals(getFileTabAt(i), fileTab)) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove >= 0) {
            // Cleanup resources before removing
            fileTab.cleanup();
            tabbedPane.removeTabAt(indexToRemove);
            tabMap.remove(tabId);
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
        for (FileTab fileTab : tabMap.values()) {
            if (tabbedPane.indexOfComponent(fileTab.getScrollPane()) == index) {
                return fileTab;
            }
        }
        return null;
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
        FileTab activeTab = getActiveTab();
        if (activeTab != null) {
            int index = tabbedPane.getSelectedIndex();
            if (index >= 0) {
                // Update the tab component with new title
                int tabId = getTabIdForFileTab(activeTab);
                if (tabId >= 0) {
                    tabbedPane.setTabComponentAt(index, createTabComponent(tabId, activeTab));
                }
            }
        }
    }

    /**
     * Finds the tab ID for a given FileTab.
     */
    private int getTabIdForFileTab(FileTab fileTab) {
        for (Map.Entry<Integer, FileTab> entry : tabMap.entrySet()) {
            if (Objects.equals(entry.getValue(), fileTab)) {
                return entry.getKey();
            }
        }
        return -1;
    }

}
