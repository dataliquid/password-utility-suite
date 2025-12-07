package com.dataliquid.passwordsuite.crypto.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;

/**
 * MuleSoft-compatible AES-CBC cipher that uses the password as IV.
 * <p>
 * This is the default MuleSoft behavior when NOT using the
 * {@code --use-random-iv} flag. The first 16 characters of the password are
 * used as the initialization vector.
 * </p>
 * <p>
 * Use {@link MuleSoftAesCbcCipher} for MuleSoft's random IV mode.
 * </p>
 */
public class MuleSoftAesCbcPasswordIvCipher implements Cipher {

    private static final int IV_LENGTH = 16; // 128 bits for AES block size

    protected final CipherConfig config;

    /**
     * Creates a MuleSoft-compatible AES-CBC cipher using password as IV.
     *
     * @param config the cipher configuration
     */
    public MuleSoftAesCbcPasswordIvCipher(CipherConfig config) {
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

            // Use first 16 chars of password as IV (MuleSoft default behavior)
            String password = config.getKeyConfig().getMasterPassword();
            byte[] iv = password.substring(0, IV_LENGTH).getBytes(StandardCharsets.UTF_8);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Initialize cipher
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, ivSpec);

            // Encrypt
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Base64 encode (no IV prepended - MuleSoft default format)
            String base64 = Base64.getEncoder().encodeToString(ciphertext);

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
            byte[] ciphertext = Base64.getDecoder().decode(base64);

            // Get key from password directly (MuleSoft style - no PBKDF2)
            SecretKey key = getKeyFromPassword();

            // Use first 16 chars of password as IV
            String password = config.getKeyConfig().getMasterPassword();
            byte[] iv = password.substring(0, IV_LENGTH).getBytes(StandardCharsets.UTF_8);
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
