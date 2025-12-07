package com.dataliquid.passwordsuite.domain;

/**
 * Represents the format of configuration data.
 */
public enum ConfigFormat {
    /**
     * YAML format (e.g., key: value)
     */
    YAML,

    /**
     * Properties format (e.g., key=value)
     */
    PROPERTIES,

    /**
     * Unknown or auto-detected format
     */
    UNKNOWN
}
