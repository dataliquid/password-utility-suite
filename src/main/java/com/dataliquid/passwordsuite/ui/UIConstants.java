package com.dataliquid.passwordsuite.ui;

import com.dataliquid.passwordsuite.environment.EnvironmentManager;

/**
 * Centralized UI constants to avoid magic strings throughout the codebase.
 */
public final class UIConstants {

    /** Default environment label when no environment is selected. */
    public static final String NO_ENVIRONMENT = EnvironmentManager.NO_ENVIRONMENT;

    /** Status bar label when no file is open. */
    public static final String NO_FILE = "No file";

    /** Error message when no active tab is available. */
    public static final String NO_ACTIVE_TAB_MSG = "No active tab";

    /** Error message when crypto service is not configured. */
    public static final String CONFIGURE_PASSWORD_MSG = "Please configure master password and select algorithm in Environments";

    private UIConstants() {
        // Prevent instantiation
    }
}
