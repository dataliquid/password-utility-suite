package com.dataliquid.passwordsuite.crypto.impl;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import com.dataliquid.passwordsuite.crypto.config.CipherConfig;

/**
 * MuleSoft-compatible AES-CBC cipher that uses the password as IV.
 * <p>
 * This is the default MuleSoft behavior when NOT using the
 * {@code --use-random-iv} flag. The first 16 characters of the password are
 * used as the initialization vector; no IV is stored in the output.
 * </p>
 * <p>
 * Use {@link MuleSoftAesCbcCipher} for MuleSoft's random IV mode.
 * </p>
 */
public class MuleSoftAesCbcPasswordIvCipher extends AbstractMuleSoftCipher {

    /**
     * Creates a MuleSoft-compatible AES-CBC cipher using password as IV.
     *
     * @param config the cipher configuration
     */
    public MuleSoftAesCbcPasswordIvCipher(CipherConfig config) {
        super(config);
    }

    @Override
    protected byte[] doEncrypt(byte[] plaintext, SecretKey key) throws Exception {
        return initCipher(javax.crypto.Cipher.ENCRYPT_MODE, key, passwordIv()).doFinal(plaintext);
    }

    @Override
    protected byte[] doDecrypt(byte[] payload, SecretKey key) throws Exception {
        return initCipher(javax.crypto.Cipher.DECRYPT_MODE, key, passwordIv()).doFinal(payload);
    }

    /**
     * MuleSoft default mode: the first 16 characters of the password act as IV.
     */
    private byte[] passwordIv() {
        return config.getKeyConfig().getMasterPassword().substring(0, IV_LENGTH).getBytes(StandardCharsets.UTF_8);
    }
}
