package com.dataliquid.passwordsuite.ui.handler;

import java.awt.Frame;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.factory.CipherRegistry;
import com.dataliquid.passwordsuite.domain.ConfigTree;
import com.dataliquid.passwordsuite.service.CryptoService;
import com.dataliquid.passwordsuite.service.CryptoServiceFactory;
import com.dataliquid.passwordsuite.ui.ActionPanel;
import com.dataliquid.passwordsuite.ui.EnvironmentsDialog;
import com.dataliquid.passwordsuite.ui.FileTab;
import com.dataliquid.passwordsuite.ui.TabbedEditorPanel;
import com.dataliquid.passwordsuite.ui.TreePanel;

/**
 * Handles environment operations (Environments dialog, algorithm change,
 * environment change, tab change).
 * <p>
 * Extracted from MainFrame to follow Single Responsibility Principle.
 * </p>
 */
public class EnvironmentsHandler {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentsHandler.class);

    private final ActionPanel actionPanel;
    private final TreePanel treePanel;
    private final TabbedEditorPanel tabbedEditorPanel;
    private final CryptoService cryptoService;
    private final PasswordManager passwordManager;
    private final CryptoServiceFactory cryptoServiceFactory;
    private final CipherRegistry cipherRegistry;
    private final Frame parentFrame;
    private final BiConsumer<FileTab, String> autoParseCallback;
    private Runnable statusBarUpdateCallback;

    /**
     * Creates a new EnvironmentsHandler.
     *
     * @param actionPanel       the action panel with dropdowns
     * @param treePanel         the tree panel for configuration display
     * @param tabbedEditorPanel the tabbed editor panel
     * @param cryptoService     the crypto service
     * @param passwordManager   the password manager
     * @param cipherRegistry    the cipher registry
     * @param parentFrame       the parent frame for dialogs
     * @param autoParseCallback callback to auto-parse tab content
     */
    public EnvironmentsHandler(ActionPanel actionPanel, TreePanel treePanel, TabbedEditorPanel tabbedEditorPanel,
            CryptoService cryptoService, PasswordManager passwordManager, CipherRegistry cipherRegistry,
            Frame parentFrame, BiConsumer<FileTab, String> autoParseCallback) {
        this.actionPanel = actionPanel;
        this.treePanel = treePanel;
        this.tabbedEditorPanel = tabbedEditorPanel;
        this.cryptoService = cryptoService;
        this.passwordManager = passwordManager;
        this.cryptoServiceFactory = new CryptoServiceFactory(cipherRegistry);
        this.cipherRegistry = cipherRegistry;
        this.parentFrame = parentFrame;
        this.autoParseCallback = autoParseCallback;
    }

    /**
     * Sets the callback to be invoked when environments are changed.
     *
     * @param callback the callback to update the status bar
     */
    public void setStatusBarUpdateCallback(Runnable callback) {
        this.statusBarUpdateCallback = callback;
    }

    /**
     * Opens the environments dialog and handles the result.
     *
     * @param errorCallback callback to display error messages
     * @param infoCallback  callback to display info messages
     */
    public void handleEnvironments(Consumer<String> errorCallback, Consumer<String> infoCallback) {
        EnvironmentsDialog dialog = new EnvironmentsDialog(parentFrame, passwordManager.getEnvironmentsCopy(),
                passwordManager.getActiveEnvironment(), cipherRegistry);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            // Update password manager with new environments
            passwordManager.updateFromDialog(dialog.getEnvironments(), dialog.getSelectedEnvironment());

            // Synchronize environment dropdown
            actionPanel.updateEnvironments(passwordManager.getEnvironmentNames());
            if (passwordManager.getActiveEnvironment() != null) {
                actionPanel.setSelectedEnvironment(passwordManager.getActiveEnvironment());
            }

            // Configure CryptoService with password and format from active environment
            try {
                updateCryptoServicePassword();

                // Set default algorithm from active environment (only if environments exist)
                if (passwordManager.hasEnvironments()) {
                    String defaultAlgorithm = passwordManager.getActiveDefaultAlgorithm();
                    actionPanel.setSelectedAlgorithm(defaultAlgorithm);
                    cryptoService.setAlgorithm(defaultAlgorithm);
                    // Also save in active tab
                    FileTab activeTab = tabbedEditorPanel.getActiveTab();
                    if (activeTab != null) {
                        activeTab.setSelectedAlgorithm(defaultAlgorithm);
                    }
                } else {
                    // No environments - clear algorithm selection
                    actionPanel.clearAlgorithmSelection();
                    FileTab activeTab = tabbedEditorPanel.getActiveTab();
                    if (activeTab != null) {
                        activeTab.setSelectedAlgorithm(null);
                    }
                }

                logger.info("CryptoService configured with password, format and algorithm");
            } catch (Exception e) {
                logger.error("Failed to configure CryptoService", e);
                errorCallback.accept("Failed to configure encryption: " + e.getMessage());
            }

            if (logger.isInfoEnabled()) {
                logger
                        .info("Environments updated - {} environments configured, active: {}",
                                passwordManager.getEnvironmentCount(), passwordManager.getActiveEnvironment());
            }

            // Update status bar with new environment count
            if (statusBarUpdateCallback != null) {
                statusBarUpdateCallback.run();
            }

            // Trigger environment change handling to re-parse with new format
            // (setSelectedEnvironment suppresses events, so we call it explicitly)
            handleEnvironmentChange();
        }
    }

    /**
     * Handles algorithm selection change.
     */
    public void handleAlgorithmChange() {
        FileTab activeTab = tabbedEditorPanel.getActiveTab();
        if (activeTab != null) {
            String selected = actionPanel.getSelectedAlgorithm();
            activeTab.setSelectedAlgorithm(selected);

            // Skip if no algorithm selected (null or placeholder)
            if (selected == null || selected.isEmpty()) {
                logger.debug("No algorithm selected");
                return;
            }

            // Update CryptoService to use the selected algorithm
            try {
                cryptoService.setAlgorithm(selected);
                if (logger.isInfoEnabled()) {
                    logger
                            .info("Algorithm changed to: '{}' env='{}' for tab: {}", selected,
                                    passwordManager.getActiveEnvironment(), activeTab.getTabTitle());
                }
            } catch (CryptoException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to switch algorithm to: " + selected, e);
                }
                JOptionPane
                        .showMessageDialog(parentFrame, "Failed to switch algorithm: " + e.getMessage(),
                                "Algorithm Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles environment selection change.
     */
    public void handleEnvironmentChange() {
        String selected = actionPanel.getSelectedEnvironment();
        logger.debug("Environment change requested: {}", selected);

        // Update password manager first
        passwordManager.setActiveEnvironment(selected);

        // Update CryptoService with new password and format from selected environment
        if (selected != null) {
            try {
                updateCryptoServicePassword();
            } catch (CryptoException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to update CryptoService for environment: " + selected, e);
                }
            }
        }

        // Update algorithm to environment's default (only if environment exists)
        if (passwordManager.hasEnvironments() && selected != null && !"No Environment".equals(selected)) {
            String defaultAlgorithm = passwordManager.getActiveDefaultAlgorithm();
            actionPanel.setSelectedAlgorithm(defaultAlgorithm);
            try {
                cryptoService.setAlgorithm(defaultAlgorithm);
            } catch (CryptoException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to set default algorithm: " + defaultAlgorithm, e);
                }
            }
        } else {
            // No environment - clear algorithm selection
            logger.info("Clearing algorithm selection - no environment");
            actionPanel.clearAlgorithmSelection();
        }
        String defaultAlgorithm = passwordManager.hasEnvironments() ? passwordManager.getActiveDefaultAlgorithm()
                : null;

        // Update active tab if exists
        FileTab activeTab = tabbedEditorPanel.getActiveTab();
        if (activeTab != null) {
            activeTab.setSelectedEnvironment(selected);
            // Also save the algorithm in the tab
            activeTab.setSelectedAlgorithm(defaultAlgorithm);

            // Re-parse active tab with new format settings
            if (autoParseCallback != null) {
                autoParseCallback.accept(activeTab, activeTab.getText());
            }
        }

        if (logger.isInfoEnabled()) {
            logger
                    .info("Environment switched to: '{}' algo='{}' format='{}{}'", selected, defaultAlgorithm,
                            passwordManager.getActiveFormatPrefix(), passwordManager.getActiveFormatSuffix());
        }
    }

    /**
     * Handles tab change event - synchronizes tree with active tab.
     */
    public void handleTabChange() {
        FileTab activeTab = tabbedEditorPanel.getActiveTab();
        if (activeTab != null) {
            ConfigTree tree = activeTab.getConfigTree();
            if (tree != null) {
                treePanel.loadConfigTree(tree);
            } else {
                treePanel.clear();
            }

            // Sync dropdowns with tab's saved values
            String tabAlgorithm = activeTab.getSelectedAlgorithm();
            String tabEnvironment = activeTab.getSelectedEnvironment();

            // If tab has no algorithm/environment yet, inherit from current dropdown
            // selection
            if (tabAlgorithm == null) {
                tabAlgorithm = actionPanel.getSelectedAlgorithm();
                if (tabAlgorithm != null) {
                    activeTab.setSelectedAlgorithm(tabAlgorithm);
                }
            }
            if (tabEnvironment == null || "No Environment".equals(tabEnvironment)) {
                tabEnvironment = actionPanel.getSelectedEnvironment();
                if (tabEnvironment != null && !"No Environment".equals(tabEnvironment)) {
                    activeTab.setSelectedEnvironment(tabEnvironment);
                }
            }

            if (logger.isDebugEnabled()) {
                logger
                        .debug("Tab change: syncing algo='{}' env='{}' for tab: {}", tabAlgorithm, tabEnvironment,
                                activeTab.getTabTitle());
            }
            actionPanel.setSelectedAlgorithm(tabAlgorithm);
            actionPanel.setSelectedEnvironment(tabEnvironment);

            // Register auto-parse callback for this tab
            if (autoParseCallback != null) {
                activeTab.setParseCallback(() -> {
                    autoParseCallback.accept(activeTab, activeTab.getText());
                });
            }
        }
    }

    /**
     * Updates the CryptoService with the password and format from the active
     * environment. Uses CryptoServiceFactory to avoid code duplication.
     *
     * @throws CryptoException if password update fails
     */
    private void updateCryptoServicePassword() throws CryptoException {
        if (passwordManager.hasEnvironments()) {
            cryptoServiceFactory.updatePassword(cryptoService, passwordManager.getActivePassword());
            cryptoService.setFormat(passwordManager.getActiveFormatPrefix(), passwordManager.getActiveFormatSuffix());
        }
    }
}
