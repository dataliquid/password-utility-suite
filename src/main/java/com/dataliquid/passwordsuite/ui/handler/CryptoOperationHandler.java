package com.dataliquid.passwordsuite.ui.handler;

import java.util.function.Consumer;

import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.operation.Operation;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.EditorService;
import com.dataliquid.passwordsuite.ui.FileTab;
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
    private final PasswordManager passwordManager;

    public CryptoOperationHandler(CryptoService cryptoService, EditorService editorService,
            PasswordManager passwordManager) {
        this.cryptoService = cryptoService;
        this.editorService = editorService;
        this.passwordManager = passwordManager;
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

        editorService.replaceValueInEditor(entry, oldValue, newValue, editor);

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
                                passwordManager.getActiveEnvironment(), cryptoService.getCurrentAlgorithm(),
                                cryptoService.getFormatPrefix(), cryptoService.getFormatSuffix());
            }
        } catch (Exception ex) {
            logger.error("Error toggling encryption via double-click", ex);
            errorCallback.accept("Error: " + ex.getMessage());
        }
    }
}
