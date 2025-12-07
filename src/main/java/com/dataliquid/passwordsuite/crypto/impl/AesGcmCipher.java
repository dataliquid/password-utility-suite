package com.dataliquid.passwordsuite.crypto.impl;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.core.SymmetricCipher;
import com.dataliquid.passwordsuite.crypto.key.SecureKeyGenerator;

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
        super(config);
    }

    @Override
    protected byte[] encryptBytes(byte[] plaintext, SecretKey key) throws CryptoException {
        try {
            // Generate random IV
            byte[] iv = SecureKeyGenerator.generateIV(IV_LENGTH);

            // Create GCM parameter spec
            AlgorithmParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, spec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Combine IV + ciphertext
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

            return result;

        } catch (Exception e) {
            throw new CryptoException("AES-GCM encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected byte[] decryptBytes(byte[] combined, SecretKey key) throws CryptoException {
        try {
            // Split IV and ciphertext
            if (combined.length < IV_LENGTH) {
                throw new CryptoException("Invalid ciphertext: too short");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Create GCM parameter spec
            AlgorithmParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, spec);

            // Decrypt
            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            throw new CryptoException("AES-GCM decryption failed: " + e.getMessage(), e);
        }
    }
}
