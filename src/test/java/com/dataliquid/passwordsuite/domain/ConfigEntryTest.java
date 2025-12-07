package com.dataliquid.passwordsuite.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigEntryTest {

    @Test
    void shouldCreateConfigEntry() {
        ConfigEntry entry = new ConfigEntry("database.host", "localhost");

        assertThat(entry.getKey()).isEqualTo("database.host");
        assertThat(entry.getValue()).isEqualTo("localhost");
        assertThat(entry.isEncrypted()).isFalse();
        assertThat(entry.isLeaf()).isTrue();
    }

    @Test
    void shouldMarkAsEncrypted() {
        ConfigEntry entry = new ConfigEntry("password", "secret123");

        entry.markAsEncrypted("ENC[base64data]");

        assertThat(entry.getValue()).isEqualTo("ENC[base64data]");
        assertThat(entry.isEncrypted()).isTrue();
    }

    @Test
    void shouldMarkAsPlainText() {
        ConfigEntry entry = new ConfigEntry("password", "ENC[base64data]");
        entry.setEncrypted(true);

        entry.markAsPlainText("secret123");

        assertThat(entry.getValue()).isEqualTo("secret123");
        assertThat(entry.isEncrypted()).isFalse();
    }

    @Test
    void shouldCreateDeepCopy() {
        ConfigEntry original = new ConfigEntry("key", "value");
        original.markAsEncrypted("ENC[data]");

        ConfigEntry copy = original.deepCopy();

        assertThat(copy.getKey()).isEqualTo(original.getKey());
        assertThat(copy.getValue()).isEqualTo(original.getValue());
        assertThat(copy.isEncrypted()).isEqualTo(original.isEncrypted());
        assertThat(copy).isNotSameAs(original);
    }

    @Test
    void shouldNotBeMarkedForEncryptionByDefault() {
        ConfigEntry entry = new ConfigEntry("key", "value");

        assertThat(entry.isMarkedForEncryption()).isFalse();
    }

    @Test
    void shouldSetMarkedForEncryption() {
        ConfigEntry entry = new ConfigEntry("password", "secret");

        entry.setMarkedForEncryption(true);

        assertThat(entry.isMarkedForEncryption()).isTrue();
    }

    @Test
    void shouldToggleMarkedForEncryption() {
        ConfigEntry entry = new ConfigEntry("password", "secret");

        entry.setMarkedForEncryption(true);
        assertThat(entry.isMarkedForEncryption()).isTrue();

        entry.setMarkedForEncryption(false);
        assertThat(entry.isMarkedForEncryption()).isFalse();
    }

    @Test
    void shouldCopyMarkedForEncryptionInDeepCopy() {
        ConfigEntry original = new ConfigEntry("key", "value");
        original.setMarkedForEncryption(true);

        ConfigEntry copy = original.deepCopy();

        assertThat(copy.isMarkedForEncryption()).isTrue();
    }

    @Test
    void shouldGetPath() {
        ConfigGroup parent = new ConfigGroup("database");
        ConfigEntry entry = new ConfigEntry("host", "localhost");
        parent.addChild(entry);

        assertThat(entry.getPath()).isEqualTo("database.host");
    }
}
