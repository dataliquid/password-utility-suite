package com.dataliquid.passwordsuite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.crypto.CryptoException;
import com.dataliquid.passwordsuite.crypto.config.KeyConfig;
import com.dataliquid.passwordsuite.crypto.factory.CipherFactory;
import com.dataliquid.passwordsuite.crypto.factory.CipherRegistry;
import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.operation.EditValueOperation;

class CryptoServiceTest {

    private CryptoService service;
    private static final String TEST_PASSWORD = "test-password-123";
    private static final String TEST_ALGORITHM = "AES-256-GCM";

    @BeforeEach
    void setUp() throws CryptoException {
        // Create CryptoService with CipherFactory
        KeyConfig keyConfig = new KeyConfig(TEST_PASSWORD);
        CipherRegistry registry = new CipherRegistry();
        CipherFactory cipherFactory = new CipherFactory(registry, keyConfig);

        service = new CryptoService(cipherFactory);
        service.setAlgorithm(TEST_ALGORITHM);
    }

    @Test
    void shouldCreateEncryptOperation() throws CryptoException {
        ConfigEntry entry = new ConfigEntry("password", "secret123");

        EditValueOperation operation = service.createEncryptOperation(entry);
        operation.execute();

        assertThat(entry.getValue()).startsWith("ENC[");
        assertThat(entry.getValue()).endsWith("]");
        assertThat(entry.isEncrypted()).isTrue();
        assertThat(entry.getValue()).isNotEqualTo("secret123");
    }

    @Test
    void shouldCreateDecryptOperation() throws CryptoException {
        ConfigEntry entry = new ConfigEntry("password", "secret123");

        // First encrypt
        EditValueOperation encryptOp = service.createEncryptOperation(entry);
        encryptOp.execute();
        String encrypted = entry.getValue();

        // Then decrypt
        EditValueOperation decryptOp = service.createDecryptOperation(entry);
        decryptOp.execute();

        assertThat(entry.getValue()).isEqualTo("secret123");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldEncryptAndDecryptCorrectly() throws CryptoException {
        ConfigEntry entry = new ConfigEntry("password", "mySecret");

        // Encrypt
        EditValueOperation encryptOp = service.createEncryptOperation(entry);
        encryptOp.execute();
        String encrypted = entry.getValue();
        assertThat(encrypted).startsWith("ENC[");
        assertThat(entry.isEncrypted()).isTrue();

        // Decrypt
        EditValueOperation decryptOp = service.createDecryptOperation(entry);
        decryptOp.execute();
        assertThat(entry.getValue()).isEqualTo("mySecret");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldSupportUndoRedoForEncryption() throws CryptoException {
        ConfigEntry entry = new ConfigEntry("password", "original");

        EditValueOperation encryptOp = service.createEncryptOperation(entry);
        encryptOp.execute();
        assertThat(entry.getValue()).startsWith("ENC[");
        assertThat(entry.isEncrypted()).isTrue();

        encryptOp.undo();
        assertThat(entry.getValue()).isEqualTo("original");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldSupportUndoRedoForDecryption() throws CryptoException {
        ConfigEntry entry = new ConfigEntry("password", "test");

        // First encrypt to get a valid encrypted value
        EditValueOperation encryptOp = service.createEncryptOperation(entry);
        encryptOp.execute();
        String encrypted = entry.getValue();

        // Now decrypt
        EditValueOperation decryptOp = service.createDecryptOperation(entry);
        decryptOp.execute();
        assertThat(entry.getValue()).isEqualTo("test");
        assertThat(entry.isEncrypted()).isFalse();

        // Undo decryption
        decryptOp.undo();
        assertThat(entry.getValue()).isEqualTo(encrypted);
        assertThat(entry.isEncrypted()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenNotConfigured() {
        CryptoService unconfiguredService = new CryptoService();
        ConfigEntry entry = new ConfigEntry("password", "secret");

        assertThatThrownBy(() -> {
            EditValueOperation op = unconfiguredService.createEncryptOperation(entry);
            op.execute();
        }).isInstanceOf(RuntimeException.class).hasMessageContaining("not configured");
    }

    @Test
    void shouldWorkWithDifferentAlgorithms() throws CryptoException {
        // Test with AES-256-CBC
        service.setAlgorithm("AES-256-CBC");

        ConfigEntry entry = new ConfigEntry("password", "testValue");

        EditValueOperation encryptOp = service.createEncryptOperation(entry);
        encryptOp.execute();
        String encrypted = entry.getValue();
        assertThat(encrypted).startsWith("ENC[");

        EditValueOperation decryptOp = service.createDecryptOperation(entry);
        decryptOp.execute();
        assertThat(entry.getValue()).isEqualTo("testValue");
    }

    @Test
    void shouldReturnCurrentAlgorithm() throws CryptoException {
        assertThat(service.getCurrentAlgorithm()).isEqualTo(TEST_ALGORITHM);

        service.setAlgorithm("Blowfish");
        assertThat(service.getCurrentAlgorithm()).isEqualTo("Blowfish");
    }
}
