package com.dataliquid.passwordsuite.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.config.KeyConfig;

class AesGcmCipherTest {

    private AesGcmCipher cipher;
    private CipherConfig cipherConfig;
    private static final String TEST_PASSWORD = "test-password-123";
    private static final String TEST_PLAINTEXT = "Hello, World!";

    @BeforeEach
    void setUp() throws CryptoException {
        KeyConfig keyConfig = new KeyConfig(TEST_PASSWORD);
        cipherConfig = new CipherConfig("AES", "GCM", "NoPadding", 256, "ENC[", "]", keyConfig);
        cipher = new AesGcmCipher(cipherConfig);
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() throws CryptoException {
        String encrypted = cipher.encrypt(TEST_PLAINTEXT);
        assertThat(encrypted).isNotEqualTo(TEST_PLAINTEXT);
        assertThat(encrypted).startsWith("ENC[");
        assertThat(encrypted).endsWith("]");

        String decrypted = cipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldDecryptWithNewCipherInstance() throws CryptoException {
        // Encrypt with original cipher
        String encrypted = cipher.encrypt(TEST_PLAINTEXT);

        // Create a new cipher instance with same config
        AesGcmCipher newCipher = new AesGcmCipher(cipherConfig);

        // Should be able to decrypt with new instance (salt is stored in output)
        String decrypted = newCipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldProduceDifferentCiphertextForSameInput() throws CryptoException {
        String encrypted1 = cipher.encrypt(TEST_PLAINTEXT);
        String encrypted2 = cipher.encrypt(TEST_PLAINTEXT);

        // Should be different due to random salt and IV
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both should decrypt to same plaintext
        assertThat(cipher.decrypt(encrypted1)).isEqualTo(TEST_PLAINTEXT);
        assertThat(cipher.decrypt(encrypted2)).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldHandleEmptyString() throws CryptoException {
        String encrypted = cipher.encrypt("");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("");
    }

    @Test
    void shouldHandleSpecialCharacters() throws CryptoException {
        String special = "äöü€@#$%^&*(){}[]|\\<>?/~`";
        String encrypted = cipher.encrypt(special);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(special);
    }

    @Test
    void shouldHandleLongText() throws CryptoException {
        String longText = "Lorem ipsum ".repeat(100);
        String encrypted = cipher.encrypt(longText);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(longText);
    }
}
