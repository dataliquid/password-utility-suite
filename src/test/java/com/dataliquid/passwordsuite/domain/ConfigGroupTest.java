package com.dataliquid.passwordsuite.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ConfigGroupTest {

    @Test
    void shouldCreateConfigGroup() {
        ConfigGroup group = new ConfigGroup("database");

        assertThat(group.getKey()).isEqualTo("database");
        assertThat(group.getChildCount()).isZero();
        assertThat(group.isLeaf()).isFalse();
    }

    @Test
    void shouldAddChildren() {
        ConfigGroup group = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");
        ConfigEntry port = new ConfigEntry("port", "5432");

        group.addChild(host);
        group.addChild(port);

        assertThat(group.getChildCount()).isEqualTo(2);
        assertThat(group.getChildren()).containsExactly(host, port);
        assertThat(host.getParent()).isSameAs(group);
        assertThat(port.getParent()).isSameAs(group);
    }

    @Test
    void shouldRemoveChild() {
        ConfigGroup group = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");
        group.addChild(host);

        group.removeChild(host);

        assertThat(group.getChildCount()).isZero();
        assertThat(host.getParent()).isNull();
    }

    @Test
    void shouldFindChildByKey() {
        ConfigGroup group = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");
        group.addChild(host);

        Optional<ConfigNode> found = group.findChildByKey("host");

        assertThat(found).isPresent();
        assertThat(found.get()).isSameAs(host);
    }

    @Test
    void shouldFindByPath() {
        ConfigGroup root = new ConfigGroup("root");
        ConfigGroup database = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");

        root.addChild(database);
        database.addChild(host);

        Optional<ConfigNode> found = root.findByPath("database.host");

        assertThat(found).isPresent();
        assertThat(found.get()).isSameAs(host);
    }

    @Test
    void shouldGetAllEntries() {
        ConfigGroup root = new ConfigGroup("root");
        ConfigGroup database = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");
        ConfigEntry port = new ConfigEntry("port", "5432");

        root.addChild(database);
        database.addChild(host);
        database.addChild(port);

        List<ConfigEntry> entries = root.getAllEntries();

        assertThat(entries).containsExactly(host, port);
    }

    @Test
    void shouldCreateDeepCopy() {
        ConfigGroup original = new ConfigGroup("database");
        ConfigEntry host = new ConfigEntry("host", "localhost");
        original.addChild(host);

        ConfigGroup copy = original.deepCopy();

        assertThat(copy.getKey()).isEqualTo(original.getKey());
        assertThat(copy.getChildCount()).isEqualTo(original.getChildCount());
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getChildren().get(0)).isNotSameAs(host);
    }
}
