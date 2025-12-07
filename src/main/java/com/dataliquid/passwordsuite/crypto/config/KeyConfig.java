package com.dataliquid.passwordsuite.crypto.config;

/**
 * Configuration for key derivation and management. Defines parameters for
 * PBKDF2 key derivation from master password.
 */
public class KeyConfig {

    private static final int MIN_ITERATIONS = 1000;
    private static final int MIN_SALT_LENGTH = 8;

    private final String masterPassword;
    private final String kdfAlgorithm;
    private final int iterations;
    private final int saltLength;

    /**
     * Creates a key configuration with default settings.
     *
     * @param masterPassword the master password for key derivation
     */
    public KeyConfig(String masterPassword) {
        this(masterPassword, "PBKDF2WithHmacSHA256", 65536, 16);
    }

    /**
     * Creates a key configuration with custom settings.
     *
     * @param masterPassword the master password for key derivation
     * @param kdfAlgorithm   the key derivation algorithm (e.g.,
     *                       "PBKDF2WithHmacSHA256")
     * @param iterations     the number of PBKDF2 iterations (recommended: 65536+)
     * @param saltLength     the length of the salt in bytes (recommended: 16)
     */
    public KeyConfig(String masterPassword, String kdfAlgorithm, int iterations, int saltLength) {
        if (masterPassword == null || masterPassword.isEmpty()) {
            throw new IllegalArgumentException("Master password cannot be null or empty");
        }
        if (iterations < MIN_ITERATIONS) {
            throw new IllegalArgumentException(
                    "Iterations must be at least " + MIN_ITERATIONS + " (recommended: 65536+)");
        }
        if (saltLength < MIN_SALT_LENGTH) {
            throw new IllegalArgumentException(
                    "Salt length must be at least " + MIN_SALT_LENGTH + " bytes (recommended: 16)");
        }

        this.masterPassword = masterPassword;
        this.kdfAlgorithm = kdfAlgorithm;
        this.iterations = iterations;
        this.saltLength = saltLength;
    }

    public String getMasterPassword() {
        return masterPassword;
    }

    public String getKdfAlgorithm() {
        return kdfAlgorithm;
    }

    public int getIterations() {
        return iterations;
    }

    public int getSaltLength() {
        return saltLength;
    }

    @Override
    public String toString() {
        return "KeyConfig{" + "kdfAlgorithm='" + kdfAlgorithm + '\'' + ", iterations=" + iterations + ", saltLength="
                + saltLength + ", passwordSet=" + (masterPassword != null && !masterPassword.isEmpty()) + '}';
    }
}
