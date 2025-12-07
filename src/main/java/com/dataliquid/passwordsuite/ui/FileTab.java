package com.dataliquid.passwordsuite.ui;

import java.awt.Font;
import java.io.File;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.dataliquid.passwordsuite.domain.ConfigTree;

/**
 * Represents a file tab in the tabbed editor. Contains the file, editor
 * component, and associated config tree.
 */
public class FileTab {

    private File file;
    private final JTextArea editor;
    private ConfigTree configTree;
    private boolean modified;
    private final JScrollPane scrollPane;
    private Runnable parseCallback;
    private Timer parseTimer;
    private String selectedAlgorithm;
    private String selectedEnvironment;
    private final DocumentListener documentListener;

    public FileTab() {
        // Initialize default values
        this.selectedEnvironment = UIConstants.NO_ENVIRONMENT;

        // Create editor
        editor = new JTextArea();
        editor.setName("fileEditor");
        editor.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editor.setTabSize(2);
        editor.setLineWrap(false);

        // Store DocumentListener reference to enable cleanup
        documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleDocumentChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleDocumentChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleDocumentChange();
            }
        };
        editor.getDocument().addDocumentListener(documentListener);

        scrollPane = new JScrollPane(editor);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Handles document changes - marks as modified and triggers auto-parse.
     */
    private void handleDocumentChange() {
        setModified(true);
        triggerAutoParseWithDelay();
    }

    /**
     * Returns the file associated with this tab.
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file associated with this tab.
     */
    public void setFile(File file) {
        this.file = file;
        setModified(false); // Reset modified flag when loading file
    }

    /**
     * Returns the text editor component.
     */
    public JTextArea getEditor() {
        return editor;
    }

    /**
     * Returns the scroll pane containing the editor.
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Returns the text content of the editor.
     */
    public String getText() {
        return editor.getText();
    }

    /**
     * Sets the text content of the editor.
     */
    public void setText(String text) {
        editor.setText(text);
        editor.setCaretPosition(0);
        setModified(false);
    }

    /**
     * Returns the config tree associated with this tab.
     */
    public ConfigTree getConfigTree() {
        return configTree;
    }

    /**
     * Sets the config tree associated with this tab.
     */
    public void setConfigTree(ConfigTree configTree) {
        this.configTree = configTree;
    }

    /**
     * Returns true if the content has been modified.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Sets the modified flag.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * Returns the tab title (including * if modified).
     */
    public String getTabTitle() {
        String title = file != null ? file.getName() : "Untitled";
        return modified ? title + " *" : title;
    }

    /**
     * Clears the editor content.
     */
    public void clear() {
        editor.setText("");
        clearConfigTree();
        setModified(false);
    }

    /**
     * Clears the config tree reference.
     */
    @SuppressWarnings("PMD.NullAssignment")
    private void clearConfigTree() {
        this.configTree = null;
    }

    /**
     * Sets the callback to be invoked when auto-parse is triggered.
     */
    public void setParseCallback(Runnable callback) {
        this.parseCallback = callback;
    }

    /**
     * Triggers auto-parse with a 500ms debounce delay. Cancels any existing pending
     * parse timer.
     */
    private void triggerAutoParseWithDelay() {
        // Cancel existing timer if any
        if (parseTimer != null && parseTimer.isRunning()) {
            parseTimer.stop();
        }

        // Only trigger auto-parse if callback is set
        if (parseCallback != null) {
            // Create new timer with 500ms delay (debounce)
            parseTimer = new Timer(500, e -> {
                parseCallback.run();
            });
            parseTimer.setRepeats(false); // Only fire once
            parseTimer.start();
        }
    }

    /**
     * Returns the selected algorithm for this tab.
     */
    public String getSelectedAlgorithm() {
        return selectedAlgorithm;
    }

    /**
     * Sets the selected algorithm for this tab.
     */
    public void setSelectedAlgorithm(String algorithm) {
        this.selectedAlgorithm = algorithm;
    }

    /**
     * Returns the selected environment for this tab.
     */
    public String getSelectedEnvironment() {
        return selectedEnvironment;
    }

    /**
     * Sets the selected environment for this tab.
     */
    public void setSelectedEnvironment(String environment) {
        this.selectedEnvironment = environment;
    }

    /**
     * Cleans up resources held by this tab. Should be called before the tab is
     * closed/removed.
     */
    public void cleanup() {
        // Stop any pending auto-parse timer
        if (parseTimer != null && parseTimer.isRunning()) {
            parseTimer.stop();
        }
        // Remove document listener to prevent memory leak
        editor.getDocument().removeDocumentListener(documentListener);
    }
}
