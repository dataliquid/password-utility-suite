package com.dataliquid.passwordsuite.domain;

import java.util.List;
import java.util.Optional;

/**
 * Represents the root container for a configuration tree. Contains the root
 * group and metadata about the configuration format.
 */
public class ConfigTree {

    private final ConfigGroup root;
    private ConfigFormat format;

    public ConfigTree(ConfigFormat format) {
        this.root = new ConfigGroup("root");
        this.format = format;
    }

    /**
     * Returns the root group of this tree.
     */
    public ConfigGroup getRoot() {
        return root;
    }

    /**
     * Returns the format of this configuration (YAML, PROPERTIES, etc.).
     */
    public ConfigFormat getFormat() {
        return format;
    }

    /**
     * Sets the format of this configuration.
     */
    public void setFormat(ConfigFormat format) {
        this.format = format;
    }

    /**
     * Finds a node by path (e.g., "database.host").
     */
    public Optional<ConfigNode> findByPath(String path) {
        return root.findByPath(path);
    }

    /**
     * Returns all ConfigEntry leaf nodes in the tree.
     */
    public List<ConfigEntry> getAllEntries() {
        return root.getAllEntries();
    }

    /**
     * Returns the number of entries in the tree.
     */
    public int getEntryCount() {
        return getAllEntries().size();
    }

    /**
     * Creates a deep copy of this tree.
     */
    public ConfigTree deepCopy() {
        ConfigTree copy = new ConfigTree(this.format);
        for (ConfigNode child : root.getChildren()) {
            copy.root.addChild(child.deepCopy());
        }
        return copy;
    }

    @Override
    public String toString() {
        return "ConfigTree{format=" + format + ", entries=" + getEntryCount() + "}";
    }
}
