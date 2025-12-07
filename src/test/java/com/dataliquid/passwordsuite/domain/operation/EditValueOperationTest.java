package com.dataliquid.passwordsuite.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;

class EditValueOperationTest {

    @Test
    void shouldExecuteOperation() {
        ConfigEntry entry = new ConfigEntry("key", "oldValue");
        EditValueOperation operation = new EditValueOperation(entry, "newValue", false);

        operation.execute();

        assertThat(entry.getValue()).isEqualTo("newValue");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldUndoOperation() {
        ConfigEntry entry = new ConfigEntry("key", "oldValue");
        EditValueOperation operation = new EditValueOperation(entry, "newValue", false);

        operation.execute();
        operation.undo();

        assertThat(entry.getValue()).isEqualTo("oldValue");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldHandleEncryptionFlag() {
        ConfigEntry entry = new ConfigEntry("password", "secret");
        EditValueOperation operation = new EditValueOperation(entry, "ENC[data]", true);

        operation.execute();

        assertThat(entry.getValue()).isEqualTo("ENC[data]");
        assertThat(entry.isEncrypted()).isTrue();

        operation.undo();

        assertThat(entry.getValue()).isEqualTo("secret");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldHaveDescription() {
        ConfigEntry entry = new ConfigEntry("key", "value");
        EditValueOperation operation = new EditValueOperation(entry, "newValue", false);

        assertThat(operation.getDescription()).contains("key");
    }
}
