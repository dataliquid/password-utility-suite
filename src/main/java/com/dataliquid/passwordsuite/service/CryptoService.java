package com.dataliquid.passwordsuite.service;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.factory.CipherFactory;
import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.operation.EditValueOperation;

/**
 * Service for encrypting and decrypting configuration entries. Supports
 * multiple encryption algorithms through CipherFactory.
 */
public class CryptoService {

    private Cipher cipher;
    private CipherFactory cipherFactory;
    private String currentAlgorithm;
    private String formatPrefix;
    private String formatSuffix;

    /**
     * Creates a CryptoService with no cipher configured. Use setCipherFactory() and
     * setAlgorithm() to configure encryption.
     */
    public CryptoService() {
        // Fields default to null - no explicit initialization needed
    }

    /**
     * Creates a CryptoService with a cipher factory for algorithm selection.
     *
     * @param cipherFactory the cipher factory
     */
    public CryptoService(CipherFactory cipherFactory) {
        this.cipherFactory = cipherFactory;
        // cipher and currentAlgorithm default to null
    }

    /**
     * Sets the cipher factory for this service. Resets the current cipher and
     * algorithm selection.
     *
     * @param cipherFactory the cipher factory
     */
    public void setCipherFactory(CipherFactory cipherFactory) {
        this.cipherFactory = cipherFactory;
        resetCipherState();
    }

    /**
     * Resets cipher state when factory changes.
     */
    @SuppressWarnings("PMD.NullAssignment")
    private void resetCipherState() {
        this.cipher = null;
        this.currentAlgorithm = null;
    }

    /**
     * Checks if the service is configured with a cipher.
     *
     * @return true if a cipher is configured, false otherwise
     */
    public boolean isConfigured() {
        return cipherFactory != null && cipher != null;
    }

    /**
     * Creates an operation to encrypt a ConfigEntry.
     *
     * @param  entry the entry to encrypt
     *
     * @return       an EditValueOperation that encrypts the entry
     */
    public EditValueOperation createEncryptOperation(ConfigEntry entry) {
        return new EditValueOperation(entry, encryptValue(entry.getValue()), true);
    }

    /**
     * Creates an operation to decrypt a ConfigEntry.
     *
     * @param  entry the entry to decrypt
     *
     * @return       an EditValueOperation that decrypts the entry
     */
    public EditValueOperation createDecryptOperation(ConfigEntry entry) {
        return new EditValueOperation(entry, decryptValue(entry.getValue()), false);
    }

    /**
     * Encrypts a plain text value.
     *
     * @param  plaintext the value to encrypt
     *
     * @return           the encrypted value
     */
    private String encryptValue(String plaintext) {
        if (cipher == null) {
            throw new RuntimeException("Encryption not configured. Please set master password and select algorithm.");
        }
        try {
            return cipher.encrypt(plaintext);
        } catch (CryptoException e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts an encrypted value.
     *
     * @param  encrypted the encrypted value
     *
     * @return           the decrypted plain text
     */
    private String decryptValue(String encrypted) {
        if (cipher == null) {
            throw new RuntimeException("Decryption not configured. Please set master password and select algorithm.");
        }
        try {
            return cipher.decrypt(encrypted);
        } catch (CryptoException e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Switches to a different encryption algorithm.
     *
     * @param  algorithmName   the algorithm name (e.g., "AES-256-GCM")
     *
     * @throws CryptoException if algorithm switch fails
     */
    public void setAlgorithm(String algorithmName) throws CryptoException {
        if (cipherFactory == null) {
            throw new CryptoException("No cipher factory available. Set master password first.");
        }
        this.cipher = cipherFactory.createCipher(algorithmName, formatPrefix, formatSuffix);
        this.currentAlgorithm = algorithmName;
    }

    /**
     * Sets the encryption format prefix and suffix.
     *
     * @param formatPrefix the prefix for encrypted values (e.g., "ENC[")
     * @param formatSuffix the suffix for encrypted values (e.g., "]")
     */
    public void setFormat(String formatPrefix, String formatSuffix) {
        this.formatPrefix = formatPrefix;
        this.formatSuffix = formatSuffix;
    }

    /**
     * Gets the format prefix for encrypted values.
     *
     * @return the format prefix
     */
    public String getFormatPrefix() {
        return formatPrefix;
    }

    /**
     * Gets the format suffix for encrypted values.
     *
     * @return the format suffix
     */
    public String getFormatSuffix() {
        return formatSuffix;
    }

    /**
     * Returns the current algorithm name.
     *
     * @return the algorithm name
     */
    public String getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    /**
     * Returns the cipher factory.
     *
     * @return the cipher factory, or null if not set
     */
    public CipherFactory getCipherFactory() {
        return cipherFactory;
    }

    /**
     * Encrypts a plain text value. Public convenience method for CLI usage.
     *
     * @param  plaintext the value to encrypt
     *
     * @return           the encrypted value with format prefix/suffix
     */
    public String encrypt(String plaintext) {
        return encryptValue(plaintext);
    }

    /**
     * Decrypts an encrypted value. Public convenience method for CLI usage.
     *
     * @param  ciphertext the encrypted value
     *
     * @return            the decrypted plain text
     */
    public String decrypt(String ciphertext) {
        return decryptValue(ciphertext);
    }
}
