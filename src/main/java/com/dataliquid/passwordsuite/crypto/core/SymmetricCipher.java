package com.dataliquid.passwordsuite.crypto.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;

/**
 * Abstract base class for symmetric encryption algorithms. Handles common tasks
 * like Base64 encoding, format wrapping, and salt management.
 * <p>
 * Output format: Base64(salt + IV + ciphertext)
 * </p>
 */
public abstract class SymmetricCipher extends AbstractCipher {

    /**
     * Creates a symmetric cipher with configuration.
     *
     * @param config the cipher configuration
     */
    protected SymmetricCipher(CipherConfig config) {
        super(config);
    }

    @Override
    public String encrypt(String plaintext) throws CryptoException {
        if (plaintext == null) {
            throw new CryptoException("Plaintext cannot be null");
        }

        try {
            // Generate salt for this encryption
            byte[] salt = generateSalt();

            // Derive key from password using this salt
            SecretKey key = deriveKey(salt);

            // Convert plaintext to bytes
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);

            // Encrypt (returns IV + ciphertext)
            byte[] ivAndCiphertext = encryptBytes(plaintextBytes, key);

            // Combine salt + IV + ciphertext
            byte[] result = new byte[salt.length + ivAndCiphertext.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(ivAndCiphertext, 0, result, salt.length, ivAndCiphertext.length);

            // Base64 encode
            String base64 = Base64.getEncoder().encodeToString(result);

            // Wrap with format
            return config.wrapValue(base64);

        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Encryption failed: " + e.getMessage(), e);
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

            // Extract salt
            int saltLength = getSaltLength();
            if (combined.length < saltLength) {
                throw new CryptoException("Invalid ciphertext: too short for salt");
            }

            byte[] salt = new byte[saltLength];
            byte[] ivAndCiphertext = new byte[combined.length - saltLength];
            System.arraycopy(combined, 0, salt, 0, saltLength);
            System.arraycopy(combined, saltLength, ivAndCiphertext, 0, ivAndCiphertext.length);

            // Derive key from password using extracted salt
            SecretKey key = deriveKey(salt);

            // Decrypt (expects IV + ciphertext)
            byte[] plaintextBytes = decryptBytes(ivAndCiphertext, key);

            // Convert to string
            return new String(plaintextBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            throw new CryptoException("Invalid encrypted format: " + e.getMessage(), e);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
