package com.dataliquid.passwordsuite.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigGroup;
import com.dataliquid.passwordsuite.domain.ConfigTree;

class ExportServiceTest {

    private final ExportService service = new ExportService();

    @Test
    void shouldExportFlatYaml() {
        ConfigTree tree = new ConfigTree(ConfigFormat.YAML);
        tree.getRoot().addChild(new ConfigEntry("host", "localhost"));
        tree.getRoot().addChild(new ConfigEntry("port", "5432"));

        String yaml = service.exportToYaml(tree);

        assertThat(yaml).contains("host: localhost");
        assertThat(yaml).contains("port: '5432'");
    }

    @Test
    void shouldExportNestedYaml() {
        ConfigTree tree = new ConfigTree(ConfigFormat.YAML);
        ConfigGroup database = new ConfigGroup("database");
        database.addChild(new ConfigEntry("host", "localhost"));
        database.addChild(new ConfigEntry("port", "5432"));
        tree.getRoot().addChild(database);

        String yaml = service.exportToYaml(tree);

        assertThat(yaml).contains("database:");
        assertThat(yaml).contains("host: localhost");
        assertThat(yaml).contains("port: '5432'");
    }

    @Test
    void shouldExportEncryptedValues() {
        ConfigTree tree = new ConfigTree(ConfigFormat.YAML);
        ConfigEntry password = new ConfigEntry("password", "ENC[base64data]");
        password.setEncrypted(true);
        tree.getRoot().addChild(password);

        String yaml = service.exportToYaml(tree);

        assertThat(yaml).contains("password: ENC[base64data]");
    }

    @Test
    void shouldExportFlatProperties() {
        ConfigTree tree = new ConfigTree(ConfigFormat.PROPERTIES);
        tree.getRoot().addChild(new ConfigEntry("host", "localhost"));
        tree.getRoot().addChild(new ConfigEntry("port", "5432"));

        String props = service.exportToProperties(tree);

        assertThat(props).contains("host=localhost");
        assertThat(props).contains("port=5432");
    }

    @Test
    void shouldExportNestedPropertiesWithDotNotation() {
        ConfigTree tree = new ConfigTree(ConfigFormat.PROPERTIES);
        ConfigGroup database = new ConfigGroup("database");
        database.addChild(new ConfigEntry("host", "localhost"));
        database.addChild(new ConfigEntry("port", "5432"));
        tree.getRoot().addChild(database);

        String props = service.exportToProperties(tree);

        assertThat(props).contains("database.host=localhost");
        assertThat(props).contains("database.port=5432");
    }

    @Test
    void shouldExportUsingOriginalFormat() {
        ConfigTree yamlTree = new ConfigTree(ConfigFormat.YAML);
        yamlTree.getRoot().addChild(new ConfigEntry("key", "value"));

        ConfigTree propsTree = new ConfigTree(ConfigFormat.PROPERTIES);
        propsTree.getRoot().addChild(new ConfigEntry("key", "value"));

        String yamlExport = service.export(yamlTree);
        String propsExport = service.export(propsTree);

        assertThat(yamlExport).contains("key: value");
        assertThat(propsExport).contains("key=value");
    }

    @Test
    void shouldHandleDeepNesting() {
        ConfigTree tree = new ConfigTree(ConfigFormat.PROPERTIES);
        ConfigGroup level1 = new ConfigGroup("level1");
        ConfigGroup level2 = new ConfigGroup("level2");
        ConfigGroup level3 = new ConfigGroup("level3");
        level3.addChild(new ConfigEntry("value", "deep"));
        level2.addChild(level3);
        level1.addChild(level2);
        tree.getRoot().addChild(level1);

        String props = service.exportToProperties(tree);

        assertThat(props).contains("level1.level2.level3.value=deep");
    }
}
