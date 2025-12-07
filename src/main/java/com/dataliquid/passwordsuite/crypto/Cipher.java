package com.dataliquid.passwordsuite.crypto;

/**
 * Interface for encryption and decryption operations. This interface allows for
 * easy swapping of encryption implementations.
 */
public interface Cipher {

    /**
     * Encrypts the given plaintext and returns an encrypted payload. The encrypted
     * payload format is implementation-specific but should be self-contained (e.g.,
     * "ENC[...]").
     *
     * @param  plaintext       the text to encrypt
     *
     * @return                 the encrypted payload
     *
     * @throws CryptoException if encryption fails
     */
    String encrypt(String plaintext) throws CryptoException;

    /**
     * Decrypts the given encrypted payload and returns the plaintext. The encrypted
     * payload should match the format produced by encrypt().
     *
     * @param  encrypted       the encrypted payload (e.g., "ENC[...]")
     *
     * @return                 the decrypted plaintext
     *
     * @throws CryptoException if decryption fails (wrong format, corrupted data,
     *                         etc.)
     */
    String decrypt(String encrypted) throws CryptoException;
}
