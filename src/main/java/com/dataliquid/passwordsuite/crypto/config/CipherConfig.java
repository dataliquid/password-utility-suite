package com.dataliquid.passwordsuite.crypto.config;

/**
 * Configuration for a specific cipher implementation. Contains algorithm
 * parameters and output format settings.
 */
public class CipherConfig {

    private final String algorithm;
    private final String mode;
    private final String padding;
    private final int keySize;
    private final String formatPrefix;
    private final String formatSuffix;
    private final KeyConfig keyConfig;

    /**
     * Creates a cipher configuration.
     *
     * @param algorithm    the encryption algorithm (e.g., "AES", "Blowfish")
     * @param mode         the cipher mode (e.g., "GCM", "CBC")
     * @param padding      the padding scheme (e.g., "NoPadding", "PKCS5Padding")
     * @param keySize      the key size in bits (e.g., 256)
     * @param formatPrefix the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix the suffix for encrypted values (e.g., "]")
     * @param keyConfig    the key derivation configuration
     */
    public CipherConfig(String algorithm, String mode, String padding, int keySize, String formatPrefix,
            String formatSuffix, KeyConfig keyConfig) {
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
        this.keySize = keySize;
        this.formatPrefix = formatPrefix;
        this.formatSuffix = formatSuffix;
        this.keyConfig = keyConfig;
    }

    /**
     * Creates a cipher configuration from a registered algorithm configuration.
     *
     * @param algorithmConfig the algorithm parameters (algorithm, mode, padding,
     *                        key size)
     * @param formatPrefix    the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix    the suffix for encrypted values (e.g., "]")
     * @param keyConfig       the key derivation configuration
     */
    public CipherConfig(AlgorithmConfig algorithmConfig, String formatPrefix, String formatSuffix,
            KeyConfig keyConfig) {
        this(algorithmConfig.getAlgorithm(), algorithmConfig.getMode(), algorithmConfig.getPadding(),
                algorithmConfig.getKeySize(), formatPrefix, formatSuffix, keyConfig);
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

    public String getFormatPrefix() {
        return formatPrefix;
    }

    public String getFormatSuffix() {
        return formatSuffix;
    }

    public KeyConfig getKeyConfig() {
        return keyConfig;
    }

    /**
     * Returns the full transformation string for Java Cipher.
     *
     * @return transformation string (e.g., "AES/GCM/NoPadding")
     */
    public String getTransformation() {
        return algorithm + "/" + mode + "/" + padding;
    }

    /**
     * Wraps a base64-encoded value with the configured format.
     *
     * @param  base64Value the base64-encoded encrypted value
     *
     * @return             formatted encrypted string
     */
    public String wrapValue(String base64Value) {
        return formatPrefix + base64Value + formatSuffix;
    }

    /**
     * Unwraps a formatted encrypted value to extract base64 data.
     *
     * @param  formattedValue the formatted encrypted string
     *
     * @return                base64-encoded data
     */
    public String unwrapValue(String formattedValue) {
        if (!formattedValue.startsWith(formatPrefix) || !formattedValue.endsWith(formatSuffix)) {
            throw new IllegalArgumentException("Invalid format: expected " + formatPrefix + "..." + formatSuffix);
        }
        return formattedValue.substring(formatPrefix.length(), formattedValue.length() - formatSuffix.length());
    }

    @Override
    public String toString() {
        return "CipherConfig{" + "transformation='" + getTransformation() + '\'' + ", keySize=" + keySize + ", format='"
                + formatPrefix + "..." + formatSuffix + '\'' + '}';
    }
}
