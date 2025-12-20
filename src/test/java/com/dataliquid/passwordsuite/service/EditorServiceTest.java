package com.dataliquid.passwordsuite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.swing.JTextArea;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigTree;
import com.dataliquid.passwordsuite.domain.operation.EditValueOperation;
import com.dataliquid.passwordsuite.parser.ParseException;

class EditorServiceTest {

    private final EditorService service = new EditorService();

    @Test
    void shouldParseYamlContent() throws ParseException {
        String yaml = "host: localhost\nport: 5432";

        ConfigTree tree = service.parseContent(yaml);

        assertThat(tree).isNotNull();
        assertThat(tree.getEntryCount()).isEqualTo(2);
        assertThat(service.getCurrentTree()).isSameAs(tree);
    }

    @Test
    void shouldParsePropertiesContent() throws ParseException {
        String props = "host=localhost\nport=5432";

        ConfigTree tree = service.parseContent(props);

        assertThat(tree).isNotNull();
        assertThat(tree.getEntryCount()).isEqualTo(2);
    }

    @Test
    void shouldThrowExceptionForInvalidContent() {
        assertThatThrownBy(() -> service.parseContent("")).isInstanceOf(ParseException.class);
    }

    @Test
    void shouldExecuteOperation() throws ParseException {
        service.parseContent("key: value");
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("key").orElseThrow();

        EditValueOperation operation = new EditValueOperation(entry, "newValue", false);
        service.executeOperation(operation);

        assertThat(entry.getValue()).isEqualTo("newValue");
        assertThat(service.canUndo()).isTrue();
        assertThat(service.getUndoStackSize()).isEqualTo(1);
    }

    @Test
    void shouldUndoOperation() throws ParseException {
        service.parseContent("key: oldValue");
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("key").orElseThrow();

        EditValueOperation operation = new EditValueOperation(entry, "newValue", false);
        service.executeOperation(operation);
        service.undo();

        assertThat(entry.getValue()).isEqualTo("oldValue");
        assertThat(service.canRedo()).isTrue();
        assertThat(service.getRedoStackSize()).isEqualTo(1);
    }

    @Test
    void shouldRedoOperation() throws ParseException {
        service.parseContent("key: oldValue");
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("key").orElseThrow();

        EditValueOperation operation = new EditValueOperation(entry, "newValue", false);
        service.executeOperation(operation);
        service.undo();
        service.redo();

        assertThat(entry.getValue()).isEqualTo("newValue");
        assertThat(service.canUndo()).isTrue();
        assertThat(service.canRedo()).isFalse();
    }

    @Test
    void shouldClearRedoStackOnNewOperation() throws ParseException {
        service.parseContent("key: value1");
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("key").orElseThrow();

        service.executeOperation(new EditValueOperation(entry, "value2", false));
        service.undo();
        assertThat(service.canRedo()).isTrue();

        service.executeOperation(new EditValueOperation(entry, "value3", false));
        assertThat(service.canRedo()).isFalse();
    }

    @Test
    void shouldClearUndoRedoOnParse() throws ParseException {
        service.parseContent("key: value1");
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("key").orElseThrow();

        service.executeOperation(new EditValueOperation(entry, "value2", false));
        assertThat(service.canUndo()).isTrue();

        service.parseContent("newKey: newValue");
        assertThat(service.canUndo()).isFalse();
        assertThat(service.canRedo()).isFalse();
    }

    @Test
    void shouldHandleMultipleUndoRedo() throws ParseException {
        service.parseContent("key: value1");
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("key").orElseThrow();

        service.executeOperation(new EditValueOperation(entry, "value2", false));
        service.executeOperation(new EditValueOperation(entry, "value3", false));
        service.executeOperation(new EditValueOperation(entry, "value4", false));

        assertThat(service.getUndoStackSize()).isEqualTo(3);

        service.undo();
        service.undo();
        assertThat(entry.getValue()).isEqualTo("value2");
        assertThat(service.getRedoStackSize()).isEqualTo(2);

        service.redo();
        assertThat(entry.getValue()).isEqualTo("value3");
    }

    @Test
    void shouldReplaceMultilineYamlValue() throws ParseException {
        String yaml = """
                gcp:
                  project_id: my-project-12345
                  service_account_key: |
                    {
                      "type": "service_account",
                      "project_id": "my-project-12345",
                      "private_key": "-----BEGIN PRIVATE KEY-----"
                    }
                  other_key: value
                """;

        service.parseContent(yaml);
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("gcp.service_account_key").orElseThrow();

        String oldValue = entry.getValue();
        String newValue = "ENC[encrypted_multiline_value]";

        JTextArea editor = new JTextArea(yaml);
        service.replaceValueInEditor(entry, oldValue, newValue, editor);

        String result = editor.getText();

        // The multiline value should be replaced with the single-line encrypted value
        assertThat(result).contains("service_account_key: ENC[encrypted_multiline_value]");
        assertThat(result).doesNotContain("\"type\": \"service_account\"");
        assertThat(result).doesNotContain("\"private_key\"");
        // Other entries should remain unchanged
        assertThat(result).contains("project_id: my-project-12345");
        assertThat(result).contains("other_key: value");
    }

    @Test
    void shouldReplaceSingleLineValue() throws ParseException {
        String yaml = "host: localhost\nport: 5432";

        service.parseContent(yaml);
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("host").orElseThrow();

        JTextArea editor = new JTextArea(yaml);
        service.replaceValueInEditor(entry, "localhost", "ENC[encrypted]", editor);

        assertThat(editor.getText()).isEqualTo("host: ENC[encrypted]\nport: 5432");
    }

    @Test
    void shouldPreserveEntriesAfterMultilineBlock() throws ParseException {
        String yaml = """
                gcp:
                  project_id: my-project-12345
                  service_account_key: |
                    {
                      "type": "service_account"
                    }

                external_apis:
                  stripe:
                    public_key: pk_test_123
                    secret_key: sk_test_456
                  twilio:
                    account_sid: AC123
                """;

        service.parseContent(yaml);
        ConfigEntry entry = (ConfigEntry) service.getCurrentTree().findByPath("gcp.service_account_key").orElseThrow();

        String oldValue = entry.getValue();
        String newValue = "ENC[encrypted]";

        JTextArea editor = new JTextArea(yaml);
        service.replaceValueInEditor(entry, oldValue, newValue, editor);

        String result = editor.getText();

        // Multiline block should be replaced
        assertThat(result).contains("service_account_key: ENC[encrypted]");
        assertThat(result).doesNotContain("\"type\": \"service_account\"");

        // All entries after the block must be preserved
        assertThat(result).contains("external_apis:");
        assertThat(result).contains("stripe:");
        assertThat(result).contains("public_key: pk_test_123");
        assertThat(result).contains("secret_key: sk_test_456");
        assertThat(result).contains("twilio:");
        assertThat(result).contains("account_sid: AC123");
    }
}
