package com.dataliquid.passwordsuite.ui;

import javax.swing.JTextArea;

/**
 * Utility for applying text updates to editor components.
 */
public final class EditorTextUtil {

    private EditorTextUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Replaces the editor content while preserving the caret position.
     *
     * @param editor     the editor to update
     * @param newContent the new content
     */
    public static void setTextPreservingCaret(JTextArea editor, String newContent) {
        int caretPos = editor.getCaretPosition();
        editor.setText(newContent);
        editor.setCaretPosition(Math.min(caretPos, newContent.length()));
    }
}
