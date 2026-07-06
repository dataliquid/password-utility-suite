package com.dataliquid.passwordsuite.crypto.impl;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.GCMParameterSpec;

import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.core.SymmetricCipher;

/**
 * AES-GCM (Galois/Counter Mode) cipher implementation. Provides authenticated
 * encryption with associated data (AEAD).
 * <ul>
 * <li>Algorithm: AES</li>
 * <li>Mode: GCM</li>
 * <li>Padding: NoPadding (GCM doesn't require padding)</li>
 * <li>IV Length: 12 bytes (96 bits, recommended for GCM)</li>
 * <li>Tag Length: 128 bits (authentication tag)</li>
 * </ul>
 */
public class AesGcmCipher extends SymmetricCipher {

    private static final int IV_LENGTH = 12; // 96 bits (recommended for GCM)
    private static final int TAG_LENGTH = 128; // 128-bit authentication tag

    /**
     * Creates an AES-GCM cipher.
     *
     * @param config the cipher configuration
     */
    public AesGcmCipher(CipherConfig config) {
        super(config, IV_LENGTH);
    }

    @Override
    protected AlgorithmParameterSpec createParameterSpec(byte[] iv) {
        return new GCMParameterSpec(TAG_LENGTH, iv);
    }
}
