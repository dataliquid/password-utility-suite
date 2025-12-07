package com.dataliquid.passwordsuite.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigFormat;
import com.dataliquid.passwordsuite.domain.ConfigTree;

class PropertiesConfigParserTest {

    private final PropertiesConfigParser parser = new PropertiesConfigParser();

    @Test
    void shouldParseFlatProperties() throws ParseException {
        String props = "host=localhost\n" + "port=5432\n" + "username=admin";

        ConfigTree tree = parser.parse(props);

        assertThat(tree.getFormat()).isEqualTo(ConfigFormat.PROPERTIES);
        assertThat(tree.getEntryCount()).isEqualTo(3);
    }

    @Test
    void shouldParseDotNotationAsHierarchy() throws ParseException {
        String props = "database.host=localhost\n" + "database.port=5432";

        ConfigTree tree = parser.parse(props);

        assertThat(tree.getEntryCount()).isEqualTo(2);
        assertThat(tree.findByPath("database.host")).isPresent();
        assertThat(tree.findByPath("database.port")).isPresent();

        ConfigEntry host = (ConfigEntry) tree.findByPath("database.host").orElseThrow();
        assertThat(host.getValue()).isEqualTo("localhost");
    }

    @Test
    void shouldDetectEncryptedValues() throws ParseException {
        String props = "database.password=ENC[base64data]";

        ConfigTree tree = parser.parse(props);

        ConfigEntry entry = (ConfigEntry) tree.findByPath("database.password").orElseThrow();
        assertThat(entry.isEncrypted()).isTrue();
        assertThat(entry.getValue()).isEqualTo("ENC[base64data]");
    }

    @Test
    void shouldAutoMarkEncryptedValuesForEncryption() throws ParseException {
        String props = "database.password=ENC[base64data]\n" + "database.host=localhost";

        ConfigTree tree = parser.parse(props);

        ConfigEntry passwordEntry = (ConfigEntry) tree.findByPath("database.password").orElseThrow();
        assertThat(passwordEntry.isMarkedForEncryption()).isTrue();

        ConfigEntry hostEntry = (ConfigEntry) tree.findByPath("database.host").orElseThrow();
        assertThat(hostEntry.isMarkedForEncryption()).isFalse();
    }

    @Test
    void shouldThrowExceptionForEmptyContent() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForNullContent() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldGetSupportedFormat() {
        assertThat(parser.getSupportedFormat()).isEqualTo(ConfigFormat.PROPERTIES);
    }

    @Test
    void shouldHandleDeepNesting() throws ParseException {
        String props = "level1.level2.level3.value=deep";

        ConfigTree tree = parser.parse(props);

        assertThat(tree.findByPath("level1.level2.level3.value")).isPresent();
        ConfigEntry entry = (ConfigEntry) tree.findByPath("level1.level2.level3.value").orElseThrow();
        assertThat(entry.getValue()).isEqualTo("deep");
    }

    @Test
    void shouldHandleMixedHierarchyLevels() throws ParseException {
        String props = "app.name=MyApp\n" + "app.database.host=localhost\n" + "app.database.port=5432";

        ConfigTree tree = parser.parse(props);

        assertThat(tree.getEntryCount()).isEqualTo(3);
        assertThat(tree.findByPath("app.name")).isPresent();
        assertThat(tree.findByPath("app.database.host")).isPresent();
        assertThat(tree.findByPath("app.database.port")).isPresent();
    }
}
