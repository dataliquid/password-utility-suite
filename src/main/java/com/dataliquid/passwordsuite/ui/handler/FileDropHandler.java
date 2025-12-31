package com.dataliquid.passwordsuite.ui.handler;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.TransferHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles drag and drop of files into the application.
 * <p>
 * Accepts files with extensions: .properties, .yaml, .yml
 * </p>
 */
public class FileDropHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FileDropHandler.class);

    private static final Set<String> VALID_EXTENSIONS = Set.of("properties", "yaml", "yml");

    private final transient Consumer<File> fileOpenCallback;

    /**
     * Creates a new FileDropHandler.
     *
     * @param fileOpenCallback callback invoked for each valid dropped file
     */
    public FileDropHandler(Consumer<File> fileOpenCallback) {
        this.fileOpenCallback = fileOpenCallback;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        Transferable transferable = support.getTransferable();
        List<File> validFiles = extractValidFiles(transferable);

        if (validFiles.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No valid files in drop");
            }
            return false;
        }

        for (File file : validFiles) {
            try {
                fileOpenCallback.accept(file);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to open dropped file: {}", file.getName(), e);
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Dropped {} file(s)", validFiles.size());
        }
        return true;
    }

    /**
     * Extracts valid files from the transferable.
     * <p>
     * Filters for files with valid extensions (.properties, .yaml, .yml).
     * </p>
     *
     * @param  transferable the transferable containing dropped data
     *
     * @return              list of valid files, empty if none found
     */
    @SuppressWarnings("unchecked")
    List<File> extractValidFiles(Transferable transferable) {
        List<File> validFiles = new ArrayList<>();

        try {
            List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

            for (File file : droppedFiles) {
                if (file.isFile() && hasValidExtension(file)) {
                    validFiles.add(file);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Skipping invalid file: {}", file.getName());
                }
            }
        } catch (UnsupportedFlavorException | IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to extract files from drop", e);
            }
        }

        return validFiles;
    }

    /**
     * Checks if the file has a valid extension.
     *
     * @param  file the file to check
     *
     * @return      true if the file has a valid extension
     */
    boolean hasValidExtension(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return false;
        }
        String extension = name.substring(lastDot + 1);
        return VALID_EXTENSIONS.contains(extension);
    }
}
