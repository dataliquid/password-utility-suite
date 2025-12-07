package com.dataliquid.passwordsuite.crypto.impl;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.core.SymmetricCipher;
import com.dataliquid.passwordsuite.crypto.key.SecureKeyGenerator;

/**
 * Blowfish cipher implementation with CBC mode. Legacy algorithm, but still
 * supported for compatibility.
 * <ul>
 * <li>Algorithm: Blowfish</li>
 * <li>Mode: CBC</li>
 * <li>Padding: PKCS5Padding</li>
 * <li>IV Length: 8 bytes (64 bits, Blowfish block size)</li>
 * <li>Key Size: variable (32-448 bits, typically 128 bits)</li>
 * </ul>
 */
public class BlowfishCipher extends SymmetricCipher {

    private static final int IV_LENGTH = 8; // 64 bits (Blowfish block size)

    /**
     * Creates a Blowfish cipher.
     *
     * @param config the cipher configuration
     */
    public BlowfishCipher(CipherConfig config) {
        super(config);
    }

    @Override
    protected byte[] encryptBytes(byte[] plaintext, SecretKey key) throws CryptoException {
        try {
            // Generate random IV
            byte[] iv = SecureKeyGenerator.generateIV(IV_LENGTH);

            // Create IV parameter spec
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, ivSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Combine IV + ciphertext
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

            return result;

        } catch (Exception e) {
            throw new CryptoException("Blowfish encryption failed: " + e.getMessage(), e);
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

            // Create IV parameter spec
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, ivSpec);

            // Decrypt
            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            throw new CryptoException("Blowfish decryption failed: " + e.getMessage(), e);
        }
    }
}
