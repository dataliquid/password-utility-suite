package com.dataliquid.passwordsuite.domain;

/**
 * Represents a leaf node in the configuration tree (key-value pair). Can be
 * encrypted or plain text.
 */
public class ConfigEntry extends ConfigNode {

    private String value;
    private boolean encrypted;
    private boolean markedForEncryption;
    private int lineNumber = -1; // -1 means no line number tracked

    public ConfigEntry() {
        super();
    }

    public ConfigEntry(String key, String value) {
        super(key);
        this.value = value;
        this.encrypted = false;
    }

    /**
     * Returns the value of this entry. If encrypted, returns the encrypted payload
     * (e.g., "ENC[...]").
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this entry.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns true if this entry is encrypted.
     */
    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * Marks this entry as encrypted and sets the encrypted payload. The encrypted
     * flag is automatically set based on the value format.
     */
    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * Convenience method to mark this entry as encrypted with a payload.
     */
    public void markAsEncrypted(String encryptedPayload) {
        this.value = encryptedPayload;
        this.encrypted = true;
    }

    /**
     * Convenience method to mark this entry as plain text.
     */
    public void markAsPlainText(String plainValue) {
        this.value = plainValue;
        this.encrypted = false;
    }

    /**
     * Returns the line number where this entry appears in the source file. Returns
     * -1 if no line number is tracked.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the line number where this entry appears in the source file.
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns true if this entry is marked for encryption.
     */
    public boolean isMarkedForEncryption() {
        return markedForEncryption;
    }

    /**
     * Sets whether this entry is marked for encryption.
     */
    public void setMarkedForEncryption(boolean marked) {
        this.markedForEncryption = marked;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public ConfigEntry deepCopy() {
        ConfigEntry copy = new ConfigEntry(this.key, this.value);
        copy.encrypted = this.encrypted;
        copy.markedForEncryption = this.markedForEncryption;
        copy.lineNumber = this.lineNumber;
        return copy;
    }

    @Override
    public String toString() {
        if (encrypted) {
            return key + " = " + value + " [ENC]";
        } else {
            return key + " = " + value;
        }
    }
}
