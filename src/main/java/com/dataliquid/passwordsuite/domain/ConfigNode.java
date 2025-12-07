package com.dataliquid.passwordsuite.domain;

/**
 * Abstract base class for configuration tree nodes. Implements composite
 * pattern for hierarchical configuration structure.
 */
public abstract class ConfigNode {

    protected String key;
    protected ConfigNode parent;

    public ConfigNode() {
    }

    public ConfigNode(String key) {
        this.key = key;
    }

    /**
     * Returns the key/name of this node.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key/name of this node.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Returns the parent node, or null if this is the root.
     */
    public ConfigNode getParent() {
        return parent;
    }

    /**
     * Sets the parent node.
     */
    public void setParent(ConfigNode parent) {
        this.parent = parent;
    }

    /**
     * Returns true if this node is a leaf (has no children).
     */
    public abstract boolean isLeaf();

    /**
     * Creates a deep copy of this node for undo/redo support.
     */
    public abstract ConfigNode deepCopy();

    /**
     * Returns the full path from root to this node (e.g., "database.host").
     */
    public String getPath() {
        if (parent == null || parent.getKey() == null || "root".equals(parent.getKey())) {
            return key;
        }
        return parent.getPath() + "." + key;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{key='" + key + "'}";
    }
}
