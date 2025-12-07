package com.dataliquid.passwordsuite.crypto.key;

import java.security.SecureRandom;

/**
 * Utility class for generating secure random data. Generates IVs, salts, and
 * other cryptographic random values.
 */
public class SecureKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a random initialization vector (IV).
     *
     * @param  length the length of the IV in bytes
     *
     * @return        random IV bytes
     */
    public static byte[] generateIV(int length) {
        byte[] iv = new byte[length];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Generates a random salt for key derivation.
     *
     * @param  length the length of the salt in bytes
     *
     * @return        random salt bytes
     */
    public static byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }
}
