package com.dataliquid.passwordsuite.parser;

import com.dataliquid.passwordsuite.domain.ConfigFormat;

/**
 * Factory for creating appropriate ConfigParser based on content
 * auto-detection.
 */
public class ParserFactory {

    /**
     * Returns a parser appropriate for the given content with default format.
     * <p>
     * Performs simple heuristic-based detection:
     * </p>
     * <ul>
     * <li>YAML: starts with "---" or contains ": " (key: value pattern)</li>
     * <li>Otherwise: Properties format</li>
     * </ul>
     *
     * @param  content the configuration content
     *
     * @return         an appropriate ConfigParser
     */
    public static ConfigParser getParser(String content) {
        return getParser(content, null, null);
    }

    /**
     * Returns a parser appropriate for the given content with custom format.
     *
     * @param  content      the configuration content
     * @param  formatPrefix the prefix for encrypted values (e.g., "ENC[")
     * @param  formatSuffix the suffix for encrypted values (e.g., "]")
     *
     * @return              an appropriate ConfigParser
     */
    public static ConfigParser getParser(String content, String formatPrefix, String formatSuffix) {
        if (content == null || content.isBlank()) {
            // Default to YAML for empty content
            return new YamlConfigParser(formatPrefix, formatSuffix);
        }

        String trimmed = content.trim();

        // Check for YAML indicators
        if (trimmed.startsWith("---")) {
            return new YamlConfigParser(formatPrefix, formatSuffix);
        }

        // Check for YAML-style key: value (with space after colon)
        if (trimmed.contains(": ") || trimmed.contains(":\n")) {
            return new YamlConfigParser(formatPrefix, formatSuffix);
        }

        // Check for Properties-style key=value
        if (trimmed.contains("=")) {
            return new PropertiesConfigParser(formatPrefix, formatSuffix);
        }

        // Default to YAML
        return new YamlConfigParser(formatPrefix, formatSuffix);
    }

    /**
     * Returns a parser for the specified format with default format settings.
     *
     * @param  content the configuration content
     * @param  format  the configuration format
     *
     * @return         an appropriate ConfigParser
     */
    public static ConfigParser getParser(String content, ConfigFormat format) {
        switch (format) {
        case YAML:
            return new YamlConfigParser(null, null);
        case PROPERTIES:
            return new PropertiesConfigParser(null, null);
        default:
            return getParser(content, null, null);
        }
    }
}
