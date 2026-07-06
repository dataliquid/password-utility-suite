package com.dataliquid.passwordsuite.ui.handler;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.domain.ConfigTree;
import com.dataliquid.passwordsuite.environment.EnvironmentManager;
import com.dataliquid.passwordsuite.service.EditorService;
import com.dataliquid.passwordsuite.service.ExportService;
import com.dataliquid.passwordsuite.service.FileIOService;
import com.dataliquid.passwordsuite.ui.FileTab;
import com.dataliquid.passwordsuite.ui.TabbedEditorPanel;
import com.dataliquid.passwordsuite.ui.TreePanel;
import com.dataliquid.passwordsuite.ui.UIConstants;

/**
 * Handles file operations (New, Open, Save, SaveAs, Exit, Copy).
 * <p>
 * Extracted from MainFrame to follow Single Responsibility Principle.
 * </p>
 */
public class FileOperationHandler {

    private static final Logger logger = LoggerFactory.getLogger(FileOperationHandler.class);

    private final TabbedEditorPanel tabbedEditorPanel;
    private final TreePanel treePanel;
    private final EditorService editorService;
    private final ExportService exportService;
    private final FileIOService fileIOService;
    private final EnvironmentManager environmentManager;
    private final Component parentComponent;
    private final BiConsumer<FileTab, String> autoParseCallback;

    private File lastDirectory;
    private Runnable statusBarUpdateCallback;

    /**
     * Creates a new FileOperationHandler.
     *
     * @param tabbedEditorPanel  the tabbed editor panel
     * @param treePanel          the tree panel for configuration display
     * @param editorService      the editor service
     * @param exportService      the export service
     * @param fileIOService      the file I/O service
     * @param environmentManager the environment manager
     * @param parentComponent    the parent component for dialogs
     * @param autoParseCallback  callback to auto-parse tab content
     */
    public FileOperationHandler(TabbedEditorPanel tabbedEditorPanel, TreePanel treePanel, EditorService editorService,
            ExportService exportService, FileIOService fileIOService, EnvironmentManager environmentManager,
            Component parentComponent, BiConsumer<FileTab, String> autoParseCallback) {
        this.tabbedEditorPanel = tabbedEditorPanel;
        this.treePanel = treePanel;
        this.editorService = editorService;
        this.exportService = exportService;
        this.fileIOService = fileIOService;
        this.environmentManager = environmentManager;
        this.parentComponent = parentComponent;
        this.autoParseCallback = autoParseCallback;
    }

    /**
     * Sets the callback to update the status bar after file operations.
     *
     * @param callback the callback to invoke
     */
    public void setStatusBarUpdateCallback(Runnable callback) {
        this.statusBarUpdateCallback = callback;
    }

    /**
     * Creates a new empty tab.
     */
    public void handleNew() {
        tabbedEditorPanel.addNewTab();
        treePanel.clear();
        editorService.clearUndoRedo();
        logger.info("Created new tab");
    }

    /**
     * Opens one or more configuration files via file chooser dialog.
     */
    public void handleOpen() {
        JFileChooser chooser = new JFileChooser();

        // Add file filters for Properties and YAML files
        FileNameExtensionFilter configFilter = new FileNameExtensionFilter(
                "Configuration Files (*.properties, *.yaml, *.yml)", "properties", "yaml", "yml");
        FileNameExtensionFilter propertiesFilter = new FileNameExtensionFilter("Properties Files (*.properties)",
                "properties");
        FileNameExtensionFilter yamlFilter = new FileNameExtensionFilter("YAML Files (*.yaml, *.yml)", "yaml", "yml");

        chooser.addChoosableFileFilter(configFilter);
        chooser.addChoosableFileFilter(propertiesFilter);
        chooser.addChoosableFileFilter(yamlFilter);
        chooser.setFileFilter(configFilter);
        chooser.setMultiSelectionEnabled(true);

        // Set current directory: prefer lastDirectory, fallback to active tab's file
        if (lastDirectory != null) {
            chooser.setCurrentDirectory(lastDirectory);
        } else {
            FileTab activeTab = tabbedEditorPanel.getActiveTab();
            if (activeTab != null && activeTab.getFile() != null) {
                chooser.setCurrentDirectory(activeTab.getFile().getParentFile());
            }
        }

        int result = chooser.showOpenDialog(parentComponent);
        if (result == JFileChooser.APPROVE_OPTION) {
            // Remember the directory for next time
            lastDirectory = chooser.getCurrentDirectory();
            File[] selectedFiles = chooser.getSelectedFiles();

            if (selectedFiles == null || selectedFiles.length == 0) {
                return;
            }

            int successCount = 0;
            int errorCount = 0;

            for (File selectedFile : selectedFiles) {
                try {
                    openFile(selectedFile);
                    successCount++;
                } catch (IOException ex) {
                    if (logger.isErrorEnabled()) {
                        logger.error("File open error: {} - {}", selectedFile.getName(), ex.getMessage(), ex);
                    }
                    errorCount++;
                }
            }

            // Show summary message
            if (successCount > 0 && errorCount == 0) {
                showInfo(successCount + " file(s) opened successfully");
            } else if (successCount > 0 && errorCount > 0) {
                showInfo(successCount + " file(s) opened, " + errorCount + " failed");
            } else {
                showError("Failed to open selected files");
            }
        }
    }

    /**
     * Opens a single file and creates a new tab for it.
     * <p>
     * This method can be used programmatically to open files without showing the
     * file chooser dialog (e.g., for drag & drop, recent files, or testing).
     * </p>
     *
     * @param  file        the file to open
     *
     * @return             the created FileTab
     *
     * @throws IOException if the file cannot be read
     */
    public FileTab openFile(File file) throws IOException {
        String content = fileIOService.loadFile(file);

        // Create new tab for the file
        FileTab newTab = tabbedEditorPanel.addNewTab();
        newTab.setFile(file);
        newTab.setText(content);
        tabbedEditorPanel.updateActiveTabTitle();

        // Auto-parse the file content
        if (autoParseCallback != null) {
            autoParseCallback.accept(newTab, content);
        }

        // Update status bar
        if (statusBarUpdateCallback != null) {
            statusBarUpdateCallback.run();
        }

        if (logger.isInfoEnabled()) {
            logger.info("Opened file: {}", file.getAbsolutePath());
        }

        return newTab;
    }

    /**
     * Saves the active tab to its current file.
     */
    public void handleSave() {
        FileTab activeTab = tabbedEditorPanel.getActiveTab();
        if (activeTab == null) {
            showError(UIConstants.NO_ACTIVE_TAB_MSG);
            return;
        }

        if (activeTab.getFile() == null) {
            handleSaveAs();
            return;
        }

        try {
            ConfigTree tree = activeTab.getConfigTree();
            String content = tree != null ? exportService.export(tree) : activeTab.getText();
            fileIOService.saveFile(activeTab.getFile(), content);
            activeTab.setModified(false);
            tabbedEditorPanel.updateActiveTabTitle();
            if (logger.isInfoEnabled()) {
                logger.info("Saved file: {}", activeTab.getFile().getAbsolutePath());
            }
            showInfo("File saved: " + activeTab.getFile().getName());
        } catch (IOException ex) {
            logger.error("File save error", ex);
            showError("Error saving file: " + ex.getMessage());
        }
    }

    /**
     * Saves the active tab with a new file name.
     */
    public void handleSaveAs() {
        FileTab activeTab = tabbedEditorPanel.getActiveTab();
        if (activeTab == null) {
            showError(UIConstants.NO_ACTIVE_TAB_MSG);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        if (activeTab.getFile() != null) {
            chooser.setCurrentDirectory(activeTab.getFile().getParentFile());
            chooser.setSelectedFile(activeTab.getFile());
        } else if (lastDirectory != null) {
            chooser.setCurrentDirectory(lastDirectory);
        }

        int result = chooser.showSaveDialog(parentComponent);
        if (result == JFileChooser.APPROVE_OPTION) {
            // Remember the directory for next time
            lastDirectory = chooser.getCurrentDirectory();
            activeTab.setFile(chooser.getSelectedFile());
            tabbedEditorPanel.updateActiveTabTitle();
            handleSave();
        }
    }

    /**
     * Exits the application after cleanup.
     */
    public void handleExit() {
        // Clear sensitive data from memory
        environmentManager.cleanup();

        logger.info("Application exit requested");
        if (parentComponent instanceof JFrame) {
            ((JFrame) parentComponent).dispose();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(parentComponent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(parentComponent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
