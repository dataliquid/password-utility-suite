package com.dataliquid.passwordsuite.crypto.impl;

import java.util.Arrays;

import javax.crypto.SecretKey;

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
public class MuleSoftAesCbcCipher extends AbstractMuleSoftCipher {

    /**
     * Creates a MuleSoft-compatible AES-CBC cipher.
     *
     * @param config the cipher configuration
     */
    public MuleSoftAesCbcCipher(CipherConfig config) {
        super(config);
    }

    @Override
    protected byte[] doEncrypt(byte[] plaintext, SecretKey key) throws Exception {
        // Random IV is prepended to the ciphertext (MuleSoft --use-random-iv format)
        byte[] iv = SecureKeyGenerator.generateIV(IV_LENGTH);
        byte[] ciphertext = initCipher(javax.crypto.Cipher.ENCRYPT_MODE, key, iv).doFinal(plaintext);

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    @Override
    protected byte[] doDecrypt(byte[] payload, SecretKey key) throws Exception {
        // Minimum length: IV (16) + at least one block of ciphertext (16)
        if (payload.length < IV_LENGTH + IV_LENGTH) {
            throw new CryptoException("Invalid ciphertext: too short for random IV format");
        }

        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);
        return initCipher(javax.crypto.Cipher.DECRYPT_MODE, key, iv).doFinal(ciphertext);
    }
}
