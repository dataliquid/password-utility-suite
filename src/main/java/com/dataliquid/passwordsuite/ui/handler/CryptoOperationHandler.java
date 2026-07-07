package com.dataliquid.passwordsuite.ui.handler;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.operation.Operation;
import com.dataliquid.passwordsuite.environment.EnvironmentManager;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.EditorService;
import com.dataliquid.passwordsuite.ui.EditorTextUtil;
import com.dataliquid.passwordsuite.ui.FileTab;
import com.dataliquid.passwordsuite.ui.TreePanel;
import com.dataliquid.passwordsuite.ui.UIConstants;

/**
 * Handles encryption and decryption operations on configuration entries.
 * Eliminates code duplication between encrypt/decrypt handlers.
 */
public class CryptoOperationHandler {

    private static final Logger logger = LoggerFactory.getLogger(CryptoOperationHandler.class);
    private static final String ENCRYPTED_VERB = "Encrypted";
    private static final String DECRYPTED_VERB = "Decrypted";

    private final CryptoService cryptoService;
    private final EditorService editorService;
    private final EnvironmentManager environmentManager;

    public CryptoOperationHandler(CryptoService cryptoService, EditorService editorService,
            EnvironmentManager environmentManager) {
        this.cryptoService = cryptoService;
        this.editorService = editorService;
        this.environmentManager = environmentManager;
    }

    /**
     * Processes a single entry for encryption or decryption (for double-click
     * toggle).
     *
     * @param  entry           the entry to process
     * @param  editor          the text editor to update
     * @param  refreshCallback callback to refresh the UI after processing
     *
     * @return                 true if encryption was performed, false if decryption
     */
    public boolean toggleEncryption(ConfigEntry entry, JTextArea editor, Runnable refreshCallback) {
        boolean wasEncrypted = entry.isEncrypted();
        String oldValue = entry.getValue();

        Operation operation = wasEncrypted ? cryptoService.createDecryptOperation(entry)
                : cryptoService.createEncryptOperation(entry);

        editorService.executeOperation(operation);
        String newValue = entry.getValue();

        editorService
                .replaceValueInContent(entry, oldValue, newValue, editor.getText())
                .ifPresent(updated -> EditorTextUtil.setTextPreservingCaret(editor, updated));

        if (refreshCallback != null) {
            refreshCallback.run();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} entry: {}", wasEncrypted ? DECRYPTED_VERB : ENCRYPTED_VERB, entry.getKey());
        }
        return !wasEncrypted;
    }

    /**
     * Checks if the crypto service is properly configured.
     *
     * @return true if encryption/decryption is available
     */
    public boolean isConfigured() {
        return cryptoService.isConfigured();
    }

    /**
     * Encrypts all marked entries in the tree panel.
     *
     * @param activeTab       the active file tab
     * @param treePanel       the tree panel containing marked entries
     * @param refreshCallback callback to refresh UI after operation
     * @param errorCallback   callback to display error messages
     * @param infoCallback    callback to display info messages
     */
    public void handleEncryptMarked(FileTab activeTab, TreePanel treePanel, Runnable refreshCallback,
            Consumer<String> errorCallback, Consumer<String> infoCallback) {
        handleMarkedOperation(true, activeTab, treePanel, refreshCallback, errorCallback, infoCallback);
    }

    /**
     * Decrypts all marked entries in the tree panel.
     *
     * @param activeTab       the active file tab
     * @param treePanel       the tree panel containing marked entries
     * @param refreshCallback callback to refresh UI after operation
     * @param errorCallback   callback to display error messages
     * @param infoCallback    callback to display info messages
     */
    public void handleDecryptMarked(FileTab activeTab, TreePanel treePanel, Runnable refreshCallback,
            Consumer<String> errorCallback, Consumer<String> infoCallback) {
        handleMarkedOperation(false, activeTab, treePanel, refreshCallback, errorCallback, infoCallback);
    }

    /**
     * Handles encryption or decryption of all marked entries.
     *
     * @param encrypt         true for encryption, false for decryption
     * @param activeTab       the active file tab
     * @param treePanel       the tree panel containing marked entries
     * @param refreshCallback callback to refresh UI after operation
     * @param errorCallback   callback to display error messages
     * @param infoCallback    callback to display info messages
     */
    private void handleMarkedOperation(boolean encrypt, FileTab activeTab, TreePanel treePanel,
            Runnable refreshCallback, Consumer<String> errorCallback, Consumer<String> infoCallback) {
        String operationName = encrypt ? "encryption" : "decryption";
        String operationVerb = encrypt ? ENCRYPTED_VERB : DECRYPTED_VERB;
        String skipReason = encrypt ? "already encrypted" : "not encrypted";

        if (activeTab == null) {
            errorCallback.accept(UIConstants.NO_ACTIVE_TAB_MSG);
            return;
        }

        if (!isConfigured()) {
            errorCallback.accept(UIConstants.CONFIGURE_PASSWORD_MSG);
            return;
        }

        List<ConfigEntry> markedEntries = treePanel.getMarkedEntries();
        if (markedEntries.isEmpty()) {
            infoCallback.accept("No entries marked for " + operationName);
            return;
        }

        int processedCount = 0;
        int skippedCount = 0;

        for (ConfigEntry entry : markedEntries) {
            boolean shouldProcess = encrypt ? !entry.isEncrypted() : entry.isEncrypted();
            if (shouldProcess) {
                try {
                    toggleEncryption(entry, activeTab.getEditor(), () -> {
                    });
                    processedCount++;
                } catch (Exception ex) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Error {} entry: {} - {}", operationName, entry.getKey(), ex.getMessage(), ex);
                    }
                }
            } else {
                skippedCount++;
            }
        }

        if (refreshCallback != null) {
            refreshCallback.run();
        }

        if (logger.isInfoEnabled()) {
            logger
                    .info("{} Marked: {} {}, {} skipped - env='{}' algo='{}' format='{}{}'",
                            encrypt ? "Encrypt" : "Decrypt", processedCount, operationVerb.toLowerCase(Locale.ROOT),
                            skippedCount, environmentManager.getActiveEnvironment(),
                            cryptoService.getCurrentAlgorithm(), cryptoService.getFormatPrefix(),
                            cryptoService.getFormatSuffix());
        }
        infoCallback
                .accept(operationVerb + " " + processedCount + " entries"
                        + (skippedCount > 0 ? " (" + skippedCount + " " + skipReason + ")" : ""));
    }

    /**
     * Handles double-click on a tree entry to toggle encryption.
     *
     * @param entry           the config entry that was double-clicked
     * @param activeTab       the active file tab
     * @param refreshCallback callback to refresh UI after operation
     * @param errorCallback   callback to display error messages
     */
    public void handleTreeDoubleClick(ConfigEntry entry, FileTab activeTab, Runnable refreshCallback,
            Consumer<String> errorCallback) {
        if (activeTab == null) {
            errorCallback.accept(UIConstants.NO_ACTIVE_TAB_MSG);
            return;
        }

        if (!isConfigured()) {
            errorCallback.accept(UIConstants.CONFIGURE_PASSWORD_MSG);
            return;
        }

        try {
            boolean encrypted = toggleEncryption(entry, activeTab.getEditor(), refreshCallback);
            if (logger.isInfoEnabled()) {
                logger
                        .info("{} key='{}' env='{}' algo='{}' format='{}{}'",
                                encrypted ? ENCRYPTED_VERB : DECRYPTED_VERB, entry.getKey(),
                                environmentManager.getActiveEnvironment(), cryptoService.getCurrentAlgorithm(),
                                cryptoService.getFormatPrefix(), cryptoService.getFormatSuffix());
            }
        } catch (Exception ex) {
            logger.error("Error toggling encryption via double-click", ex);
            errorCallback.accept("Error: " + ex.getMessage());
        }
    }
}
