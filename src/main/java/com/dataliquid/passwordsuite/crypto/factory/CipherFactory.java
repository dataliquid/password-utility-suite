package com.dataliquid.passwordsuite.crypto.factory;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.AlgorithmConfig;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.config.FormatConfig;
import com.dataliquid.passwordsuite.crypto.config.KeyConfig;

/**
 * Factory for creating cipher instances. Key derivation is handled by the
 * cipher implementations themselves, using per-operation salt stored in the
 * output.
 */
public class CipherFactory {

    private static final String DEFAULT_FORMAT_PREFIX = FormatConfig.DEFAULT_PREFIX;
    private static final String DEFAULT_FORMAT_SUFFIX = FormatConfig.DEFAULT_SUFFIX;

    private final CipherRegistry registry;
    private final KeyConfig keyConfig;

    /**
     * Creates a cipher factory.
     *
     * @param registry  the cipher registry
     * @param keyConfig the key configuration (password, iterations, etc.)
     */
    public CipherFactory(CipherRegistry registry, KeyConfig keyConfig) {
        this.registry = registry;
        this.keyConfig = keyConfig;
    }

    /**
     * Creates a cipher for the specified algorithm with default format.
     *
     * @param  algorithmName   the algorithm name (e.g., "AES-256-GCM")
     *
     * @return                 the cipher instance
     *
     * @throws CryptoException if cipher creation fails
     */
    public Cipher createCipher(String algorithmName) throws CryptoException {
        return createCipher(algorithmName, DEFAULT_FORMAT_PREFIX, DEFAULT_FORMAT_SUFFIX);
    }

    /**
     * Creates a cipher for the specified algorithm with custom format. Key
     * derivation is handled per-operation by the cipher implementations.
     *
     * @param  algorithmName   the algorithm name (e.g., "AES-256-GCM")
     * @param  formatPrefix    the prefix for encrypted values (e.g., "ENC[")
     * @param  formatSuffix    the suffix for encrypted values (e.g., "]")
     *
     * @return                 the cipher instance
     *
     * @throws CryptoException if cipher creation fails
     */
    public Cipher createCipher(String algorithmName, String formatPrefix, String formatSuffix) throws CryptoException {
        try {
            // Get algorithm configuration
            AlgorithmConfig algConfig = registry.getAlgorithm(algorithmName);

            // Use provided format or defaults
            String prefix = formatPrefix != null ? formatPrefix : DEFAULT_FORMAT_PREFIX;
            String suffix = formatSuffix != null ? formatSuffix : DEFAULT_FORMAT_SUFFIX;

            // Create cipher configuration with KeyConfig for per-operation key derivation
            CipherConfig cipherConfig = new CipherConfig(algConfig, prefix, suffix, keyConfig);

            // Create cipher instance (key derivation happens per encrypt/decrypt)
            return algConfig.getFactory().create(cipherConfig);

        } catch (Exception e) {
            throw new CryptoException("Failed to create cipher: " + e.getMessage(), e);
        }
    }
}
