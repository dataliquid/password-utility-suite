package com.dataliquid.passwordsuite.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Returns the paths of all entries marked for encryption. Together with
     * {@link #applyMarkedPaths(Set)} this allows preserving markers across
     * re-parsing.
     */
    public Set<String> getMarkedPaths() {
        Set<String> paths = new HashSet<>();
        for (ConfigEntry entry : getAllEntries()) {
            if (entry.isMarkedForEncryption()) {
                paths.add(entry.getPath());
            }
        }
        return paths;
    }

    /**
     * Marks all entries whose path is contained in the given set for encryption.
     */
    public void applyMarkedPaths(Set<String> paths) {
        for (ConfigEntry entry : getAllEntries()) {
            if (paths.contains(entry.getPath())) {
                entry.setMarkedForEncryption(true);
            }
        }
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
