package com.dataliquid.passwordsuite.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a composite node in the configuration tree (a group of child
 * nodes). Implements the composite pattern.
 */
public class ConfigGroup extends ConfigNode {

    private final List<ConfigNode> children = new ArrayList<>();

    public ConfigGroup() {
        super();
    }

    public ConfigGroup(String key) {
        super(key);
    }

    /**
     * Adds a child node to this group.
     */
    public void addChild(ConfigNode child) {
        if (child != null) {
            child.setParent(this);
            children.add(child);
        }
    }

    /**
     * Removes a child node from this group.
     */
    public void removeChild(ConfigNode child) {
        children.remove(child);
        if (child != null) {
            child.setParent(null);
        }
    }

    /**
     * Returns an unmodifiable list of child nodes.
     */
    public List<ConfigNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns the number of children.
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Finds a child node by key.
     */
    public Optional<ConfigNode> findChildByKey(String key) {
        return children.stream().filter(child -> key.equals(child.getKey())).findFirst();
    }

    /**
     * Finds a node by path (e.g., "database.host"). Supports dot-separated paths
     * for nested nodes.
     */
    public Optional<ConfigNode> findByPath(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }

        int dotIndex = path.indexOf('.');
        boolean hasRemainingPath = dotIndex >= 0;
        String firstKey = hasRemainingPath ? path.substring(0, dotIndex) : path;

        Optional<ConfigNode> child = findChildByKey(firstKey);
        if (child.isEmpty()) {
            return Optional.empty();
        }

        if (!hasRemainingPath) {
            return child;
        }

        // Continue searching in child if it's a group
        String remainingPath = path.substring(dotIndex + 1);
        if (child.get() instanceof ConfigGroup) {
            return ((ConfigGroup) child.get()).findByPath(remainingPath);
        }

        return Optional.empty();
    }

    /**
     * Collects all ConfigEntry leaf nodes recursively.
     */
    public List<ConfigEntry> getAllEntries() {
        List<ConfigEntry> entries = new ArrayList<>();
        collectEntries(entries);
        return entries;
    }

    private void collectEntries(List<ConfigEntry> entries) {
        for (ConfigNode child : children) {
            if (child instanceof ConfigEntry) {
                entries.add((ConfigEntry) child);
            } else if (child instanceof ConfigGroup) {
                ((ConfigGroup) child).collectEntries(entries);
            }
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public ConfigGroup deepCopy() {
        ConfigGroup copy = new ConfigGroup(this.key);
        for (ConfigNode child : children) {
            copy.addChild(child.deepCopy());
        }
        return copy;
    }

    @Override
    public String toString() {
        return key + " (" + children.size() + " items)";
    }
}
