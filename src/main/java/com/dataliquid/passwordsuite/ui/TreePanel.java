package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigGroup;
import com.dataliquid.passwordsuite.domain.ConfigNode;
import com.dataliquid.passwordsuite.domain.ConfigTree;

/**
 * Panel containing a tree view of the configuration structure.
 */
public final class TreePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int SINGLE_SELECTION = 1;

    final JTree tree; // Package-private for MainFrame access
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private Consumer<ConfigEntry> cryptoToggleCallback;
    private Supplier<Boolean> isYamlFormatSupplier;
    private BiConsumer<ConfigEntry, String> valueEditCallback;

    public TreePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Configuration Tree"));
        setName("treePanel");

        rootNode = new DefaultMutableTreeNode("Configuration");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setName("configTree");
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ConfigTreeCellRenderer());

        // Enable multi-selection (CTRL+click, SHIFT+click)
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Setup inline editing with Insert key
        tree.setEditable(true);
        tree.setCellEditor(new ConfigEntryCellEditor());
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                    startEditingSelectedEntry();
                }
            }
        });

        // Context menu for marking/unmarking entries
        setupContextMenu();

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setName("treeScrollPane");
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Sets the callback to be invoked when crypto toggle is requested.
     */
    public void setCryptoToggleCallback(Consumer<ConfigEntry> callback) {
        this.cryptoToggleCallback = callback;
    }

    /**
     * Sets the supplier that determines if the current file uses YAML format.
     */
    public void setFormatSupplier(Supplier<Boolean> supplier) {
        this.isYamlFormatSupplier = supplier;
    }

    /**
     * Sets the callback to be invoked when a value is edited inline.
     *
     * @param callback receives the ConfigEntry and the new value
     */
    public void setValueEditCallback(BiConsumer<ConfigEntry, String> callback) {
        this.valueEditCallback = callback;
    }

    /**
     * Starts inline editing for the currently selected entry.
     */
    private void startEditingSelectedEntry() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof ConfigEntry) {
                tree.startEditingAtPath(path);
            }
        }
    }

    /**
     * Sets up the right-click context menu for tree operations.
     */
    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem toggleMarkerItem = new JMenuItem("Toggle Encryption Marker");
        toggleMarkerItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        toggleMarkerItem.addActionListener(e -> toggleSelectedEntriesMarker());
        contextMenu.add(toggleMarkerItem);

        JMenuItem toggleCryptoItem = new JMenuItem("Toggle Encrypt/Decrypt");
        toggleCryptoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
        toggleCryptoItem.addActionListener(e -> toggleSelectedEntryCrypto());
        contextMenu.add(toggleCryptoItem);

        contextMenu.addSeparator();

        JMenuItem copyKeyItem = new JMenuItem("Copy Key");
        copyKeyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK));
        copyKeyItem.addActionListener(e -> copySelectedKey());
        contextMenu.add(copyKeyItem);

        JMenuItem copyValueItem = new JMenuItem("Copy Value");
        copyValueItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK));
        copyValueItem.addActionListener(e -> copySelectedValue());
        contextMenu.add(copyValueItem);

        JMenuItem copyEntryItem = new JMenuItem("Copy Entry");
        copyEntryItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
        copyEntryItem.addActionListener(e -> copySelectedEntries());
        contextMenu.add(copyEntryItem);

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleContextMenuTrigger(e, contextMenu, toggleMarkerItem, toggleCryptoItem, copyKeyItem, copyValueItem,
                        copyEntryItem);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleContextMenuTrigger(e, contextMenu, toggleMarkerItem, toggleCryptoItem, copyKeyItem, copyValueItem,
                        copyEntryItem);
            }
        });
    }

    /**
     * Handles context menu trigger (right-click).
     */
    private void handleContextMenuTrigger(MouseEvent e, JPopupMenu contextMenu, JMenuItem toggleMarkerItem,
            JMenuItem toggleCryptoItem, JMenuItem copyKeyItem, JMenuItem copyValueItem, JMenuItem copyEntryItem) {
        if (e.isPopupTrigger()) {
            TreePath clickedPath = tree.getPathForLocation(e.getX(), e.getY());
            if (clickedPath != null) {
                // Check if clicked path is part of current selection
                boolean clickedOnSelection = tree.isPathSelected(clickedPath);

                if (!clickedOnSelection) {
                    // If right-click outside selection, select only the clicked item
                    tree.setSelectionPath(clickedPath);
                }
                // If right-click on selection, keep the multi-selection

                // Check selected items
                List<ConfigEntry> selectedEntries = getSelectedEntries();
                List<ConfigGroup> selectedGroups = getSelectedGroups();

                boolean hasEntries = !selectedEntries.isEmpty();
                boolean hasGroups = !selectedGroups.isEmpty();

                if (hasEntries || hasGroups) {
                    // Toggle marker only visible for entries
                    toggleMarkerItem.setVisible(hasEntries);
                    if (hasEntries) {
                        int count = selectedEntries.size();
                        boolean singleEntry = count == SINGLE_SELECTION;

                        if (singleEntry) {
                            toggleMarkerItem.setText("Toggle Encryption Marker");
                        } else {
                            toggleMarkerItem.setText("Toggle Encryption Marker (" + count + " items)");
                        }
                    }

                    // Toggle crypto only visible for single entry (not groups)
                    boolean singleEntryForCrypto = selectedEntries.size() == SINGLE_SELECTION && !hasGroups;
                    toggleCryptoItem.setVisible(singleEntryForCrypto);

                    // Copy Key: visible for single entry or single group
                    boolean singleEntrySelected = selectedEntries.size() == SINGLE_SELECTION && !hasGroups;
                    boolean singleGroupSelected = selectedGroups.size() == SINGLE_SELECTION && !hasEntries;
                    copyKeyItem.setVisible(singleEntrySelected || singleGroupSelected);

                    // Copy Value: for single entry or single group (copies child values for group)
                    copyValueItem.setVisible(singleEntrySelected || singleGroupSelected);

                    // Copy Entry: visible when any entries or groups are selected
                    copyEntryItem.setVisible(true);

                    contextMenu.show(tree, e.getX(), e.getY());
                }
            }
        }
    }

    /**
     * Toggles the encryption marker for all selected entries.
     */
    public void toggleSelectedEntriesMarker() {
        List<ConfigEntry> entries = getSelectedEntries();
        if (!entries.isEmpty()) {
            for (ConfigEntry entry : entries) {
                entry.setMarkedForEncryption(!entry.isMarkedForEncryption());
            }
            refresh();
        }
    }

    /**
     * Toggles encrypt/decrypt for the selected entry (single selection only).
     */
    public void toggleSelectedEntryCrypto() {
        List<ConfigEntry> entries = getSelectedEntries();
        if (entries.size() == SINGLE_SELECTION && cryptoToggleCallback != null) {
            cryptoToggleCallback.accept(entries.get(0));
        }
    }

    /**
     * Copies the key of the selected entry or group to clipboard (single selection
     * only).
     */
    public void copySelectedKey() {
        List<ConfigEntry> entries = getSelectedEntries();
        List<ConfigGroup> groups = getSelectedGroups();

        if (entries.size() == SINGLE_SELECTION && groups.isEmpty()) {
            copyToClipboard(entries.get(0).getKey());
        } else if (groups.size() == SINGLE_SELECTION && entries.isEmpty()) {
            copyToClipboard(groups.get(0).getKey());
        }
    }

    /**
     * Copies the value of the selected entry or group children (key+value) to
     * clipboard.
     */
    public void copySelectedValue() {
        List<ConfigEntry> entries = getSelectedEntries();
        List<ConfigGroup> groups = getSelectedGroups();

        if (entries.size() == SINGLE_SELECTION && groups.isEmpty()) {
            copyToClipboard(entries.get(0).getValue());
        } else if (groups.size() == SINGLE_SELECTION && entries.isEmpty()) {
            // Copy all child entries (key + value) from the group
            boolean isYaml = isYamlFormatSupplier != null && Boolean.TRUE.equals(isYamlFormatSupplier.get());
            String separator = isYaml ? ": " : "=";
            String result = groups
                    .get(0)
                    .getAllEntries()
                    .stream()
                    .map(e -> e.getKey() + separator + e.getValue())
                    .collect(Collectors.joining("\n"));
            copyToClipboard(result);
        }
    }

    /**
     * Copies selected entries and group entries to clipboard in format depending on
     * file type. Selected items are treated as root (relative paths only).
     */
    public void copySelectedEntries() {
        boolean isYaml = isYamlFormatSupplier != null && Boolean.TRUE.equals(isYamlFormatSupplier.get());
        String separator = isYaml ? ": " : "=";
        List<String> lines = new ArrayList<>();

        // Add directly selected entries (just key, no path prefix)
        for (ConfigEntry entry : getSelectedEntries()) {
            lines.add(entry.getKey() + separator + entry.getValue());
        }

        // Add entries from selected groups (group key + relative path)
        for (ConfigGroup group : getSelectedGroups()) {
            String groupKey = group.getKey();
            String groupPath = group.getPath();
            for (ConfigEntry entry : group.getAllEntries()) {
                String entryPath = entry.getPath();
                // Calculate relative path from group parent, including group key
                String relativePath = entryPath.startsWith(groupPath + ".")
                        ? groupKey + "." + entryPath.substring(groupPath.length() + 1)
                        : groupKey + "." + entry.getKey();
                lines.add(relativePath + separator + entry.getValue());
            }
        }

        if (!lines.isEmpty()) {
            copyToClipboard(String.join("\n", lines));
        }
    }

    /**
     * Copies text to the system clipboard.
     */
    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /**
     * Custom TreeCellRenderer that displays a lock icon for entries marked for
     * encryption.
     */
    private static final class ConfigTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final String LOCK_ICON = "\uD83D\uDD12"; // 🔒
        private static final String KEY_VALUE_SEPARATOR = " = ";

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof ConfigEntry) {
                    ConfigEntry entry = (ConfigEntry) userObject;
                    String label = buildNodeLabel(entry);

                    if (entry.isMarkedForEncryption()) {
                        setText(LOCK_ICON + " " + label);
                    } else {
                        setText(label);
                    }
                } else if (userObject instanceof ConfigGroup) {
                    ConfigGroup group = (ConfigGroup) userObject;
                    setText(group.getKey() + " (" + group.getChildCount() + " items)");
                }
            }

            return this;
        }

        private String buildNodeLabel(ConfigEntry entry) {
            String value = entry.getValue();
            String base = entry.getKey() + KEY_VALUE_SEPARATOR + value;
            if (entry.isEncrypted()) {
                return base + " [ENC]";
            }
            return base;
        }
    }

    /**
     * Custom TreeCellEditor that allows editing only the value of ConfigEntry
     * nodes. Editing is triggered by the Insert key and only works on ConfigEntry
     * nodes.
     */
    @SuppressWarnings("PMD.NullAssignment")
    private final class ConfigEntryCellEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        private transient ConfigEntry currentEntry;
        private String originalValue;
        private boolean cancelled;

        ConfigEntryCellEditor() {
            super(new JTextField());
            // Add key listener to catch Escape before DefaultCellEditor processes it
            JTextField textField = (JTextField) getComponent();
            textField.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        cancelled = true;
                    }
                }
            });
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            return event == null;
        }

        @Override
        public Component getTreeCellEditorComponent(JTree editTree, Object value, boolean isSelected, boolean expanded,
                boolean leaf, int row) {
            cancelled = false;
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof ConfigEntry) {
                    currentEntry = (ConfigEntry) userObject;
                    originalValue = currentEntry.getValue();
                    JTextField textField = (JTextField) getComponent();
                    textField.setText(originalValue);
                    return textField;
                }
            }
            currentEntry = null;
            originalValue = null;
            return super.getTreeCellEditorComponent(editTree, value, isSelected, expanded, leaf, row);
        }

        @Override
        public void cancelCellEditing() {
            cancelled = true;
            super.cancelCellEditing();
        }

        @Override
        public boolean stopCellEditing() {
            if (!cancelled) {
                JTextField textField = (JTextField) getComponent();
                String newValue = textField.getText();

                if (currentEntry != null && valueEditCallback != null && !newValue.equals(originalValue)) {
                    valueEditCallback.accept(currentEntry, newValue);
                }
            }

            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            JTextField textField = (JTextField) getComponent();
            return cancelled ? originalValue : textField.getText();
        }
    }

    /**
     * Loads a ConfigTree into the tree view.
     */
    public void loadConfigTree(ConfigTree configTree) {
        rootNode.removeAllChildren();

        if (configTree != null) {
            for (ConfigNode child : configTree.getRoot().getChildren()) {
                DefaultMutableTreeNode childNode = buildTreeNode(child);
                rootNode.add(childNode);
            }
        }

        treeModel.reload();
        expandAll();
    }

    /**
     * Recursively builds tree nodes from ConfigNode structure.
     */
    private DefaultMutableTreeNode buildTreeNode(ConfigNode configNode) {
        // Note: The initial label is not displayed - ConfigTreeCellRenderer uses
        // userObject
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(configNode.getKey());
        treeNode.setUserObject(configNode);

        if (configNode instanceof ConfigGroup) {
            ConfigGroup group = (ConfigGroup) configNode;
            for (ConfigNode child : group.getChildren()) {
                treeNode.add(buildTreeNode(child));
            }
        }

        return treeNode;
    }

    /**
     * Expands all nodes in the tree.
     */
    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Returns all selected ConfigEntry nodes.
     */
    public List<ConfigEntry> getSelectedEntries() {
        List<ConfigEntry> entries = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();

        if (paths != null) {
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();

                if (userObject instanceof ConfigEntry) {
                    entries.add((ConfigEntry) userObject);
                }
            }
        }

        return entries;
    }

    /**
     * Returns all selected ConfigGroup nodes.
     */
    public List<ConfigGroup> getSelectedGroups() {
        List<ConfigGroup> groups = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();

        if (paths != null) {
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();

                if (userObject instanceof ConfigGroup) {
                    groups.add((ConfigGroup) userObject);
                }
            }
        }

        return groups;
    }

    /**
     * Refreshes the tree display (e.g., after encryption/decryption).
     */
    public void refresh() {
        treeModel.reload();
        expandAll();
    }

    /**
     * Returns all ConfigEntry nodes that are marked for encryption.
     */
    public List<ConfigEntry> getMarkedEntries() {
        List<ConfigEntry> entries = new ArrayList<>();
        collectMarkedEntries(rootNode, entries);
        return entries;
    }

    /**
     * Recursively collects all marked ConfigEntry nodes from the tree.
     */
    private void collectMarkedEntries(DefaultMutableTreeNode node, List<ConfigEntry> entries) {
        Object userObject = node.getUserObject();
        if (userObject instanceof ConfigEntry) {
            ConfigEntry entry = (ConfigEntry) userObject;
            if (entry.isMarkedForEncryption()) {
                entries.add(entry);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectMarkedEntries((DefaultMutableTreeNode) node.getChildAt(i), entries);
        }
    }

    /**
     * Clears the tree.
     */
    public void clear() {
        rootNode.removeAllChildren();
        treeModel.reload();
    }
}
