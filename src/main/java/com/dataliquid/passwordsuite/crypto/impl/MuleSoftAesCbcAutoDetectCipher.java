package com.dataliquid.passwordsuite.crypto.impl;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import com.dataliquid.passwordsuite.crypto.Cipher;
import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.CipherConfig;

/**
 * MuleSoft-compatible AES-CBC cipher that auto-detects the IV mode on decrypt.
 * <p>
 * MuleSoft's encrypted format carries no marker for whether a value was
 * encrypted with the default mode (password as IV) or with
 * {@code --use-random-iv} (random IV prepended to the ciphertext). This cipher
 * detects the mode from the decrypted payload:
 * </p>
 * <ol>
 * <li>A 16-byte payload can only be password-IV (random IV needs at least 32
 * bytes: IV + one cipher block).</li>
 * <li>Otherwise the payload is decrypted in password-IV mode. Because both
 * modes share the same key and a CBC IV only affects the first plaintext block,
 * this never fails for either mode: for a password-IV value it yields the full
 * plaintext, for a random-IV value it yields 16 garbage bytes followed by the
 * exact random-IV plaintext.</li>
 * <li>If the full result is plausible text, the value is password-IV. If only
 * the result minus its first 16 bytes is plausible text, the value is
 * random-IV.</li>
 * </ol>
 * <p>
 * Encryption always uses the random-IV variant (the semantically secure mode).
 * </p>
 */
public class MuleSoftAesCbcAutoDetectCipher implements Cipher {

    private static final int IV_LENGTH = 16; // 128 bits for AES block size
    private static final char FIRST_PRINTABLE = 0x20; // below: control characters
    private static final char DELETE_CHAR = 0x7F;

    private final CipherConfig config;
    private final MuleSoftAesCbcCipher randomIvCipher;
    private final MuleSoftAesCbcPasswordIvCipher passwordIvCipher;

    /**
     * Creates a MuleSoft-compatible AES-CBC cipher with IV mode auto-detection.
     *
     * @param config the cipher configuration
     */
    public MuleSoftAesCbcAutoDetectCipher(CipherConfig config) {
        this.config = config;
        this.randomIvCipher = new MuleSoftAesCbcCipher(config);
        this.passwordIvCipher = new MuleSoftAesCbcPasswordIvCipher(config);
    }

    @Override
    public String encrypt(String plaintext) throws CryptoException {
        return randomIvCipher.encrypt(plaintext);
    }

    @Override
    public String decrypt(String encrypted) throws CryptoException {
        if (encrypted == null) {
            throw new CryptoException("Encrypted text cannot be null");
        }

        byte[] payload = decodePayload(encrypted);

        // A random-IV payload needs at least IV (16) + one cipher block (16)
        if (payload.length < IV_LENGTH + IV_LENGTH) {
            return passwordIvCipher.decrypt(encrypted);
        }

        // Decrypting in password-IV mode succeeds for both modes (same key; the
        // IV only affects the first block, and the PKCS5 padding on the last
        // block stays valid either way).
        byte[] passwordIvResult = passwordIvCipher.decryptToBytes(encrypted);

        if (isPlausibleText(passwordIvResult)) {
            return new String(passwordIvResult, StandardCharsets.UTF_8);
        }

        // For a random-IV value the password-IV result is 16 garbage bytes
        // followed by the exact random-IV plaintext.
        byte[] randomIvResult = Arrays.copyOfRange(passwordIvResult, IV_LENGTH, passwordIvResult.length);
        if (isPlausibleText(randomIvResult)) {
            return new String(randomIvResult, StandardCharsets.UTF_8);
        }

        throw new CryptoException(
                "Could not auto-detect MuleSoft IV mode: neither interpretation yields readable text. "
                        + "Check the password, or select the IV mode explicitly for binary values.");
    }

    /**
     * Decodes the wrapped Base64 payload without decrypting.
     */
    private byte[] decodePayload(String encrypted) throws CryptoException {
        try {
            return Base64.getDecoder().decode(config.unwrapValue(encrypted));
        } catch (IllegalArgumentException e) {
            throw new CryptoException("Invalid encrypted format: " + e.getMessage(), e);
        }
    }

    /**
     * Checks whether the bytes are strictly valid UTF-8 without control characters
     * (except tab, CR, LF). Sixteen bytes of AES output passing this check by
     * chance has a probability of roughly 1e-7.
     */
    private static boolean isPlausibleText(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String text;
        try {
            text = decoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean control = c < FIRST_PRINTABLE && c != '\t' && c != '\n' && c != '\r';
            if (control || c == DELETE_CHAR) {
                return false;
            }
        }
        return true;
    }
}
