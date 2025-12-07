package com.dataliquid.passwordsuite.domain.operation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;

class CompositeOperationTest {

    @Test
    void shouldExecuteAllOperations() {
        ConfigEntry entry1 = new ConfigEntry("key1", "value1");
        ConfigEntry entry2 = new ConfigEntry("key2", "value2");

        CompositeOperation composite = new CompositeOperation("Bulk Edit");
        composite.addOperation(new EditValueOperation(entry1, "newValue1", false));
        composite.addOperation(new EditValueOperation(entry2, "newValue2", false));

        composite.execute();

        assertThat(entry1.getValue()).isEqualTo("newValue1");
        assertThat(entry2.getValue()).isEqualTo("newValue2");
    }

    @Test
    void shouldUndoAllOperationsInReverseOrder() {
        ConfigEntry entry1 = new ConfigEntry("key1", "value1");
        ConfigEntry entry2 = new ConfigEntry("key2", "value2");

        CompositeOperation composite = new CompositeOperation("Bulk Edit");
        composite.addOperation(new EditValueOperation(entry1, "newValue1", false));
        composite.addOperation(new EditValueOperation(entry2, "newValue2", false));

        composite.execute();
        composite.undo();

        assertThat(entry1.getValue()).isEqualTo("value1");
        assertThat(entry2.getValue()).isEqualTo("value2");
    }

    @Test
    void shouldHaveDescription() {
        CompositeOperation composite = new CompositeOperation("Bulk Encrypt");
        composite.addOperation(new EditValueOperation(new ConfigEntry("key1", "value1"), "new1", true));
        composite.addOperation(new EditValueOperation(new ConfigEntry("key2", "value2"), "new2", true));

        assertThat(composite.getDescription()).contains("Bulk Encrypt");
        assertThat(composite.getDescription()).contains("2 operations");
        assertThat(composite.getOperationCount()).isEqualTo(2);
    }
}
