package com.dataliquid.passwordsuite.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigFormat;

class ParserFactoryTest {

    @Test
    void shouldDetectYamlByDashes() {
        String yaml = "---\nhost: localhost";

        ConfigParser parser = ParserFactory.getParser(yaml);

        assertThat(parser).isInstanceOf(YamlConfigParser.class);
    }

    @Test
    void shouldDetectYamlByColonPattern() {
        String yaml = "host: localhost\nport: 5432";

        ConfigParser parser = ParserFactory.getParser(yaml);

        assertThat(parser).isInstanceOf(YamlConfigParser.class);
    }

    @Test
    void shouldDetectPropertiesByEqualsSign() {
        String props = "host=localhost\nport=5432";

        ConfigParser parser = ParserFactory.getParser(props);

        assertThat(parser).isInstanceOf(PropertiesConfigParser.class);
    }

    @Test
    void shouldDefaultToYamlForEmptyContent() {
        ConfigParser parser = ParserFactory.getParser("");

        assertThat(parser).isInstanceOf(YamlConfigParser.class);
    }

    @Test
    void shouldDefaultToYamlForNullContent() {
        ConfigParser parser = ParserFactory.getParser(null);

        assertThat(parser).isInstanceOf(YamlConfigParser.class);
    }

    @Test
    void shouldGetParserByFormat() {
        ConfigParser yamlParser = ParserFactory.getParser("any content", ConfigFormat.YAML);
        ConfigParser propsParser = ParserFactory.getParser("any content", ConfigFormat.PROPERTIES);

        assertThat(yamlParser).isInstanceOf(YamlConfigParser.class);
        assertThat(propsParser).isInstanceOf(PropertiesConfigParser.class);
    }
}
