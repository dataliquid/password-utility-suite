package com.dataliquid.passwordsuite.crypto.impl;

/**
 * Password/key length rules for MuleSoft-compatible algorithms. MuleSoft uses
 * the password directly as the encryption key, so the allowed password length
 * is dictated by the algorithm's key sizes.
 */
public final class MuleSoftKeyRules {

    private MuleSoftKeyRules() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks whether a password length is a valid key length for the algorithm.
     *
     * @param  algorithm the JCE algorithm name (e.g., "AES")
     * @param  length    the password length in bytes
     *
     * @return           true if the length is valid
     */
    public static boolean isValidKeyLength(String algorithm, int length) {
        return switch (algorithm) {
        case "AES" -> length == 16 || length == 32;
        case "DESede" -> length == 24;
        case "Blowfish" -> length >= 8 && length <= 56;
        default -> true; // let the JCE reject unknown combinations
        };
    }

    /**
     * Human-readable password length requirement for an algorithm.
     *
     * @param  algorithm the JCE algorithm name
     *
     * @return           requirement text (e.g., "16 or 32")
     */
    public static String passwordRequirement(String algorithm) {
        return switch (algorithm) {
        case "AES" -> "16 or 32";
        case "DESede" -> "exactly 24";
        case "Blowfish" -> "8 to 56";
        default -> "an algorithm-specific number of";
        };
    }
}
