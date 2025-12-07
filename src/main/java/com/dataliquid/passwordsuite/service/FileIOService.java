package com.dataliquid.passwordsuite.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Service for file I/O operations (loading and saving configuration files).
 */
public class FileIOService {

    /**
     * Loads content from a file.
     *
     * @param  file        the file to load
     *
     * @return             the file content as a string
     *
     * @throws IOException if the file cannot be read
     */
    public String loadFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IOException("Path is not a file: " + file.getAbsolutePath());
        }

        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * Saves content to a file.
     *
     * @param  file        the file to save to
     * @param  content     the content to write
     *
     * @throws IOException if the file cannot be written
     */
    public void saveFile(File file, String content) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        // Create parent directories if they don't exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories: " + parentDir.getAbsolutePath());
            }
        }

        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }
}
