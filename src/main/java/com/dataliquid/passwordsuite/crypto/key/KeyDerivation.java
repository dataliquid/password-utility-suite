package com.dataliquid.passwordsuite.crypto.key;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.dataliquid.passwordsuite.crypto.CryptoException;

/**
 * Utility class for key derivation using PBKDF2. Derives cryptographic keys
 * from passwords using salt and iterations.
 */
public class KeyDerivation {

    /**
     * Derives a secret key from a password using PBKDF2.
     *
     * @param  password        the password to derive the key from
     * @param  salt            the salt for key derivation
     * @param  keySize         the desired key size in bits (e.g., 256)
     * @param  iterations      the number of PBKDF2 iterations (recommended: 65536+)
     * @param  algorithm       the KDF algorithm (e.g., "PBKDF2WithHmacSHA256")
     * @param  keyAlgorithm    the target encryption algorithm (e.g., "AES",
     *                         "Blowfish")
     *
     * @return                 the derived secret key
     *
     * @throws CryptoException if key derivation fails
     */
    public static SecretKey deriveKey(String password, byte[] salt, int keySize, int iterations, String algorithm,
            String keyAlgorithm) throws CryptoException {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keySize);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();

            return new SecretKeySpec(keyBytes, keyAlgorithm);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException("Failed to derive key: " + e.getMessage(), e);
        }
    }

}
