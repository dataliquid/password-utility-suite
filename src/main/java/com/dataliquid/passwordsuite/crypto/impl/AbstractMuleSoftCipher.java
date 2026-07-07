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
 * Base class for MuleSoft Secure Properties Tool compatible ciphers. MuleSoft
 * uses the password directly as the encryption key (no PBKDF2); the allowed
 * password length depends on the algorithm (AES: exactly 16 or 32 bytes,
 * DESede: exactly 24 bytes, Blowfish: 8 to 56 bytes).
 */
abstract class AbstractMuleSoftCipher implements Cipher {

    private static final int AES_BLOCK_SIZE = 16;
    private static final int LEGACY_BLOCK_SIZE = 8; // Blowfish, DES, DESede

    protected final CipherConfig config;

    /** IV length in bytes = cipher block size (16 for AES, 8 for the rest). */
    protected final int ivLength;

    protected AbstractMuleSoftCipher(CipherConfig config) {
        this.config = config;
        this.ivLength = "AES".equals(config.getAlgorithm()) ? AES_BLOCK_SIZE : LEGACY_BLOCK_SIZE;
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
            throw new CryptoException("MuleSoft " + config.getAlgorithm() + " encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String encrypted) throws CryptoException {
        return new String(decryptToBytes(encrypted), StandardCharsets.UTF_8);
    }

    /**
     * Decrypts to raw plaintext bytes. Used by the auto-detect cipher, which needs
     * byte-level access (converting garbage bytes to String would replace them with
     * U+FFFD and break detection).
     *
     * @param  encrypted       the encrypted payload (e.g., "![...]")
     *
     * @return                 the plaintext bytes
     *
     * @throws CryptoException if decryption fails
     */
    byte[] decryptToBytes(String encrypted) throws CryptoException {
        if (encrypted == null) {
            throw new CryptoException("Encrypted text cannot be null");
        }

        try {
            byte[] payload = Base64.getDecoder().decode(config.unwrapValue(encrypted));
            SecretKey key = getKeyFromPassword();
            return doDecrypt(payload, key);

        } catch (IllegalArgumentException e) {
            throw new CryptoException("Invalid encrypted format: " + e.getMessage(), e);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("MuleSoft " + config.getAlgorithm() + " decryption failed: " + e.getMessage(), e);
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
     * Creates an initialized cipher for the given mode and IV.
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
     * Gets the key directly from the password (MuleSoft style - no key derivation).
     * The allowed password length depends on the algorithm.
     *
     * @return                 the secret key
     *
     * @throws CryptoException if password length is invalid for the algorithm
     */
    private SecretKey getKeyFromPassword() throws CryptoException {
        String algorithm = config.getAlgorithm();
        byte[] keyBytes = config.getKeyConfig().getMasterPassword().getBytes(StandardCharsets.UTF_8);

        validateKeyLength(algorithm, keyBytes.length);
        return new SecretKeySpec(keyBytes, algorithm);
    }

    private static void validateKeyLength(String algorithm, int length) throws CryptoException {
        if (!MuleSoftKeyRules.isValidKeyLength(algorithm, length)) {
            throw new CryptoException(
                    "MuleSoft " + algorithm + " requires " + MuleSoftKeyRules.passwordRequirement(algorithm)
                            + " character password, but got " + length + " characters.");
        }
    }
}
