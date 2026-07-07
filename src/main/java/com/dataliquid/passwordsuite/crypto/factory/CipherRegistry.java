package com.dataliquid.passwordsuite.crypto.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dataliquid.passwordsuite.crypto.config.AlgorithmConfig;
import com.dataliquid.passwordsuite.crypto.core.SymmetricCipher;
import com.dataliquid.passwordsuite.crypto.impl.AesGcmCipher;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftAesCbcAutoDetectCipher;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftAesCbcCipher;
import com.dataliquid.passwordsuite.crypto.impl.MuleSoftAesCbcPasswordIvCipher;

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

        // MuleSoft-compatible AES-CBC with password as IV (default MuleSoft mode)
        // This is the standard MuleSoft format when NOT using --use-random-iv
        register("MuleSoft-AES-CBC", new AlgorithmConfig("AES", "CBC", "PKCS5Padding", 256,
                MuleSoftAesCbcPasswordIvCipher::new, "![", "]", 16, 32));

        // MuleSoft-compatible AES-CBC with random IV prepended
        // This matches MuleSoft's --use-random-iv flag
        register("MuleSoft-AES-CBC-RandomIV",
                new AlgorithmConfig("AES", "CBC", "PKCS5Padding", 256, MuleSoftAesCbcCipher::new, "![", "]", 16, 32));

        // MuleSoft-compatible AES-CBC that auto-detects the IV mode on decrypt
        // (encrypts with random IV)
        register("MuleSoft-AES-CBC-Auto", new AlgorithmConfig("AES", "CBC", "PKCS5Padding", 256,
                MuleSoftAesCbcAutoDetectCipher::new, "![", "]", 16, 32));
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
