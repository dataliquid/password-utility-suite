package com.dataliquid.passwordsuite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
