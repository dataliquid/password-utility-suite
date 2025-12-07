package com.dataliquid.passwordsuite.parser;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.dataliquid.passwordsuite.crypto.config.FormatConfig;
import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigGroup;
import com.dataliquid.passwordsuite.domain.ConfigTree;

/**
 * Parser for YAML configuration format. Uses SnakeYAML library to parse YAML
 * content.
 */
public class YamlConfigParser implements ConfigParser {

    private final Yaml yaml = new Yaml();
    private final String formatPrefix;
    private final String formatSuffix;

    /**
     * Creates a parser with default format (ENC[...]).
     */
    public YamlConfigParser() {
        this(FormatConfig.DEFAULT_PREFIX, FormatConfig.DEFAULT_SUFFIX);
    }

    /**
     * Creates a parser with custom format.
     *
     * @param formatPrefix the prefix for encrypted values
     * @param formatSuffix the suffix for encrypted values
     */
    public YamlConfigParser(String formatPrefix, String formatSuffix) {
        this.formatPrefix = formatPrefix != null ? formatPrefix : FormatConfig.DEFAULT_PREFIX;
        this.formatSuffix = formatSuffix != null ? formatSuffix : FormatConfig.DEFAULT_SUFFIX;
    }

    @Override
    public ConfigTree parse(String content) throws ParseException {
        if (content == null || content.isBlank()) {
            throw new ParseException("Content cannot be null or empty");
        }

        try {
            Object data = yaml.load(content);

            if (!(data instanceof Map)) {
                throw new ParseException("YAML root must be a map/object");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rootMap = (Map<String, Object>) data;

            ConfigTree tree = new ConfigTree(ConfigFormat.YAML);
            buildTree(tree.getRoot(), rootMap);

            // Post-process: Assign line numbers by scanning content
            assignLineNumbers(tree.getRoot(), content);

            return tree;

        } catch (Exception e) {
            throw new ParseException("Failed to parse YAML content: " + e.getMessage(), e);
        }
    }

    @Override
    public ConfigFormat getSupportedFormat() {
        return ConfigFormat.YAML;
    }

    /**
     * Recursively builds the configuration tree from a YAML map. Note: Object
     * instantiation in loop is intentional - each entry needs its own instance.
     */
    @SuppressWarnings({ "unchecked", "PMD.AvoidInstantiatingObjectsInLoops" })
    private void buildTree(ConfigGroup parent, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // Nested object -> create ConfigGroup
                ConfigGroup group = new ConfigGroup(key);
                parent.addChild(group);
                buildTree(group, (Map<String, Object>) value);

            } else {
                // Leaf value -> create ConfigEntry
                String valueStr = value != null ? value.toString() : "";
                ConfigEntry configEntry = new ConfigEntry(key, valueStr);

                // Check if value is encrypted using configurable format
                if (valueStr.startsWith(formatPrefix) && valueStr.endsWith(formatSuffix)) {
                    configEntry.setEncrypted(true);
                    configEntry.setMarkedForEncryption(true);
                }

                parent.addChild(configEntry);
            }
        }
    }

    /**
     * Post-processes the tree to assign line numbers by scanning the content. This
     * is a best-effort approach for YAML since SnakeYAML doesn't provide line info
     * directly.
     */
    private void assignLineNumbers(ConfigGroup group, String content) {
        String[] lines = content.split("\n");
        Set<Integer> usedLines = new HashSet<>();
        assignLineNumbersRecursive(group, lines, usedLines);
    }

    /**
     * Recursively assigns line numbers while tracking which lines have already been
     * used. This ensures that duplicate keys in different sections get assigned to
     * different lines.
     */
    private void assignLineNumbersRecursive(ConfigGroup group, String[] lines, Set<Integer> usedLines) {
        for (com.dataliquid.passwordsuite.domain.ConfigNode node : group.getChildren()) {
            if (node instanceof ConfigEntry) {
                ConfigEntry entry = (ConfigEntry) node;
                int lineNum = findLineNumber(entry.getKey(), entry.getValue(), lines, usedLines);
                entry.setLineNumber(lineNum);
                if (lineNum >= 0) {
                    usedLines.add(lineNum);
                }
            } else if (node instanceof ConfigGroup) {
                assignLineNumbersRecursive((ConfigGroup) node, lines, usedLines);
            }
        }
    }

    /**
     * Finds the line number for a YAML key-value pair. Searches for lines
     * containing "key: value" with matching value. Skips lines that have already
     * been assigned to other entries. Falls back to just matching "key:" if exact
     * match not found.
     */
    private int findLineNumber(String key, String value, String[] lines, Set<Integer> usedLines) {
        // First pass: Try to find exact match with both key and value
        for (int i = 0; i < lines.length; i++) {
            if (usedLines.contains(i)) {
                continue; // Skip lines already assigned
            }
            String line = lines[i].trim();
            if (line.startsWith(key + ":")) {
                // Extract the value part after "key:"
                String lineValue = line.substring(key.length() + 1).trim();
                // Check if the value matches (handle different formats)
                if (lineValue.equals(value) || lineValue.startsWith(value) || lineValue.contains(value)) {
                    return i;
                }
            }
        }

        // Second pass: Fall back to just matching the key if exact match not found
        // This helps with multi-line values or complex cases
        for (int i = 0; i < lines.length; i++) {
            if (usedLines.contains(i)) {
                continue; // Skip lines already assigned
            }
            String line = lines[i].trim();
            if (line.startsWith(key + ":")) {
                return i;
            }
        }

        return -1; // Not found
    }
}
