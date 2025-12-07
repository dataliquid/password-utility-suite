package com.dataliquid.passwordsuite.domain.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite operation that groups multiple operations together. Useful for bulk
 * operations and atomic undo/redo.
 */
public class CompositeOperation implements Operation {

    private final String description;
    private final List<Operation> operations = new ArrayList<>();

    public CompositeOperation(String description) {
        this.description = description;
    }

    /**
     * Adds an operation to this composite.
     */
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    /**
     * Returns the number of operations in this composite.
     */
    public int getOperationCount() {
        return operations.size();
    }

    @Override
    public void execute() {
        for (Operation operation : operations) {
            operation.execute();
        }
    }

    @Override
    public void undo() {
        // Undo in reverse order
        List<Operation> reversed = new ArrayList<>(operations);
        Collections.reverse(reversed);
        for (Operation operation : reversed) {
            operation.undo();
        }
    }

    @Override
    public String getDescription() {
        return description + " (" + operations.size() + " operations)";
    }
}
