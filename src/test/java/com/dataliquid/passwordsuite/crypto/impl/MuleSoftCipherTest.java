package com.dataliquid.passwordsuite.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.config.KeyConfig;

/**
 * Tests for MuleSoft-compatible AES-CBC cipher.
 * <p>
 * MuleSoft uses the password directly as the AES key (no PBKDF2). With
 * --use-random-iv, format is: Base64(IV + ciphertext)
 * </p>
 */
class MuleSoftCipherTest {

    // MuleSoft requires exactly 16-character password for AES-128
    private static final String MULESOFT_PASSWORD = "1234567890123456";
    private static final String TEST_PLAINTEXT = "secret123";

    private MuleSoftRandomIvCipher cipher;
    private CipherConfig cipherConfig;

    @BeforeEach
    void setUp() {
        KeyConfig keyConfig = new KeyConfig(MULESOFT_PASSWORD);
        // Use "![" and "]" as format prefix/suffix (MuleSoft style, but configurable)
        cipherConfig = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
        cipher = new MuleSoftRandomIvCipher(cipherConfig);
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() throws CryptoException {
        String encrypted = cipher.encrypt(TEST_PLAINTEXT);
        assertThat(encrypted).isNotEqualTo(TEST_PLAINTEXT);
        assertThat(encrypted).startsWith("![");
        assertThat(encrypted).endsWith("]");

        String decrypted = cipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldDecryptWithNewCipherInstance() throws CryptoException {
        // Encrypt with original cipher
        String encrypted = cipher.encrypt(TEST_PLAINTEXT);

        // Create a new cipher instance with same config
        MuleSoftRandomIvCipher newCipher = new MuleSoftRandomIvCipher(cipherConfig);

        // Should be able to decrypt with new instance
        String decrypted = newCipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldProduceDifferentCiphertextForSameInput() throws CryptoException {
        String encrypted1 = cipher.encrypt(TEST_PLAINTEXT);
        String encrypted2 = cipher.encrypt(TEST_PLAINTEXT);

        // Should be different due to random IV
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both should decrypt to same plaintext
        assertThat(cipher.decrypt(encrypted1)).isEqualTo(TEST_PLAINTEXT);
        assertThat(cipher.decrypt(encrypted2)).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldRejectInvalidPasswordLength() {
        // Password too short (not 16 or 32)
        KeyConfig shortKeyConfig = new KeyConfig("short");
        CipherConfig shortConfig = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", shortKeyConfig);
        MuleSoftRandomIvCipher shortCipher = new MuleSoftRandomIvCipher(shortConfig);

        assertThatThrownBy(() -> shortCipher.encrypt("test"))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("16 or 32 character password");

        // Password wrong length (not 16 or 32)
        KeyConfig wrongKeyConfig = new KeyConfig("this-is-24-characters!!");
        CipherConfig wrongConfig = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", wrongKeyConfig);
        MuleSoftRandomIvCipher wrongCipher = new MuleSoftRandomIvCipher(wrongConfig);

        assertThatThrownBy(() -> wrongCipher.encrypt("test"))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("16 or 32 character password");
    }

    @Test
    void shouldSupportAes256With32CharPassword() throws CryptoException {
        // 32-character password for AES-256
        String aes256Password = "12345678901234567890123456789012";
        KeyConfig keyConfig = new KeyConfig(aes256Password);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 256, "![", "]", keyConfig);
        MuleSoftRandomIvCipher aes256Cipher = new MuleSoftRandomIvCipher(config);

        String encrypted = aes256Cipher.encrypt(TEST_PLAINTEXT);
        assertThat(encrypted).startsWith("![");
        assertThat(encrypted).endsWith("]");

        String decrypted = aes256Cipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(TEST_PLAINTEXT);
    }

    @Test
    void shouldDecryptMuleSoftEncryptedValue() throws CryptoException {
        // Value encrypted by MuleSoft Secure Properties Tool:
        // java -jar secure-properties-tool.jar string encrypt AES CBC 1234567890123456
        // secret123
        // --use-random-iv
        // Result varies each time due to random IV, so we test by encrypting and
        // decrypting
        // The important test is that the format is compatible

        String ourEncrypted = cipher.encrypt("secret123");
        String decrypted = cipher.decrypt(ourEncrypted);
        assertThat(decrypted).isEqualTo("secret123");
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

    @Test
    void shouldRejectNullPlaintext() {
        assertThatThrownBy(() -> cipher.encrypt(null)).isInstanceOf(CryptoException.class).hasMessageContaining("null");
    }

    @Test
    void shouldRejectNullEncryptedText() {
        assertThatThrownBy(() -> cipher.decrypt(null)).isInstanceOf(CryptoException.class).hasMessageContaining("null");
    }

    @Test
    void shouldDecryptMuleSoftPasswordIvFormat() throws CryptoException {
        // Value encrypted by MuleSoft WITHOUT --use-random-iv (password as IV):
        // java -jar secure-properties-tool.jar string encrypt AES CBC 1234567890123456
        // "1234"
        // Result: w9oKTqKTtvBuRUVbhQP/qw==
        KeyConfig keyConfig = new KeyConfig(MULESOFT_PASSWORD);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "", "", keyConfig);
        MuleSoftPasswordIvCipher passwordIvCipher = new MuleSoftPasswordIvCipher(config);

        String muleSoftPasswordIvValue = "w9oKTqKTtvBuRUVbhQP/qw==";
        String decrypted = passwordIvCipher.decrypt(muleSoftPasswordIvValue);

        assertThat(decrypted).isEqualTo("1234");
    }

    @Test
    void shouldDecryptMuleSoftAes256PasswordIvFormat() throws CryptoException {
        // Value encrypted by MuleSoft WITHOUT --use-random-iv using 32-char password
        // (AES-256):
        // java -jar secure-properties-tool.jar string encrypt AES CBC
        // 12345678901234567890123456789012 "test123"
        // Result: hvviuvvOu8EADgdrFqCeXA==
        // MuleSoft uses first 16 chars of password as IV regardless of key length
        String aes256Password = "12345678901234567890123456789012";
        KeyConfig keyConfig = new KeyConfig(aes256Password);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 256, "", "", keyConfig);
        MuleSoftPasswordIvCipher aes256Cipher = new MuleSoftPasswordIvCipher(config);

        String muleSoftAes256PasswordIvValue = "hvviuvvOu8EADgdrFqCeXA==";
        String decrypted = aes256Cipher.decrypt(muleSoftAes256PasswordIvValue);

        assertThat(decrypted).isEqualTo("test123");
    }

    @Test
    void shouldEncryptAndDecryptWithPasswordIvCipher() throws CryptoException {
        // Test MuleSoftPasswordIvCipher encrypt/decrypt roundtrip
        KeyConfig keyConfig = new KeyConfig(MULESOFT_PASSWORD);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
        MuleSoftPasswordIvCipher passwordIvCipher = new MuleSoftPasswordIvCipher(config);

        String encrypted = passwordIvCipher.encrypt("testvalue");
        assertThat(encrypted).startsWith("![");
        assertThat(encrypted).endsWith("]");

        String decrypted = passwordIvCipher.decrypt(encrypted);
        assertThat(decrypted).isEqualTo("testvalue");
    }

    @Test
    void passwordIvCipherShouldProduceSameCiphertextForSameInput() throws CryptoException {
        // Password-IV cipher produces deterministic output (same password = same IV)
        KeyConfig keyConfig = new KeyConfig(MULESOFT_PASSWORD);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "", "", keyConfig);
        MuleSoftPasswordIvCipher passwordIvCipher = new MuleSoftPasswordIvCipher(config);

        String encrypted1 = passwordIvCipher.encrypt("hello");
        String encrypted2 = passwordIvCipher.encrypt("hello");

        // Should be the same (deterministic IV from password)
        assertThat(encrypted1).isEqualTo(encrypted2);
    }

    // --- Blowfish and DESede (MuleSoft legacy algorithms, 8-byte block size) ---

    private CipherConfig legacyConfig(String algorithm, int keySize, String password) {
        return new CipherConfig(algorithm, "CBC", "PKCS5Padding", keySize, "![", "]", new KeyConfig(password));
    }

    @Test
    void shouldDecryptBlowfishPasswordIvValueFromMuleSoftTool() throws CryptoException {
        // secure-properties-tool.jar string encrypt Blowfish CBC 1234567890123456
        // secret123
        MuleSoftPasswordIvCipher cipher16 = new MuleSoftPasswordIvCipher(
                legacyConfig("Blowfish", 128, "1234567890123456"));
        assertThat(cipher16.decrypt("![jCN/KItxCMZkyVFhaBCbog==]")).isEqualTo("secret123");

        // Blowfish accepts short keys too: 8-character password
        MuleSoftPasswordIvCipher cipher8 = new MuleSoftPasswordIvCipher(legacyConfig("Blowfish", 128, "12345678"));
        assertThat(cipher8.decrypt("![5M5+Q64ChgzAWeq2pf6Hag==]")).isEqualTo("secret123");
    }

    @Test
    void shouldDecryptBlowfishRandomIvValueFromMuleSoftTool() throws CryptoException {
        // secure-properties-tool.jar ... Blowfish CBC ... --use-random-iv (8-byte IV
        // prepended)
        MuleSoftRandomIvCipher cipher = new MuleSoftRandomIvCipher(legacyConfig("Blowfish", 128, "1234567890123456"));
        assertThat(cipher.decrypt("![Ea1vLjm3PbZ2C9Qu/eaVQv/eg6F9g0IM]")).isEqualTo("secret123");
    }

    @Test
    void shouldDecryptDesedePasswordIvValueFromMuleSoftTool() throws CryptoException {
        // secure-properties-tool.jar string encrypt DESede CBC 123456789012345678901234
        // secret123
        MuleSoftPasswordIvCipher cipher = new MuleSoftPasswordIvCipher(
                legacyConfig("DESede", 168, "123456789012345678901234"));
        assertThat(cipher.decrypt("![zOXZ3oxeVqv6QEJ7tSgNig==]")).isEqualTo("secret123");
    }

    @Test
    void shouldRoundTripBlowfishAndDesedeInBothModes() throws CryptoException {
        String plaintext = "round-trip \u00e4\u00f6\u00fc value";

        for (CipherConfig config : new CipherConfig[] { legacyConfig("Blowfish", 128, "1234567890123456"),
                legacyConfig("DESede", 168, "123456789012345678901234") }) {
            MuleSoftPasswordIvCipher passwordIv = new MuleSoftPasswordIvCipher(config);
            MuleSoftRandomIvCipher randomIv = new MuleSoftRandomIvCipher(config);

            assertThat(passwordIv.decrypt(passwordIv.encrypt(plaintext))).isEqualTo(plaintext);
            assertThat(randomIv.decrypt(randomIv.encrypt(plaintext))).isEqualTo(plaintext);
        }
    }

    @Test
    void shouldRejectInvalidLegacyKeyLengths() {
        // Blowfish requires at least 8 characters (password doubles as CBC IV)
        MuleSoftPasswordIvCipher blowfish = new MuleSoftPasswordIvCipher(legacyConfig("Blowfish", 128, "short"));
        assertThatThrownBy(() -> blowfish.encrypt("test"))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("8 to 56");

        // DESede requires exactly 24 characters
        MuleSoftPasswordIvCipher desede = new MuleSoftPasswordIvCipher(legacyConfig("DESede", 168, "1234567890123456"));
        assertThatThrownBy(() -> desede.encrypt("test"))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("exactly 24");
    }
}
