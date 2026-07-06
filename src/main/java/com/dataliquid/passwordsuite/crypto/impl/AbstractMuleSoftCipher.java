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
 * Base class for MuleSoft Secure Properties Tool compatible AES-CBC ciphers.
 * MuleSoft uses the password directly as the AES key (no PBKDF2); the password
 * must be exactly 16 bytes (AES-128) or 32 bytes (AES-256).
 */
abstract class AbstractMuleSoftCipher implements Cipher {

    protected static final int IV_LENGTH = 16; // 128 bits for AES block size

    protected final CipherConfig config;

    protected AbstractMuleSoftCipher(CipherConfig config) {
        this.config = config;
    }

    @Override
    public String encrypt(String plaintext) throws CryptoException {
        if (plaintext == null) {
            throw new CryptoException("Plaintext cannot be null");
        }

        try {
            SecretKey key = getKeyFromPassword();
            byte[] payload = doEncrypt(plaintext.getBytes(StandardCharsets.UTF_8), key);
            return config.wrapValue(Base64.getEncoder().encodeToString(payload));

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
            byte[] payload = Base64.getDecoder().decode(config.unwrapValue(encrypted));
            SecretKey key = getKeyFromPassword();
            byte[] plaintextBytes = doDecrypt(payload, key);
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
     * Encrypts plaintext bytes; the returned payload is Base64-encoded and wrapped
     * as-is.
     *
     * @param  plaintext the plaintext bytes
     * @param  key       the secret key
     *
     * @return           the payload bytes (including IV if the format requires it)
     *
     * @throws Exception if encryption fails
     */
    protected abstract byte[] doEncrypt(byte[] plaintext, SecretKey key) throws Exception;

    /**
     * Decrypts the Base64-decoded payload.
     *
     * @param  payload   the payload bytes (including IV if the format requires it)
     * @param  key       the secret key
     *
     * @return           the plaintext bytes
     *
     * @throws Exception if decryption fails
     */
    protected abstract byte[] doDecrypt(byte[] payload, SecretKey key) throws Exception;

    /**
     * Creates an initialized AES-CBC cipher for the given mode and IV.
     *
     * @param  mode      the cipher mode (ENCRYPT_MODE or DECRYPT_MODE)
     * @param  key       the secret key
     * @param  iv        the initialization vector
     *
     * @return           the initialized cipher
     *
     * @throws Exception if initialization fails
     */
    protected javax.crypto.Cipher initCipher(int mode, SecretKey key, byte[] iv) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(config.getTransformation());
        cipher.init(mode, key, new IvParameterSpec(iv));
        return cipher;
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
