package com.dataliquid.passwordsuite.domain.operation;

/**
 * Represents an operation that can be executed and undone. Implements the
 * Command pattern for undo/redo functionality.
 */
public interface Operation {

    /**
     * Executes this operation.
     */
    void execute();

    /**
     * Undoes this operation, reverting its effects.
     */
    void undo();

    /**
     * Returns a human-readable description of this operation.
     */
    String getDescription();
}
