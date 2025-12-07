package com.dataliquid.passwordsuite.domain.operation;

import com.dataliquid.passwordsuite.domain.ConfigEntry;

/**
 * Operation to edit the value of a ConfigEntry. Supports undo by storing the
 * previous value.
 */
public class EditValueOperation implements Operation {

    private final ConfigEntry entry;
    private final String oldValue;
    private final String newValue;
    private final boolean oldEncrypted;
    private final boolean newEncrypted;

    public EditValueOperation(ConfigEntry entry, String newValue, boolean newEncrypted) {
        this.entry = entry;
        this.oldValue = entry.getValue();
        this.oldEncrypted = entry.isEncrypted();
        this.newValue = newValue;
        this.newEncrypted = newEncrypted;
    }

    @Override
    public void execute() {
        entry.setValue(newValue);
        entry.setEncrypted(newEncrypted);
    }

    @Override
    public void undo() {
        entry.setValue(oldValue);
        entry.setEncrypted(oldEncrypted);
    }

    @Override
    public String getDescription() {
        return "Edit value of '" + entry.getKey() + "'";
    }
}
