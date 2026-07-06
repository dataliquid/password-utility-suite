package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.crypto.factory.CipherRegistry;
import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigTree;
import com.dataliquid.passwordsuite.parser.ParseException;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.EditorService;
import com.dataliquid.passwordsuite.service.ExportService;
import com.dataliquid.passwordsuite.service.FileIOService;
import com.dataliquid.passwordsuite.ui.handler.CryptoOperationHandler;
import com.dataliquid.passwordsuite.ui.handler.EditOperationHandler;
import com.dataliquid.passwordsuite.ui.handler.EnvironmentsHandler;
import com.dataliquid.passwordsuite.ui.handler.FileDropHandler;
import com.dataliquid.passwordsuite.ui.handler.FileOperationHandler;
import com.dataliquid.passwordsuite.environment.EnvironmentManager;

/**
 * Main application frame integrating all UI components and services. Delegates
 * operations to specialized handler classes.
 */
public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int DOUBLE_CLICK_COUNT = 2;

    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    // UI Components
    private final TabbedEditorPanel tabbedEditorPanel;
    private final TreePanel treePanel;
    private final ActionPanel actionPanel;
    private final StatusBar statusBar;

    // Services (transient - not serializable)
    private final transient EditorService editorService;

    // Handlers (transient - not serializable)
    private final transient FileOperationHandler fileHandler;
    private final transient EditOperationHandler editHandler;
    private final transient CryptoOperationHandler cryptoHandler;
    private final transient EnvironmentsHandler environmentsHandler;
    private final transient EnvironmentManager environmentManager;

    public MainFrame() {
        // Initialize cipher registry
        CipherRegistry cipherRegistry = new CipherRegistry();

        // Initialize services
        editorService = new EditorService();
        CryptoService cryptoService = new CryptoService();
        logger.info("CryptoService initialized (no password configured)");

        ExportService exportService = new ExportService();
        FileIOService fileIOService = new FileIOService();

        // Initialize password manager
        environmentManager = new EnvironmentManager();

        // Setup frame
        setTitle("Password Utility Suite");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create UI components
        tabbedEditorPanel = new TabbedEditorPanel();
        treePanel = new TreePanel();
        actionPanel = new ActionPanel();
        statusBar = new StatusBar();

        // Populate algorithm dropdown
        actionPanel.updateAlgorithms(cipherRegistry.getAvailableAlgorithms());

        // Initialize handlers with dependencies
        fileHandler = new FileOperationHandler(tabbedEditorPanel, treePanel, editorService, exportService,
                fileIOService, environmentManager, this, this::autoParseTab);
        fileHandler.setStatusBarUpdateCallback(this::updateStatusBar);

        editHandler = new EditOperationHandler(editorService, treePanel);

        cryptoHandler = new CryptoOperationHandler(cryptoService, editorService, environmentManager);

        environmentsHandler = new EnvironmentsHandler(actionPanel, treePanel, tabbedEditorPanel, cryptoService,
                environmentManager, cipherRegistry, this, this::autoParseTab);
        environmentsHandler.setStatusBarUpdateCallback(this::updateStatusBar);

        // Layout
        setupLayout();

        // Event handlers
        setupEventHandlers();

        // Menu bar
        setupMenuBar();

        // Drag and drop support
        setupDragAndDrop();

        logger.info("MainFrame initialized successfully");
    }

    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));

        // Create a container panel for the right side (TreePanel + ActionPanel)
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(5, 5));
        rightPanel.add(treePanel, BorderLayout.CENTER);
        rightPanel.add(actionPanel, BorderLayout.SOUTH);

        // Split pane for editor and right container
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(tabbedEditorPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        // Environments - delegate to EnvironmentsHandler
        actionPanel.setAlgorithmChangeListener(e -> environmentsHandler.handleAlgorithmChange());
        actionPanel.setEnvironmentChangeListener(e -> environmentsHandler.handleEnvironmentChange());

        // Tree format supplier - determines YAML vs Properties format for copy
        treePanel.setFormatSupplier(() -> {
            FileTab tab = tabbedEditorPanel.getActiveTab();
            if (tab != null && tab.getFile() != null) {
                String name = tab.getFile().getName().toLowerCase(java.util.Locale.ROOT);
                return name.endsWith(".yaml") || name.endsWith(".yml");
            }
            return false; // Default: Properties format
        });

        // Tree crypto toggle callback - for context menu Alt+C
        treePanel
                .setCryptoToggleCallback(entry -> cryptoHandler
                        .handleTreeDoubleClick(entry, tabbedEditorPanel.getActiveTab(), treePanel::refresh,
                                this::showError));

        // Tree inline value edit callback - for Insert key editing
        treePanel.setValueEditCallback((entry, newValue) -> {
            FileTab activeTab = tabbedEditorPanel.getActiveTab();
            if (activeTab != null) {
                String oldValue = entry.getValue();
                entry.setValue(newValue);
                editorService
                        .replaceValueInContent(entry, oldValue, newValue, activeTab.getEditor().getText())
                        .ifPresent(updated -> EditorTextUtil.setTextPreservingCaret(activeTab.getEditor(), updated));
                // Defer refresh to avoid recursion during cell editor completion
                SwingUtilities.invokeLater(treePanel::refresh);
            }
        });

        // Tree double-click listener - toggle encrypt/decrypt
        treePanel.tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == DOUBLE_CLICK_COUNT) {
                    handleTreeDoubleClick(e);
                }
            }
        });

        // Tab change listener - delegate to EnvironmentsHandler and update status bar
        tabbedEditorPanel.addTabChangeListener(e -> {
            environmentsHandler.handleTabChange();
            updateStatusBar();
        });

        // Setup global keyboard shortcuts
        setupKeyBindings();
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // File Menu - delegate to FileOperationHandler
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem newItem = createMenuItem("New", 'N', e -> fileHandler.handleNew());
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKey));
        fileMenu.add(newItem);

        JMenuItem openItem = createMenuItem("Open...", 'O', e -> fileHandler.handleOpen());
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKey));
        fileMenu.add(openItem);

        JMenuItem saveItem = createMenuItem("Save", 'S', e -> fileHandler.handleSave());
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey));
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = createMenuItem("Save As...", 'A', e -> fileHandler.handleSaveAs());
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", 'x', e -> fileHandler.handleExit()));

        // Edit Menu - delegate to EditOperationHandler
        // Mnemonic 'D' (eDit) to avoid conflict with Alt+E (Copy Entry shortcut)
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('D');
        editMenu.add(createMenuItem("Undo", 'U', e -> editHandler.handleUndo()));
        editMenu.add(createMenuItem("Redo", 'R', e -> editHandler.handleRedo()));

        editMenu.addSeparator();

        // Encryption submenu - delegate to CryptoOperationHandler
        JMenu encryptionMenu = new JMenu("Encryption");
        encryptionMenu.setMnemonic('n');

        JMenuItem encryptMarkedItem = createMenuItem("Encrypt Marked", 'E',
                e -> cryptoHandler
                        .handleEncryptMarked(tabbedEditorPanel.getActiveTab(), treePanel, treePanel::refresh,
                                this::showError, this::showInfo));
        encryptMarkedItem
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
        encryptionMenu.add(encryptMarkedItem);

        JMenuItem decryptMarkedItem = createMenuItem("Decrypt Marked", 'D',
                e -> cryptoHandler
                        .handleDecryptMarked(tabbedEditorPanel.getActiveTab(), treePanel, treePanel::refresh,
                                this::showError, this::showInfo));
        decryptMarkedItem
                .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
        encryptionMenu.add(decryptMarkedItem);

        editMenu.add(encryptionMenu);

        // Settings Menu - delegate to EnvironmentsHandler
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic('S');
        JMenuItem environmentsItem = createMenuItem("Environments...", 'E',
                e -> environmentsHandler.handleEnvironments(this::showError, this::showInfo));
        environmentsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, shortcutKey));
        settingsMenu.add(environmentsItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private void setupDragAndDrop() {
        FileDropHandler dropHandler = new FileDropHandler(file -> {
            try {
                fileHandler.openFile(file);
            } catch (java.io.IOException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to open dropped file: {}", file.getName(), e);
                }
                showError("Failed to open file: " + file.getName());
            }
        });
        ((JComponent) getContentPane()).setTransferHandler(dropHandler);
        if (logger.isDebugEnabled()) {
            logger.debug("Drag and drop support enabled");
        }
    }

    private JMenuItem createMenuItem(String text, char mnemonic, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        item.addActionListener(listener);
        return item;
    }

    private void setupKeyBindings() {
        // Alt+M to toggle encryption marker on selected entries
        KeyStroke toggleMarkerKey = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(toggleMarkerKey, "toggleMarker");
        getRootPane().getActionMap().put("toggleMarker", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.toggleSelectedEntriesMarker();
            }
        });

        // Alt+K to copy selected key
        KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(copyKeyStroke, "copyKey");
        getRootPane().getActionMap().put("copyKey", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.copySelectedKey();
            }
        });

        // Alt+V to copy selected value
        KeyStroke copyValueStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(copyValueStroke, "copyValue");
        getRootPane().getActionMap().put("copyValue", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.copySelectedValue();
            }
        });

        // Alt+E to copy selected entries
        KeyStroke copyEntryStroke = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(copyEntryStroke, "copyEntry");
        getRootPane().getActionMap().put("copyEntry", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.copySelectedEntries();
            }
        });

        // Alt+C to toggle encrypt/decrypt on selected entry
        KeyStroke toggleCryptoStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(toggleCryptoStroke, "toggleCrypto");
        getRootPane().getActionMap().put("toggleCrypto", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.toggleSelectedEntryCrypto();
            }
        });
    }

    // Remaining methods needed by MainFrame

    /**
     * Automatically parses content for a newly opened file tab. Package-private to
     * allow usage as method reference by handlers.
     */
    void autoParseTab(FileTab tab, String content) {
        if (content.isBlank()) {
            logger.debug("Skipping auto-parse: content is empty");
            return;
        }

        try {
            // Save markers from old tree before reparsing
            ConfigTree oldTree = tab.getConfigTree();
            java.util.Set<String> markedPaths = oldTree != null ? oldTree.getMarkedPaths() : java.util.Set.of();

            ConfigTree tree = editorService
                    .parseContent(content, environmentManager.getActiveFormatPrefix(),
                            environmentManager.getActiveFormatSuffix());

            // Restore markers to new tree
            if (!markedPaths.isEmpty()) {
                tree.applyMarkedPaths(markedPaths);
            }

            tab.setConfigTree(tree);
            treePanel.loadConfigTree(tree);
            if (logger.isInfoEnabled()) {
                logger.info("Auto-parsed file successfully: {} entries", tree.getEntryCount());
            }
        } catch (ParseException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Auto-parse failed (file can still be manually parsed): {}", ex.getMessage());
            }
        }
    }

    /**
     * Handles double-click on tree node - delegates to CryptoOperationHandler.
     */
    private void handleTreeDoubleClick(MouseEvent e) {
        TreePath path = treePanel.tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();

        // Only process ConfigEntry nodes (not ConfigGroup nodes)
        if (!(userObject instanceof ConfigEntry)) {
            return;
        }

        ConfigEntry entry = (ConfigEntry) userObject;
        cryptoHandler
                .handleTreeDoubleClick(entry, tabbedEditorPanel.getActiveTab(), treePanel::refresh, this::showError);
    }

    /**
     * Updates the status bar with current file name and environment count.
     */
    private void updateStatusBar() {
        FileTab activeTab = tabbedEditorPanel.getActiveTab();
        if (activeTab != null && activeTab.getFile() != null) {
            statusBar.setFileName(activeTab.getFile().getName());
        } else {
            statusBar.setFileName(null);
        }
        statusBar.setEnvironmentCount(environmentManager.getEnvironmentCount());
    }

    /** Package-private to allow usage as method reference by handlers. */
    void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Package-private to allow usage as method reference by handlers. */
    void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Test accessors (package-private) ---

    /**
     * Returns the file operation handler. Package-private for testing.
     */
    FileOperationHandler getFileHandler() {
        return fileHandler;
    }

    /**
     * Returns the tabbed editor panel. Package-private for testing.
     */
    TabbedEditorPanel getTabbedEditorPanel() {
        return tabbedEditorPanel;
    }
}
