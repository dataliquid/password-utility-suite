package com.dataliquid.passwordsuite.ui.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.service.EditorService;
import com.dataliquid.passwordsuite.ui.TreePanel;

/**
 * Handles edit operations (Undo, Redo).
 * <p>
 * Extracted from MainFrame to follow Single Responsibility Principle.
 * </p>
 */
public class EditOperationHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditOperationHandler.class);

    private final EditorService editorService;
    private final TreePanel treePanel;

    /**
     * Creates a new EditOperationHandler.
     *
     * @param editorService the editor service for undo/redo operations
     * @param treePanel     the tree panel to refresh after operations
     */
    public EditOperationHandler(EditorService editorService, TreePanel treePanel) {
        this.editorService = editorService;
        this.treePanel = treePanel;
    }

    /**
     * Performs an undo operation if available.
     */
    public void handleUndo() {
        if (editorService.canUndo()) {
            editorService.undo();
            treePanel.refresh();
            logger.info("Undo performed");
        }
    }

    /**
     * Performs a redo operation if available.
     */
    public void handleRedo() {
        if (editorService.canRedo()) {
            editorService.redo();
            treePanel.refresh();
            logger.info("Redo performed");
        }
    }

    /**
     * Checks if an undo operation is available.
     *
     * @return true if undo is available
     */
    public boolean canUndo() {
        return editorService.canUndo();
    }

    /**
     * Checks if a redo operation is available.
     *
     * @return true if redo is available
     */
    public boolean canRedo() {
        return editorService.canRedo();
    }
}
