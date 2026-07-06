package com.dataliquid.passwordsuite.crypto.config;

import java.util.function.IntPredicate;

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
    private final IntPredicate passwordLengthRule;
    private final String passwordRequirement;

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
        this(algorithm, mode, padding, keySize, factory, FormatConfig.DEFAULT_PREFIX, FormatConfig.DEFAULT_SUFFIX, null,
                null);
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
     * @param passwordLengthRule  predicate validating the password length, or null
     *                            if the password length is unconstrained
     * @param passwordRequirement human-readable requirement (e.g., "16 or 32"), or
     *                            null if unconstrained
     */
    public AlgorithmConfig(String algorithm, String mode, String padding, int keySize, CipherSupplier factory,
            String defaultFormatPrefix, String defaultFormatSuffix, IntPredicate passwordLengthRule,
            String passwordRequirement) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
        this.keySize = keySize;
        this.factory = factory;
        this.defaultFormatPrefix = defaultFormatPrefix;
        this.defaultFormatSuffix = defaultFormatSuffix;
        this.passwordLengthRule = passwordLengthRule;
        this.passwordRequirement = passwordRequirement;
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

    /**
     * Checks whether the given password length is valid for this algorithm.
     *
     * @param  length the password length
     *
     * @return        true if valid or unconstrained
     */
    public boolean isValidPasswordLength(int length) {
        return passwordLengthRule == null || passwordLengthRule.test(length);
    }

    public String getPasswordRequirement() {
        return passwordRequirement;
    }

    /**
     * Checks if this algorithm has password length constraints.
     *
     * @return true if a password length rule is set
     */
    public boolean hasPasswordConstraints() {
        return passwordLengthRule != null;
    }

    @Override
    public String toString() {
        return "AlgorithmConfig{" + "algorithm='" + algorithm + '\'' + ", mode='" + mode + '\'' + ", padding='"
                + padding + '\'' + ", keySize=" + keySize + '}';
    }
}
