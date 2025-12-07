package com.dataliquid.passwordsuite.ui.handler;

import java.util.Arrays;

import com.dataliquid.passwordsuite.crypto.config.FormatConfig;

/**
 * Configuration for a single environment including password and encryption
 * format settings.
 */
public class EnvironmentConfig {

    /** Default format prefix for encrypted values. Delegates to FormatConfig. */
    public static final String DEFAULT_FORMAT_PREFIX = FormatConfig.DEFAULT_PREFIX;

    /** Default format suffix for encrypted values. Delegates to FormatConfig. */
    public static final String DEFAULT_FORMAT_SUFFIX = FormatConfig.DEFAULT_SUFFIX;

    /** Default algorithm for encryption. */
    public static final String DEFAULT_ALGORITHM = "AES-256-GCM";

    private char[] password;
    private String formatPrefix;
    private String formatSuffix;
    private String defaultAlgorithm;

    /**
     * Creates an environment configuration with default format settings.
     *
     * @param password the master password for this environment
     */
    @SuppressWarnings("PMD.UseVarargs")
    public EnvironmentConfig(char[] password) {
        this(password, FormatConfig.DEFAULT_PREFIX, FormatConfig.DEFAULT_SUFFIX, DEFAULT_ALGORITHM);
    }

    /**
     * Creates an environment configuration with custom format settings.
     *
     * @param password     the master password for this environment
     * @param formatPrefix the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix the suffix for encrypted values (e.g., "]")
     */
    public EnvironmentConfig(char[] password, String formatPrefix, String formatSuffix) {
        this(password, formatPrefix, formatSuffix, DEFAULT_ALGORITHM);
    }

    /**
     * Creates an environment configuration with custom format and algorithm
     * settings.
     *
     * @param password         the master password for this environment
     * @param formatPrefix     the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix     the suffix for encrypted values (e.g., "]")
     * @param defaultAlgorithm the default encryption algorithm
     */
    public EnvironmentConfig(char[] password, String formatPrefix, String formatSuffix, String defaultAlgorithm) {
        this.password = password != null ? Arrays.copyOf(password, password.length) : new char[0];
        this.formatPrefix = formatPrefix != null ? formatPrefix : FormatConfig.DEFAULT_PREFIX;
        this.formatSuffix = formatSuffix != null ? formatSuffix : FormatConfig.DEFAULT_SUFFIX;
        this.defaultAlgorithm = defaultAlgorithm != null ? defaultAlgorithm : DEFAULT_ALGORITHM;
    }

    /**
     * Gets a copy of the password for this environment.
     *
     * @return a copy of the password as char array
     */
    public char[] getPassword() {
        return password != null ? Arrays.copyOf(password, password.length) : new char[0];
    }

    /**
     * Sets the password for this environment.
     *
     * @param password the new password
     */
    @SuppressWarnings("PMD.UseVarargs")
    public void setPassword(char[] password) {
        // Clear old password
        if (this.password != null) {
            Arrays.fill(this.password, ' ');
        }
        this.password = password != null ? Arrays.copyOf(password, password.length) : new char[0];
    }

    /**
     * Gets the format prefix for encrypted values.
     *
     * @return the format prefix
     */
    public String getFormatPrefix() {
        return formatPrefix;
    }

    /**
     * Sets the format prefix for encrypted values.
     *
     * @param formatPrefix the format prefix
     */
    public void setFormatPrefix(String formatPrefix) {
        this.formatPrefix = formatPrefix != null ? formatPrefix : FormatConfig.DEFAULT_PREFIX;
    }

    /**
     * Gets the format suffix for encrypted values.
     *
     * @return the format suffix
     */
    public String getFormatSuffix() {
        return formatSuffix;
    }

    /**
     * Sets the format suffix for encrypted values.
     *
     * @param formatSuffix the format suffix
     */
    public void setFormatSuffix(String formatSuffix) {
        this.formatSuffix = formatSuffix != null ? formatSuffix : FormatConfig.DEFAULT_SUFFIX;
    }

    /**
     * Gets the default encryption algorithm.
     *
     * @return the default algorithm
     */
    public String getDefaultAlgorithm() {
        return defaultAlgorithm;
    }

    /**
     * Sets the default encryption algorithm.
     *
     * @param defaultAlgorithm the default algorithm
     */
    public void setDefaultAlgorithm(String defaultAlgorithm) {
        this.defaultAlgorithm = defaultAlgorithm != null ? defaultAlgorithm : DEFAULT_ALGORITHM;
    }

    /**
     * Clears the password from memory by overwriting with spaces.
     */
    public void cleanup() {
        if (password != null) {
            Arrays.fill(password, ' ');
            password = new char[0];
        }
    }
}
