package com.dataliquid.passwordsuite.crypto.config;

import com.dataliquid.passwordsuite.crypto.Cipher;

/**
 * Configuration for a registered encryption algorithm. Defines algorithm
 * parameters and factory for creating cipher instances.
 */
public class AlgorithmConfig {

    private final String algorithm;
    private final String mode;
    private final String padding;
    private final int keySize;
    private final CipherSupplier factory;
    private final String defaultFormatPrefix;
    private final String defaultFormatSuffix;
    private final int minPasswordLength;
    private final int maxPasswordLength;

    /**
     * Functional interface for creating cipher instances. Key derivation is handled
     * by the cipher implementation using the KeyConfig in CipherConfig. (Named
     * CipherSupplier to avoid a name clash with the CipherFactory class.)
     */
    @FunctionalInterface
    public interface CipherSupplier {
        Cipher create(CipherConfig config) throws Exception;
    }

    /**
     * Creates an algorithm configuration with default format (ENC[...]) and no
     * password length constraints.
     *
     * @param algorithm the encryption algorithm (e.g., "AES")
     * @param mode      the cipher mode (e.g., "GCM", "CBC")
     * @param padding   the padding scheme (e.g., "NoPadding")
     * @param keySize   the key size in bits
     * @param factory   factory function to create cipher instances
     */
    public AlgorithmConfig(String algorithm, String mode, String padding, int keySize, CipherSupplier factory) {
        this(algorithm, mode, padding, keySize, factory, FormatConfig.DEFAULT_PREFIX, FormatConfig.DEFAULT_SUFFIX, 0,
                0);
    }

    /**
     * Creates an algorithm configuration with custom format and password length
     * constraints.
     *
     * @param algorithm           the encryption algorithm (e.g., "AES")
     * @param mode                the cipher mode (e.g., "GCM", "CBC")
     * @param padding             the padding scheme (e.g., "NoPadding")
     * @param keySize             the key size in bits
     * @param factory             factory function to create cipher instances
     * @param defaultFormatPrefix the default format prefix (e.g., "ENC[" or "![")
     * @param defaultFormatSuffix the default format suffix (e.g., "]")
     * @param minPasswordLength   minimum password length (0 = no constraint)
     * @param maxPasswordLength   maximum password length (0 = no constraint)
     */
    public AlgorithmConfig(String algorithm, String mode, String padding, int keySize, CipherSupplier factory,
            String defaultFormatPrefix, String defaultFormatSuffix, int minPasswordLength, int maxPasswordLength) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
        this.keySize = keySize;
        this.factory = factory;
        this.defaultFormatPrefix = defaultFormatPrefix;
        this.defaultFormatSuffix = defaultFormatSuffix;
        this.minPasswordLength = minPasswordLength;
        this.maxPasswordLength = maxPasswordLength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getMode() {
        return mode;
    }

    public String getPadding() {
        return padding;
    }

    public int getKeySize() {
        return keySize;
    }

    public CipherSupplier getFactory() {
        return factory;
    }

    public String getDefaultFormatPrefix() {
        return defaultFormatPrefix;
    }

    public String getDefaultFormatSuffix() {
        return defaultFormatSuffix;
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public int getMaxPasswordLength() {
        return maxPasswordLength;
    }

    /**
     * Checks if this algorithm has password length constraints.
     *
     * @return true if min or max password length is set
     */
    public boolean hasPasswordConstraints() {
        return minPasswordLength > 0 || maxPasswordLength > 0;
    }

    @Override
    public String toString() {
        return "AlgorithmConfig{" + "algorithm='" + algorithm + '\'' + ", mode='" + mode + '\'' + ", padding='"
                + padding + '\'' + ", keySize=" + keySize + '}';
    }
}
