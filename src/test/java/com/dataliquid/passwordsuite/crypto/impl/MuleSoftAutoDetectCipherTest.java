package com.dataliquid.passwordsuite.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;
import com.dataliquid.passwordsuite.crypto.config.KeyConfig;

/**
 * Tests for the MuleSoft AES-CBC cipher with IV mode auto-detection.
 * <p>
 * All reference values were generated with MuleSoft's Secure Properties Tool
 * (secure-properties-tool.jar), with and without {@code --use-random-iv}.
 * </p>
 */
class MuleSoftAutoDetectCipherTest {

    private static final String AES128_PASSWORD = "1234567890123456";
    private static final String AES256_PASSWORD = "12345678901234567890123456789012";

    private MuleSoftAutoDetectCipher cipher;

    @BeforeEach
    void setUp() {
        cipher = createCipher(AES128_PASSWORD);
    }

    private MuleSoftAutoDetectCipher createCipher(String password) {
        KeyConfig keyConfig = new KeyConfig(password);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
        return new MuleSoftAutoDetectCipher(config);
    }

    @Test
    void shouldDecryptPasswordIvValueFromMuleSoftTool() throws CryptoException {
        // secure-properties-tool.jar string encrypt AES CBC 1234567890123456 secret123
        assertThat(cipher.decrypt("![zTq43fPvTIoKq2mx+YQ6PA==]")).isEqualTo("secret123");
    }

    @Test
    void shouldDecryptRandomIvValueFromMuleSoftTool() throws CryptoException {
        // secure-properties-tool.jar ... secret123 --use-random-iv
        assertThat(cipher.decrypt("![y3VTImLJh47XE65MvrFqpCviQ5TfPbVYcXgx4Np9Wug=]")).isEqualTo("secret123");
    }

    @Test
    void shouldDecryptAmbiguousLengthPasswordIvValue() throws CryptoException {
        // Password-IV value with plaintext > 16 bytes: payload is 32 bytes, so it
        // could structurally also be a random-IV value. Detection must still pick
        // password-IV because the full plaintext is readable text.
        // secure-properties-tool.jar ... verylongpassword123
        assertThat(cipher.decrypt("![LBdZkoLcSzSPWqARlO3fwwTeQyhxYd/kQscnzlTy50U=]")).isEqualTo("verylongpassword123");
    }

    @Test
    void shouldDecryptBothModesWithAes256Key() throws CryptoException {
        MuleSoftAutoDetectCipher aes256 = createCipher(AES256_PASSWORD);

        // secure-properties-tool.jar with 32-char key, without and with --use-random-iv
        assertThat(aes256.decrypt("![lC2ckrBwrHM2+Vsd7zSPFg==]")).isEqualTo("secret");
        assertThat(aes256.decrypt("![Vvccc+cMALLHNsIv7dvPUd4LpD2F387vtWI9L5vT8zs=]")).isEqualTo("secret");
        assertThat(aes256.decrypt("![BZzQ1oQ+PI9dquh2Ewsih6TL3nlxm9IPvy4dqaUlUL8=]")).isEqualTo("verylongpassword123");
        assertThat(aes256.decrypt("![OcnOCJ5V3rx58o4yZJ9NoVX8/5pzAI5whNRdnDijW++os3bcSXZuvZYYvto36wu4]"))
                .isEqualTo("verylongpassword123");
    }

    @Test
    void shouldEncryptWithRandomIv() throws CryptoException {
        String encrypted1 = cipher.encrypt("hello");
        String encrypted2 = cipher.encrypt("hello");

        // Random IV: same input must yield different ciphertexts
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // Both must decrypt via auto-detection and via the explicit random-IV cipher
        assertThat(cipher.decrypt(encrypted1)).isEqualTo("hello");
        assertThat(cipher.decrypt(encrypted2)).isEqualTo("hello");
    }

    @Test
    void shouldRoundTripValuesFromBothExplicitCiphers() throws CryptoException {
        KeyConfig keyConfig = new KeyConfig(AES128_PASSWORD);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
        MuleSoftRandomIvCipher randomIv = new MuleSoftRandomIvCipher(config);
        MuleSoftPasswordIvCipher passwordIv = new MuleSoftPasswordIvCipher(config);

        String plaintext = "äöü€ Sonderzeichen und = : Trenner";
        assertThat(cipher.decrypt(randomIv.encrypt(plaintext))).isEqualTo(plaintext);
        assertThat(cipher.decrypt(passwordIv.encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void shouldHandleEmptyString() throws CryptoException {
        assertThat(cipher.decrypt(cipher.encrypt(""))).isEmpty();
    }

    @Test
    void shouldRejectWrongPassword() {
        MuleSoftAutoDetectCipher wrongPassword = createCipher("9999999999999999");

        assertThatThrownBy(() -> wrongPassword.decrypt("![y3VTImLJh47XE65MvrFqpCviQ5TfPbVYcXgx4Np9Wug=]"))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void shouldRejectNullEncryptedText() {
        assertThatThrownBy(() -> cipher.decrypt(null)).isInstanceOf(CryptoException.class).hasMessageContaining("null");
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThatThrownBy(() -> cipher.decrypt("no-wrapper-here"))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("Invalid");
    }

    /**
     * Plaintext lengths at the AES block boundaries change the payload structure
     * (16 vs. 32 vs. 48 bytes) and thereby which detection path is taken.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 15, 16, 17, 31, 32, 33, 64 })
    void shouldDetectBothModesAtBlockBoundaryLengths(int length) throws CryptoException {
        String plaintext = "x".repeat(length);

        for (String password : new String[] { AES128_PASSWORD, AES256_PASSWORD }) {
            MuleSoftAutoDetectCipher autoCipher = createCipher(password);
            KeyConfig keyConfig = new KeyConfig(password);
            CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);

            String randomIvValue = new MuleSoftRandomIvCipher(config).encrypt(plaintext);
            String passwordIvValue = new MuleSoftPasswordIvCipher(config).encrypt(plaintext);

            assertThat(autoCipher.decrypt(randomIvValue)).isEqualTo(plaintext);
            assertThat(autoCipher.decrypt(passwordIvValue)).isEqualTo(plaintext);
        }
    }

    @Test
    void shouldDetectManyRandomPrintableValues() throws CryptoException {
        SecureRandom random = new SecureRandom();

        for (String password : new String[] { AES128_PASSWORD, AES256_PASSWORD }) {
            MuleSoftAutoDetectCipher autoCipher = createCipher(password);
            KeyConfig keyConfig = new KeyConfig(password);
            CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
            MuleSoftRandomIvCipher randomIv = new MuleSoftRandomIvCipher(config);
            MuleSoftPasswordIvCipher passwordIv = new MuleSoftPasswordIvCipher(config);

            for (int i = 0; i < 250; i++) {
                int length = 1 + random.nextInt(40);
                StringBuilder sb = new StringBuilder(length);
                for (int j = 0; j < length; j++) {
                    sb.append((char) (0x20 + random.nextInt(0x5F)));
                }
                String plaintext = sb.toString();

                assertThat(autoCipher.decrypt(randomIv.encrypt(plaintext))).isEqualTo(plaintext);
                assertThat(autoCipher.decrypt(passwordIv.encrypt(plaintext))).isEqualTo(plaintext);
            }
        }
    }

    @Test
    void shouldHandleMultilineAndTabValues() throws CryptoException {
        KeyConfig keyConfig = new KeyConfig(AES128_PASSWORD);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);

        String plaintext = "line one\nline two\twith tab\r\nline three - long enough to cross a block";
        assertThat(cipher.decrypt(new MuleSoftRandomIvCipher(config).encrypt(plaintext))).isEqualTo(plaintext);
        assertThat(cipher.decrypt(new MuleSoftPasswordIvCipher(config).encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void shouldHandleMultibyteOnlyPlaintextCrossingBlockBoundary() throws CryptoException {
        KeyConfig keyConfig = new KeyConfig(AES128_PASSWORD);
        CipherConfig config = new CipherConfig("AES", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);

        // 12 umlauts = 24 UTF-8 bytes; the 16-byte block boundary falls inside a
        // multi-byte character
        String plaintext = "äöüäöüäöüäöü";
        assertThat(cipher.decrypt(new MuleSoftRandomIvCipher(config).encrypt(plaintext))).isEqualTo(plaintext);
        assertThat(cipher.decrypt(new MuleSoftPasswordIvCipher(config).encrypt(plaintext))).isEqualTo(plaintext);
    }

    @Test
    void shouldRejectUndetectableBinaryPlaintext() throws Exception {
        // Binary plaintext >= 16 bytes encrypted in password-IV mode is the
        // documented limit of the heuristic: detection must fail loudly instead
        // of guessing. 0xFF is invalid UTF-8 at every position, so both
        // interpretations are guaranteed to be implausible.
        byte[] binary = new byte[24];
        java.util.Arrays.fill(binary, (byte) 0xFF);

        javax.crypto.Cipher rawCipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        rawCipher
                .init(javax.crypto.Cipher.ENCRYPT_MODE,
                        new javax.crypto.spec.SecretKeySpec(AES128_PASSWORD.getBytes(StandardCharsets.UTF_8), "AES"),
                        new javax.crypto.spec.IvParameterSpec(AES128_PASSWORD.getBytes(StandardCharsets.UTF_8)));
        String encrypted = "![" + Base64.getEncoder().encodeToString(rawCipher.doFinal(binary)) + "]";

        assertThatThrownBy(() -> cipher.decrypt(encrypted))
                .isInstanceOf(CryptoException.class)
                .hasMessageContaining("auto-detect");
    }

    @Test
    void shouldAutoDetectBlowfishBothModesFromMuleSoftTool() throws CryptoException {
        // 8-byte block size: detection works on one block of garbage instead of 16
        KeyConfig keyConfig = new KeyConfig(AES128_PASSWORD);
        CipherConfig config = new CipherConfig("Blowfish", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
        MuleSoftAutoDetectCipher blowfishAuto = new MuleSoftAutoDetectCipher(config);

        // secure-properties-tool.jar values, without and with --use-random-iv
        assertThat(blowfishAuto.decrypt("![jCN/KItxCMZkyVFhaBCbog==]")).isEqualTo("secret123");
        assertThat(blowfishAuto.decrypt("![Ea1vLjm3PbZ2C9Qu/eaVQv/eg6F9g0IM]")).isEqualTo("secret123");
    }

    @Test
    void shouldRoundTripBlowfishValuesFromBothExplicitCiphers() throws CryptoException {
        KeyConfig keyConfig = new KeyConfig(AES128_PASSWORD);
        CipherConfig config = new CipherConfig("Blowfish", "CBC", "PKCS5Padding", 128, "![", "]", keyConfig);
        MuleSoftAutoDetectCipher blowfishAuto = new MuleSoftAutoDetectCipher(config);

        String plaintext = "blowfish auto-detect value";
        assertThat(blowfishAuto.decrypt(new MuleSoftRandomIvCipher(config).encrypt(plaintext))).isEqualTo(plaintext);
        assertThat(blowfishAuto.decrypt(new MuleSoftPasswordIvCipher(config).encrypt(plaintext))).isEqualTo(plaintext);
    }
}
