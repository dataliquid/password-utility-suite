package com.dataliquid.passwordsuite.crypto.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.KeyConfig;

class CipherFactoryTest {

    private CipherFactory factory;
    private static final String TEST_PASSWORD = "test-password-123";

    @BeforeEach
    void setUp() {
        KeyConfig keyConfig = new KeyConfig(TEST_PASSWORD);
        CipherRegistry registry = new CipherRegistry();
        factory = new CipherFactory(registry, keyConfig);
    }

    @Test
    void shouldCreateAesGcmCipher() throws CryptoException {
        Cipher cipher = factory.createCipher("AES-256-GCM");
        assertThat(cipher).isNotNull();

        String plaintext = "Test message";
        String encrypted = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void shouldCreateAesCbcCipher() throws CryptoException {
        Cipher cipher = factory.createCipher("AES-256-CBC");
        assertThat(cipher).isNotNull();

        String plaintext = "Test message";
        String encrypted = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void shouldCreateBlowfishCipher() throws CryptoException {
        Cipher cipher = factory.createCipher("Blowfish");
        assertThat(cipher).isNotNull();

        String plaintext = "Test message";
        String encrypted = cipher.encrypt(plaintext);
        String decrypted = cipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void shouldThrowExceptionForUnknownAlgorithm() {
        assertThatThrownBy(() -> factory.createCipher("Unknown-Algorithm"))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("Unknown algorithm");
    }
}
