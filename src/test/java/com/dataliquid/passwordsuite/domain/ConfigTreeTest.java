package com.dataliquid.passwordsuite.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ConfigTreeTest {

    @Test
    void shouldCreateConfigTree() {
        ConfigTree tree = new ConfigTree(ConfigFormat.YAML);

        assertThat(tree.getFormat()).isEqualTo(ConfigFormat.YAML);
        assertThat(tree.getRoot()).isNotNull();
        assertThat(tree.getRoot().getKey()).isEqualTo("root");
    }

    @Test
    void shouldFindByPath() {
        ConfigTree tree = new ConfigTree(ConfigFormat.YAML);
        ConfigGroup database = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");

        tree.getRoot().addChild(database);
        database.addChild(host);

        Optional<ConfigNode> found = tree.findByPath("database.host");

        assertThat(found).isPresent();
        assertThat(found.get()).isSameAs(host);
    }

    @Test
    void shouldGetAllEntries() {
        ConfigTree tree = new ConfigTree(ConfigFormat.YAML);
        ConfigGroup database = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");
        ConfigEntry port = new ConfigEntry("port", "5432");

        tree.getRoot().addChild(database);
        database.addChild(host);
        database.addChild(port);

        List<ConfigEntry> entries = tree.getAllEntries();

        assertThat(entries).containsExactly(host, port);
        assertThat(tree.getEntryCount()).isEqualTo(2);
    }

    @Test
    void shouldCreateDeepCopy() {
        ConfigTree original = new ConfigTree(ConfigFormat.YAML);
        ConfigGroup database = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");

        original.getRoot().addChild(database);
        database.addChild(host);

        ConfigTree copy = original.deepCopy();

        assertThat(copy.getFormat()).isEqualTo(original.getFormat());
        assertThat(copy.getEntryCount()).isEqualTo(original.getEntryCount());
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getRoot()).isNotSameAs(original.getRoot());
    }
}
