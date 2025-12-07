package com.dataliquid.passwordsuite.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Panel containing a text editor for configuration content input.
 */
public final class EditorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextArea textArea;
    private final JScrollPane scrollPane;

    public EditorPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Configuration Editor"));

        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setTabSize(2);
        textArea.setLineWrap(false);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Returns the text content of the editor.
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Sets the text content of the editor.
     */
    public void setText(String text) {
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }

    /**
     * Clears the editor content.
     */
    public void clear() {
        textArea.setText("");
    }

    /**
     * Returns true if the editor is empty.
     */
    public boolean isEmpty() {
        return textArea.getText().isBlank();
    }
}
