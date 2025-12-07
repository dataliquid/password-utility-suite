package com.dataliquid.passwordsuite.crypto.core;

import javax.crypto.SecretKey;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.key.KeyDerivation;
import com.dataliquid.passwordsuite.crypto.key.SecureKeyGenerator;

/**
 * Abstract base class for cipher implementations. Provides common functionality
 * for format handling, key derivation, and error management.
 * <p>
 * Key derivation happens per-operation using PBKDF2 with a random salt. The
 * salt is stored in the output to allow decryption later.
 * </p>
 */
public abstract class AbstractCipher implements Cipher {

    protected final CipherConfig config;

    /**
     * Creates an abstract cipher with configuration.
     *
     * @param config the cipher configuration (includes KeyConfig for derivation)
     */
    protected AbstractCipher(CipherConfig config) {
        this.config = config;
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
    protected SecretKey deriveKey(byte[] salt) throws CryptoException {
        return KeyDerivation
                .deriveKey(config.getKeyConfig().getMasterPassword(), salt, config.getKeySize(),
                        config.getKeyConfig().getIterations(), config.getKeyConfig().getKdfAlgorithm(),
                        config.getAlgorithm());
    }

    /**
     * Generates a random salt for key derivation.
     *
     * @return random salt bytes
     */
    protected byte[] generateSalt() {
        return SecureKeyGenerator.generateSalt(config.getKeyConfig().getSaltLength());
    }

    /**
     * Gets the salt length from configuration.
     *
     * @return salt length in bytes
     */
    protected int getSaltLength() {
        return config.getKeyConfig().getSaltLength();
    }

    /**
     * Encrypts plaintext bytes to ciphertext bytes.
     *
     * @param  plaintext       the plaintext bytes
     * @param  key             the secret key
     *
     * @return                 the ciphertext bytes (including IV if needed)
     *
     * @throws CryptoException if encryption fails
     */
    protected abstract byte[] encryptBytes(byte[] plaintext, SecretKey key) throws CryptoException;

    /**
     * Decrypts ciphertext bytes to plaintext bytes.
     *
     * @param  ciphertext      the ciphertext bytes (including IV if needed)
     * @param  key             the secret key
     *
     * @return                 the plaintext bytes
     *
     * @throws CryptoException if decryption fails
     */
    protected abstract byte[] decryptBytes(byte[] ciphertext, SecretKey key) throws CryptoException;
}
