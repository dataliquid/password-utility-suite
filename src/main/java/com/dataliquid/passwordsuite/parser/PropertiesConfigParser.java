package com.dataliquid.passwordsuite.parser;

import com.dataliquid.passwordsuite.crypto.config.FormatConfig;
import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigGroup;
import com.dataliquid.passwordsuite.domain.ConfigTree;

/**
 * Parser for Java Properties configuration format. Supports dot-notation for
 * hierarchical structures (e.g., database.host=localhost).
 */
public class PropertiesConfigParser implements ConfigParser {

    private final String formatPrefix;
    private final String formatSuffix;

    /**
     * Creates a parser with default format (ENC[...]).
     */
    public PropertiesConfigParser() {
        this(FormatConfig.DEFAULT_PREFIX, FormatConfig.DEFAULT_SUFFIX);
    }

    /**
     * Creates a parser with custom format.
     *
     * @param formatPrefix the prefix for encrypted values
     * @param formatSuffix the suffix for encrypted values
     */
    public PropertiesConfigParser(String formatPrefix, String formatSuffix) {
        this.formatPrefix = formatPrefix != null ? formatPrefix : FormatConfig.DEFAULT_PREFIX;
        this.formatSuffix = formatSuffix != null ? formatSuffix : FormatConfig.DEFAULT_SUFFIX;
    }

    @Override
    public ConfigTree parse(String content) throws ParseException {
        if (content == null || content.isBlank()) {
            throw new ParseException("Content cannot be null or empty");
        }

        ConfigTree tree = new ConfigTree(ConfigFormat.PROPERTIES);
        String[] lines = content.split("\n");

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }

            // Parse key=value or key:value
            int separatorIndex = findSeparator(line);
            if (separatorIndex == -1) {
                continue; // Invalid line, skip
            }

            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();

            // Remove inline comments from value
            int commentIndex = value.indexOf('#');
            if (commentIndex != -1) {
                value = value.substring(0, commentIndex).trim();
            }

            addPropertyToTree(tree.getRoot(), key, value, lineNum);
        }

        return tree;
    }

    /**
     * Finds the first unescaped separator (= or :) in the line.
     */
    private int findSeparator(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '=' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ConfigFormat getSupportedFormat() {
        return ConfigFormat.PROPERTIES;
    }

    /**
     * Adds a property to the tree, creating nested groups for dot-notation keys.
     * Example: "database.host" creates a group "database" with entry "host".
     */
    private void addPropertyToTree(ConfigGroup root, String key, String value, int lineNum) {
        if (!key.contains(".")) {
            // Simple key without dots -> direct entry
            ConfigEntry entry = new ConfigEntry(key, value);
            entry.setLineNumber(lineNum);
            if (value.startsWith(formatPrefix) && value.endsWith(formatSuffix)) {
                entry.setEncrypted(true);
                entry.setMarkedForEncryption(true);
            }
            root.addChild(entry);
            return;
        }

        // Split key by dots and create nested groups
        String[] parts = key.split("\\.", 2);
        String firstKey = parts[0];
        String remaining = parts[1];

        // Find or create the group for the first key
        ConfigGroup group = (ConfigGroup) root.findChildByKey(firstKey).orElseGet(() -> {
            ConfigGroup newGroup = new ConfigGroup(firstKey);
            root.addChild(newGroup);
            return newGroup;
        });

        // Recursively add the remaining path
        addPropertyToTree(group, remaining, value, lineNum);
    }
}
