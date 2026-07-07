package com.dataliquid.passwordsuite.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.domain.ConfigEntry;
import com.dataliquid.passwordsuite.domain.ConfigTree;
import com.dataliquid.passwordsuite.domain.operation.Operation;
import com.dataliquid.passwordsuite.parser.ConfigParser;
import com.dataliquid.passwordsuite.parser.ParseException;
import com.dataliquid.passwordsuite.parser.ParserFactory;

/**
 * Service for managing the configuration editor state, including parsing and
 * undo/redo operations.
 */
public class EditorService {

    private static final Logger logger = LoggerFactory.getLogger(EditorService.class);
    private static final char TAB_CHAR = '\t';
    private static final int TAB_WIDTH = 4;

    /**
     * YAML block scalar indicator at the end of a key line: | or >, optionally
     * followed by chomping (+/-) and/or indentation indicators (e.g., "|-", ">2").
     */
    private static final Pattern BLOCK_SCALAR_INDICATOR = Pattern.compile("[|>][+-]?\\d*$");

    private ConfigTree currentTree;
    private final Deque<Operation> undoStack = new ArrayDeque<>();
    private final Deque<Operation> redoStack = new ArrayDeque<>();

    /**
     * Parses the given content and creates a new ConfigTree with default format.
     * This clears the undo/redo history.
     *
     * @param  content        the configuration content to parse
     *
     * @return                the parsed ConfigTree
     *
     * @throws ParseException if parsing fails
     */
    public ConfigTree parseContent(String content) throws ParseException {
        return parseContent(content, null, null);
    }

    /**
     * Parses the given content and creates a new ConfigTree with custom format.
     * This clears the undo/redo history.
     *
     * @param  content        the configuration content to parse
     * @param  formatPrefix   the prefix for encrypted values (e.g., "ENC[")
     * @param  formatSuffix   the suffix for encrypted values (e.g., "]")
     *
     * @return                the parsed ConfigTree
     *
     * @throws ParseException if parsing fails
     */
    public ConfigTree parseContent(String content, String formatPrefix, String formatSuffix) throws ParseException {
        ConfigParser parser = ParserFactory.getParser(content, formatPrefix, formatSuffix);
        currentTree = parser.parse(content);
        clearUndoRedo();
        return currentTree;
    }

    /**
     * Returns the current ConfigTree.
     */
    public ConfigTree getCurrentTree() {
        return currentTree;
    }

    /**
     * Sets the current ConfigTree.
     */
    public void setCurrentTree(ConfigTree tree) {
        this.currentTree = tree;
    }

    /**
     * Executes an operation and adds it to the undo stack. This clears the redo
     * stack.
     */
    public void executeOperation(Operation operation) {
        operation.execute();
        undoStack.push(operation);
        redoStack.clear();
    }

    /**
     * Undoes the last operation if available.
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            Operation operation = undoStack.pop();
            operation.undo();
            redoStack.push(operation);
        }
    }

    /**
     * Redoes the last undone operation if available.
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            Operation operation = redoStack.pop();
            operation.execute();
            undoStack.push(operation);
        }
    }

    /**
     * Returns true if there are operations to undo.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns true if there are operations to redo.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Returns the number of operations in the undo stack.
     */
    public int getUndoStackSize() {
        return undoStack.size();
    }

    /**
     * Returns the number of operations in the redo stack.
     */
    public int getRedoStackSize() {
        return redoStack.size();
    }

    /**
     * Clears the undo and redo stacks.
     */
    public void clearUndoRedo() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * Replaces a value in the given editor content at the exact line where the
     * ConfigEntry is located. This preserves comments, whitespace, and all other
     * formatting.
     *
     * @param  entry    the ConfigEntry containing lineNumber and key info
     * @param  oldValue the old value to replace
     * @param  newValue the new value to insert
     * @param  content  the current editor content
     *
     * @return          the updated content, or empty if the value could not be
     *                  replaced
     */
    public Optional<String> replaceValueInContent(ConfigEntry entry, String oldValue, String newValue, String content) {
        if (entry.getLineNumber() < 0) {
            if (logger.isWarnEnabled()) {
                logger
                        .warn("Cannot replace value in editor: entry has no line number tracked (key={})",
                                entry.getKey());
            }
            return Optional.empty();
        }

        String[] lines = content.split("\n", -1); // -1 to preserve empty lines

        int lineNum = entry.getLineNumber();
        if (lineNum >= lines.length) {
            if (logger.isWarnEnabled()) {
                logger
                        .warn("Cannot replace value in editor: line number {} out of bounds (key={})", lineNum,
                                entry.getKey());
            }
            return Optional.empty();
        }

        // Multiline YAML values (literal block scalars) are collapsed to a single
        // line containing the new value
        if (oldValue.contains("\n")) {
            // Line numbers for YAML come from a heuristic - never touch a line that
            // does not belong to this key
            if (!lines[lineNum].trim().startsWith(entry.getKey() + ":")) {
                if (logger.isWarnEnabled()) {
                    logger
                            .warn("Cannot replace multiline value: line {} does not start with key '{}'", lineNum,
                                    entry.getKey());
                }
                return Optional.empty();
            }
            return Optional.of(replaceMultilineValue(lines, lineNum, entry.getKey(), newValue));
        }

        String originalLine = lines[lineNum];
        String newLine = replaceValueInLine(originalLine, oldValue, newValue);

        if (newLine.equals(originalLine)) {
            if (logger.isWarnEnabled()) {
                logger.warn("Value not found in line {} for replacement (key={})", lineNum, entry.getKey());
            }
            return Optional.empty();
        }

        lines[lineNum] = newLine;

        if (logger.isInfoEnabled()) {
            logger.info("Replaced value in editor at line {} (key={})", lineNum, entry.getKey());
        }
        return Optional.of(String.join("\n", lines));
    }

    /**
     * Replaces a multiline YAML value (literal block scalar with | or >) with a
     * single-line value. Removes all indented continuation lines while keeping
     * everything after the block.
     *
     * @param  lines    the lines of the document
     * @param  lineNum  the line number of the key
     * @param  key      the key name
     * @param  newValue the new value to insert
     *
     * @return          the new content with the multiline value replaced
     */
    private String replaceMultilineValue(String[] lines, int lineNum, String key, String newValue) {
        String keyLine = lines[lineNum];
        int keyIndent = getIndentation(keyLine);

        // Build new key line: preserve indentation, replace "key: |" (including
        // chomping/indentation indicators like "|-" or ">2") with "key: newValue"
        String trimmedKeyLine = keyLine.trim();
        String newKeyLine;
        if (BLOCK_SCALAR_INDICATOR.matcher(trimmedKeyLine).find()) {
            int colonPos = keyLine.indexOf(':');
            newKeyLine = keyLine.substring(0, colonPos + 1) + " " + newValue;
        } else {
            // Fallback: just use key: newValue with same indentation
            newKeyLine = " ".repeat(keyIndent) + key + ": " + newValue;
        }

        // Collect lines, skipping the multiline block content
        List<String> resultLines = new ArrayList<>();
        boolean blockEnded = false;

        for (int i = 0; i < lines.length; i++) {
            if (i == lineNum) {
                resultLines.add(newKeyLine);
            } else if (i > lineNum && !blockEnded) {
                if (lines[i].isBlank()) {
                    // Empty line - part of the block only if more indented lines follow
                    if (isWithinMultilineBlock(lines, i, keyIndent)) {
                        continue;
                    }
                    blockEnded = true;
                    resultLines.add(lines[i]);
                } else if (getIndentation(lines[i]) > keyIndent) {
                    continue; // Part of the multiline block
                } else {
                    // Line with same or less indentation - block has ended
                    blockEnded = true;
                    resultLines.add(lines[i]);
                }
            } else {
                resultLines.add(lines[i]);
            }
        }

        return String.join("\n", resultLines);
    }

    /**
     * Checks if an empty line at the given index is within a multiline block.
     */
    private boolean isWithinMultilineBlock(String[] lines, int index, int keyIndent) {
        // Look ahead to see if there are more indented lines after this empty line
        for (int i = index + 1; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isBlank()) {
                return getIndentation(line) > keyIndent;
            }
        }
        return false;
    }

    /**
     * Returns the number of leading spaces in a line. Tabs are counted as 4 spaces.
     */
    private int getIndentation(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                count++;
            } else if (c == TAB_CHAR) {
                count += TAB_WIDTH;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Replaces the old value with new value in a single line, using exact string
     * match. Only replaces the first occurrence after any potential key.
     */
    private String replaceValueInLine(String line, String oldValue, String newValue) {
        // Use Pattern.quote to escape special regex characters in the pattern
        String pattern = Pattern.quote(oldValue);
        // Use Matcher.quoteReplacement to escape special characters in the replacement
        // string
        // This prevents $ and \ from being interpreted as backreferences
        String replacement = java.util.regex.Matcher.quoteReplacement(newValue);
        // Replace only the first occurrence
        return line.replaceFirst(pattern, replacement);
    }
}
