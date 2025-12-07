package com.dataliquid.passwordsuite.crypto.config;

/**
 * Constants for encryption format markers. Centralizes format configuration to
 * avoid dependency pollution between layers.
 */
public final class FormatConfig {

    /** Default format prefix for encrypted values. */
    public static final String DEFAULT_PREFIX = "ENC[";

    /** Default format suffix for encrypted values. */
    public static final String DEFAULT_SUFFIX = "]";

    /** MuleSoft format prefix for encrypted values. */
    public static final String MULESOFT_PREFIX = "![";

    /** MuleSoft format suffix for encrypted values. */
    public static final String MULESOFT_SUFFIX = "]";

    private FormatConfig() {
        // Utility class - prevent instantiation
    }
}
