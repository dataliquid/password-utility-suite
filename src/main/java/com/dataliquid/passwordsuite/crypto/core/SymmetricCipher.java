package com.dataliquid.passwordsuite.crypto.core;

import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.key.KeyDerivation;
import com.dataliquid.passwordsuite.crypto.key.SecureKeyGenerator;

/**
 * Symmetric cipher with per-operation PBKDF2 key derivation. Handles format
 * wrapping, Base64 encoding, salt and IV management for all block cipher modes;
 * the parameter spec can be customized per mode (e.g., GCM).
 * <p>
 * Key derivation happens per-operation using PBKDF2 with a random salt. The
 * salt is stored in the output to allow decryption later.
 * </p>
 * <p>
 * Output format: Base64(salt + IV + ciphertext)
 * </p>
 */
public class SymmetricCipher implements Cipher {

    protected final CipherConfig config;
    private final int ivLength;

    /**
     * Creates a symmetric cipher with configuration.
     *
     * @param config   the cipher configuration
     * @param ivLength the IV length in bytes (typically the cipher block size)
     */
    public SymmetricCipher(CipherConfig config, int ivLength) {
        this.config = config;
        this.ivLength = ivLength;
    }

    @Override
    public String encrypt(String plaintext) throws CryptoException {
        if (plaintext == null) {
            throw new CryptoException("Plaintext cannot be null");
        }

        try {
            // Generate salt for this encryption and derive key from password
            byte[] salt = SecureKeyGenerator.generateSalt(config.getKeyConfig().getSaltLength());
            SecretKey key = deriveKey(salt);

            // Encrypt with a fresh random IV
            byte[] iv = SecureKeyGenerator.generateIV(ivLength);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, createParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine salt + IV + ciphertext
            byte[] result = new byte[salt.length + iv.length + ciphertext.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(iv, 0, result, salt.length, iv.length);
            System.arraycopy(ciphertext, 0, result, salt.length + iv.length, ciphertext.length);

            return config.wrapValue(Base64.getEncoder().encodeToString(result));

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
            byte[] combined = Base64.getDecoder().decode(config.unwrapValue(encrypted));

            int saltLength = config.getKeyConfig().getSaltLength();
            if (combined.length < saltLength + ivLength) {
                throw new CryptoException("Invalid ciphertext: too short");
            }

            // Split salt + IV + ciphertext
            byte[] salt = Arrays.copyOfRange(combined, 0, saltLength);
            byte[] iv = Arrays.copyOfRange(combined, saltLength, saltLength + ivLength);
            byte[] ciphertext = Arrays.copyOfRange(combined, saltLength + ivLength, combined.length);

            // Derive key from password using extracted salt and decrypt
            SecretKey key = deriveKey(salt);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, createParameterSpec(iv));
            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            return new String(plaintextBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException e) {
            throw new CryptoException("Invalid encrypted format: " + e.getMessage(), e);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the algorithm parameter spec for the given IV. Defaults to a plain
     * {@link IvParameterSpec}; modes with additional parameters (e.g., GCM)
     * override this.
     *
     * @param  iv the initialization vector
     *
     * @return    the parameter spec for cipher initialization
     */
    protected AlgorithmParameterSpec createParameterSpec(byte[] iv) {
        return new IvParameterSpec(iv);
    }

    /**
     * Derives a secret key from the master password using PBKDF2.
     *
     * @param  salt            the salt for key derivation
     *
     * @return                 the derived secret key
     *
     * @throws CryptoException if key derivation fails
     */
    private SecretKey deriveKey(byte[] salt) throws CryptoException {
        return KeyDerivation
                .deriveKey(config.getKeyConfig().getMasterPassword(), salt, config.getKeySize(),
                        config.getKeyConfig().getIterations(), config.getKeyConfig().getKdfAlgorithm(),
                        config.getAlgorithm());
    }
}
