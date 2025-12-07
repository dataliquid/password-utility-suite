package com.dataliquid.passwordsuite.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.key.SecureKeyGenerator;

/**
 * MuleSoft-compatible AES-CBC cipher with random IV.
 * <p>
 * This cipher is compatible with MuleSoft's Secure Properties Tool when using
 * the {@code --use-random-iv} flag. Output format is Base64(IV + ciphertext).
 * </p>
 * <p>
 * Use {@link MuleSoftAesCbcPasswordIvCipher} for MuleSoft's default mode
 * (password as IV).
 * </p>
 */
public class MuleSoftAesCbcCipher implements Cipher {

    private static final int IV_LENGTH = 16; // 128 bits for AES block size

    protected final CipherConfig config;

    /**
     * Creates a MuleSoft-compatible AES-CBC cipher.
     *
     * @param config the cipher configuration
     */
    public MuleSoftAesCbcCipher(CipherConfig config) {
        this.config = config;
    }

    @Override
    public String encrypt(String plaintext) throws CryptoException {
        if (plaintext == null) {
            throw new CryptoException("Plaintext cannot be null");
        }

        try {
            // Get key from password directly (MuleSoft style - no PBKDF2)
            SecretKey key = getKeyFromPassword();

            // Generate random IV
            byte[] iv = SecureKeyGenerator.generateIV(IV_LENGTH);

            // Create IV parameter spec
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, ivSpec);

            // Encrypt
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + ciphertext (MuleSoft format with --use-random-iv)
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

            // Base64 encode
            String base64 = Base64.getEncoder().encodeToString(result);

            // Wrap with format
            return config.wrapValue(base64);

        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("MuleSoft AES-CBC encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String encrypted) throws CryptoException {
        if (encrypted == null) {
            throw new CryptoException("Encrypted text cannot be null");
        }

        try {
            // Unwrap format
            String base64 = config.unwrapValue(encrypted);

            // Base64 decode
            byte[] combined = Base64.getDecoder().decode(base64);

            // Minimum length: IV (16) + at least one block of ciphertext (16)
            if (combined.length < IV_LENGTH + IV_LENGTH) {
                throw new CryptoException("Invalid ciphertext: too short for random IV format");
            }

            // Get key from password directly (MuleSoft style - no PBKDF2)
            SecretKey key = getKeyFromPassword();

            // Extract IV (first 16 bytes) and ciphertext (rest)
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
            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            // Convert to string
            return new String(plaintextBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            throw new CryptoException("Invalid encrypted format: " + e.getMessage(), e);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("MuleSoft AES-CBC decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the AES key directly from the password. The password must be exactly 16
     * bytes (AES-128) or 32 bytes (AES-256) for MuleSoft compatibility.
     *
     * @return                 the secret key
     *
     * @throws CryptoException if password length is invalid
     */
    private SecretKey getKeyFromPassword() throws CryptoException {
        String password = config.getKeyConfig().getMasterPassword();
        byte[] keyBytes = password.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != 16 && keyBytes.length != 32) {
            throw new CryptoException(
                    "MuleSoft AES requires 16 or 32 character password, but got " + keyBytes.length + " characters.");
        }

        return new SecretKeySpec(keyBytes, "AES");
    }
}
