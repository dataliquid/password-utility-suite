package com.dataliquid.passwordsuite.crypto.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dataliquid.passwordsuite.crypto.config.AlgorithmConfig;
import com.dataliquid.passwordsuite.crypto.core.SymmetricCipher;
import com.dataliquid.passwordsuite.crypto.impl.AesGcmCipher;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftAutoDetectCipher;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftKeyRules;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftPasswordIvCipher;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftRandomIvCipher;

/**
 * Registry of available encryption algorithms. Maps algorithm names to their
 * configurations.
 */
public final class CipherRegistry {

    private static final int AES_CBC_IV_LENGTH = 16; // 128 bits (AES block size)
    private static final int BLOWFISH_IV_LENGTH = 8; // 64 bits (Blowfish block size)

    private final Map<String, AlgorithmConfig> algorithms = new ConcurrentHashMap<>();

    /**
     * Creates a registry with default algorithms registered.
     */
    public CipherRegistry() {
        registerDefaultAlgorithms();
    }

    /**
     * Registers default algorithms (AES-GCM, AES-CBC, Blowfish).
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void registerDefaultAlgorithms() {
        // AES-256-GCM (recommended)
        register("AES-256-GCM", new AlgorithmConfig("AES", "GCM", "NoPadding", 256, AesGcmCipher::new));

        // AES-256-CBC
        register("AES-256-CBC", new AlgorithmConfig("AES", "CBC", "PKCS5Padding", 256,
                config -> new SymmetricCipher(config, AES_CBC_IV_LENGTH)));

        // AES-128-GCM
        register("AES-128-GCM", new AlgorithmConfig("AES", "GCM", "NoPadding", 128, AesGcmCipher::new));

        // Blowfish
        register("Blowfish", new AlgorithmConfig("Blowfish", "CBC", "PKCS5Padding", 128,
                config -> new SymmetricCipher(config, BLOWFISH_IV_LENGTH)));

        // MuleSoft Secure Properties Tool compatible algorithms. Each family
        // offers the default mode (password as IV), --use-random-iv and an
        // auto-detecting variant.
        registerMuleSoftFamily("MuleSoft-AES-CBC", "AES", 256);
        registerMuleSoftFamily("MuleSoft-Blowfish-CBC", "Blowfish", 128);
        registerMuleSoftFamily("MuleSoft-DESede-CBC", "DESede", 168);
    }

    /**
     * Registers the three IV-mode variants of a MuleSoft-compatible CBC algorithm:
     * password-as-IV (deterministic, MuleSoft default), random IV (--use-random-iv)
     * and auto-detection on decrypt.
     */
    private void registerMuleSoftFamily(String baseName, String algorithm, int keySize) {
        register(baseName, muleSoftConfig(algorithm, keySize, MuleSoftPasswordIvCipher::new));
        register(baseName + "-RandomIV", muleSoftConfig(algorithm, keySize, MuleSoftRandomIvCipher::new));
        register(baseName + "-Auto", muleSoftConfig(algorithm, keySize, MuleSoftAutoDetectCipher::new));
    }

    private static AlgorithmConfig muleSoftConfig(String algorithm, int keySize,
            AlgorithmConfig.CipherSupplier factory) {
        return new AlgorithmConfig(algorithm, "CBC", "PKCS5Padding", keySize, factory, "![", "]",
                length -> MuleSoftKeyRules.isValidKeyLength(algorithm, length),
                MuleSoftKeyRules.passwordRequirement(algorithm));
    }

    /**
     * Registers a new algorithm.
     *
     * @param name   the algorithm name (e.g., "AES-256-GCM")
     * @param config the algorithm configuration
     */
    public void register(String name, AlgorithmConfig config) {
        algorithms.put(name, config);
    }

    /**
     * Gets the configuration for an algorithm.
     *
     * @param  name                     the algorithm name
     *
     * @return                          the algorithm configuration
     *
     * @throws IllegalArgumentException if algorithm not found
     */
    public AlgorithmConfig getAlgorithm(String name) {
        AlgorithmConfig config = algorithms.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Unknown algorithm: " + name);
        }
        return config;
    }

    /**
     * Returns a list of all registered algorithm names, sorted alphabetically with
     * third-party algorithms (MuleSoft) listed at the end.
     *
     * @return list of algorithm names
     */
    public List<String> getAvailableAlgorithms() {
        List<String> result = new ArrayList<>(algorithms.keySet());
        result.sort((a, b) -> {
            boolean aMule = a.startsWith("MuleSoft");
            boolean bMule = b.startsWith("MuleSoft");
            if (aMule && !bMule) {
                return 1;
            }
            if (!aMule && bMule) {
                return -1;
            }
            return a.compareTo(b);
        });
        return result;
    }

    /**
     * Checks if an algorithm is registered.
     *
     * @param  name the algorithm name
     *
     * @return      true if registered, false otherwise
     */
    public boolean hasAlgorithm(String name) {
        return algorithms.containsKey(name);
    }
}
