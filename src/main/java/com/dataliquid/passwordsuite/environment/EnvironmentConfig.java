package com.dataliquid.passwordsuite.environment;

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
    private final String formatPrefix;
    private final String formatSuffix;
    private final String defaultAlgorithm;
    private final String pattern;

    /**
     * Creates an environment configuration with default format settings.
     *
     * @param password the master password for this environment
     */
    @SuppressWarnings("PMD.UseVarargs")
    public EnvironmentConfig(char[] password) {
        this(password, FormatConfig.DEFAULT_PREFIX, FormatConfig.DEFAULT_SUFFIX, DEFAULT_ALGORITHM, null);
    }

    /**
     * Creates an environment configuration with custom format and algorithm
     * settings and no filename pattern.
     *
     * @param password         the master password for this environment
     * @param formatPrefix     the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix     the suffix for encrypted values (e.g., "]")
     * @param defaultAlgorithm the default encryption algorithm
     */
    public EnvironmentConfig(char[] password, String formatPrefix, String formatSuffix, String defaultAlgorithm) {
        this(password, formatPrefix, formatSuffix, defaultAlgorithm, null);
    }

    /**
     * Creates an environment configuration with custom format, algorithm and
     * filename pattern settings.
     *
     * @param password         the master password for this environment
     * @param formatPrefix     the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix     the suffix for encrypted values (e.g., "]")
     * @param defaultAlgorithm the default encryption algorithm
     * @param pattern          optional regex matched against the absolute file path
     *                         to auto-detect this environment, or null
     */
    public EnvironmentConfig(char[] password, String formatPrefix, String formatSuffix, String defaultAlgorithm,
            String pattern) {
        this.password = password != null ? Arrays.copyOf(password, password.length) : new char[0];
        this.formatPrefix = formatPrefix != null ? formatPrefix : FormatConfig.DEFAULT_PREFIX;
        this.formatSuffix = formatSuffix != null ? formatSuffix : FormatConfig.DEFAULT_SUFFIX;
        this.defaultAlgorithm = defaultAlgorithm != null ? defaultAlgorithm : DEFAULT_ALGORITHM;
        this.pattern = pattern;
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
     * Gets the format prefix for encrypted values.
     *
     * @return the format prefix
     */
    public String getFormatPrefix() {
        return formatPrefix;
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
     * Gets the default encryption algorithm.
     *
     * @return the default algorithm
     */
    public String getDefaultAlgorithm() {
        return defaultAlgorithm;
    }

    /**
     * Gets the filename pattern for auto-detecting this environment.
     *
     * @return the regex pattern, or null if not set
     */
    public String getPattern() {
        return pattern;
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
