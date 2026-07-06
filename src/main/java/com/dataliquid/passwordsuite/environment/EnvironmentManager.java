package com.dataliquid.passwordsuite.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages environment configurations securely including passwords and format
 * settings.
 * <p>
 * Provides secure cleanup to prevent passwords from lingering in memory.
 * </p>
 */
public class EnvironmentManager {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentManager.class);
    private static final String NO_ENVIRONMENT = "No Environment";

    private final Map<String, EnvironmentConfig> environments = new ConcurrentHashMap<>();
    private String activeEnvironment;

    /**
     * Clears all passwords from memory by overwriting with spaces.
     */
    public void cleanup() {
        for (EnvironmentConfig config : environments.values()) {
            if (config != null) {
                config.cleanup();
            }
        }
        environments.clear();
        clearActiveEnvironment();
        logger.debug("Passwords cleared from memory");
    }

    /**
     * Clears the active environment reference.
     */
    @SuppressWarnings("PMD.NullAssignment")
    private void clearActiveEnvironment() {
        this.activeEnvironment = null;
    }

    /**
     * Updates environments from dialog settings. Cleans up existing configurations
     * before storing new ones.
     *
     * @param newEnvironments     the new environment configuration map
     * @param selectedEnvironment the active environment name
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void updateFromDialog(Map<String, EnvironmentConfig> newEnvironments, String selectedEnvironment) {
        cleanup();

        for (Map.Entry<String, EnvironmentConfig> entry : newEnvironments.entrySet()) {
            EnvironmentConfig config = entry.getValue();
            EnvironmentConfig configCopy = new EnvironmentConfig(config.getPassword(), config.getFormatPrefix(),
                    config.getFormatSuffix(), config.getDefaultAlgorithm());
            environments.put(entry.getKey(), configCopy);
        }

        activeEnvironment = selectedEnvironment;
        if (logger.isInfoEnabled()) {
            logger.info("Updated {} environments, active: {}", environments.size(), activeEnvironment);
        }
    }

    /**
     * Gets the password for the active environment or the first available.
     *
     * @return the password as char array, or empty array if no environments
     *         configured
     */
    public char[] getActivePassword() {
        EnvironmentConfig config = getActiveConfig();
        return config != null ? config.getPassword() : new char[0];
    }

    /**
     * Gets the configuration for the active environment or the first available.
     *
     * @return the environment configuration, or null if no environments configured
     */
    public EnvironmentConfig getActiveConfig() {
        if (environments.isEmpty()) {
            return null;
        }

        if (activeEnvironment != null && environments.containsKey(activeEnvironment)) {
            return environments.get(activeEnvironment);
        }

        return environments.values().iterator().next();
    }

    /**
     * Gets the format prefix for the active environment.
     *
     * @return the format prefix, or default if no environment configured
     */
    public String getActiveFormatPrefix() {
        EnvironmentConfig config = getActiveConfig();
        return config != null ? config.getFormatPrefix() : EnvironmentConfig.DEFAULT_FORMAT_PREFIX;
    }

    /**
     * Gets the format suffix for the active environment.
     *
     * @return the format suffix, or default if no environment configured
     */
    public String getActiveFormatSuffix() {
        EnvironmentConfig config = getActiveConfig();
        return config != null ? config.getFormatSuffix() : EnvironmentConfig.DEFAULT_FORMAT_SUFFIX;
    }

    /**
     * Gets the default algorithm for the active environment.
     *
     * @return the default algorithm, or default if no environment configured
     */
    public String getActiveDefaultAlgorithm() {
        EnvironmentConfig config = getActiveConfig();
        return config != null ? config.getDefaultAlgorithm() : EnvironmentConfig.DEFAULT_ALGORITHM;
    }

    /**
     * Gets the active environment name.
     *
     * @return the active environment name
     */
    public String getActiveEnvironment() {
        return activeEnvironment;
    }

    /**
     * Sets the active environment.
     *
     * @param environment the environment name to activate
     */
    public void setActiveEnvironment(String environment) {
        if (environment != null && !NO_ENVIRONMENT.equals(environment)) {
            this.activeEnvironment = environment;
            logger.debug("Active environment set to: {}", environment);
        } else {
            clearActiveEnvironment();
            logger.debug("Active environment cleared");
        }
    }

    /**
     * Checks if any environments are configured.
     *
     * @return true if at least one environment is configured
     */
    public boolean hasEnvironments() {
        return !environments.isEmpty();
    }

    /**
     * Gets the list of environment names.
     *
     * @return list of environment names
     */
    public List<String> getEnvironmentNames() {
        return new ArrayList<>(environments.keySet());
    }

    /**
     * Gets the number of configured environments.
     *
     * @return count of environments
     */
    public int getEnvironmentCount() {
        return environments.size();
    }

    /**
     * Gets a copy of all environments with their configurations. The configurations
     * are copied to prevent external modification.
     *
     * @return copy of environments map
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<String, EnvironmentConfig> getEnvironmentsCopy() {
        Map<String, EnvironmentConfig> copy = new ConcurrentHashMap<>();
        for (Map.Entry<String, EnvironmentConfig> entry : environments.entrySet()) {
            EnvironmentConfig config = entry.getValue();
            copy
                    .put(entry.getKey(), new EnvironmentConfig(config.getPassword(), config.getFormatPrefix(),
                            config.getFormatSuffix(), config.getDefaultAlgorithm()));
        }
        return copy;
    }
}
