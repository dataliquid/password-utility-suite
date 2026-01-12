package com.dataliquid.passwordsuite.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.TransferHandler.TransferSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FileDropHandler")
class FileDropHandlerTest {

    private List<File> openedFiles;
    private FileDropHandler handler;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        openedFiles = new ArrayList<>();
        handler = new FileDropHandler(openedFiles::add);
    }

    @Nested
    @DisplayName("canImport")
    class CanImport {

        @Test
        @DisplayName("should return true for javaFileListFlavor")
        void shouldReturnTrueForFileListFlavor() {
            Transferable transferable = createFileTransferable(Arrays.asList(new File("test.properties")));
            TransferSupport support = new TransferSupport(new JPanel(), transferable);

            assertTrue(handler.canImport(support));
        }

        @Test
        @DisplayName("should return false for string flavor")
        void shouldReturnFalseForStringFlavor() {
            Transferable transferable = new StringSelection("just a string");
            TransferSupport support = new TransferSupport(new JPanel(), transferable);

            assertFalse(handler.canImport(support));
        }
    }

    @Nested
    @DisplayName("hasValidExtension")
    class HasValidExtension {

        @Test
        @DisplayName("should accept .properties files")
        void shouldAcceptPropertiesFiles() {
            assertTrue(handler.hasValidExtension(new File("config.properties")));
        }

        @Test
        @DisplayName("should accept .yaml files")
        void shouldAcceptYamlFiles() {
            assertTrue(handler.hasValidExtension(new File("config.yaml")));
        }

        @Test
        @DisplayName("should accept .yml files")
        void shouldAcceptYmlFiles() {
            assertTrue(handler.hasValidExtension(new File("config.yml")));
        }

        @Test
        @DisplayName("should accept uppercase extensions")
        void shouldAcceptUppercaseExtensions() {
            assertTrue(handler.hasValidExtension(new File("CONFIG.PROPERTIES")));
            assertTrue(handler.hasValidExtension(new File("CONFIG.YAML")));
            assertTrue(handler.hasValidExtension(new File("CONFIG.YML")));
        }

        @Test
        @DisplayName("should reject .txt files")
        void shouldRejectTxtFiles() {
            assertFalse(handler.hasValidExtension(new File("readme.txt")));
        }

        @Test
        @DisplayName("should reject files without extension")
        void shouldRejectFilesWithoutExtension() {
            assertFalse(handler.hasValidExtension(new File("Makefile")));
        }

        @Test
        @DisplayName("should reject .json files")
        void shouldRejectJsonFiles() {
            assertFalse(handler.hasValidExtension(new File("config.json")));
        }
    }

    @Nested
    @DisplayName("extractValidFiles")
    class ExtractValidFiles {

        @Test
        @DisplayName("should extract valid files from transferable")
        void shouldExtractValidFiles() throws IOException {
            File validFile = createTempFile("config.properties");
            Transferable transferable = createFileTransferable(Arrays.asList(validFile));

            List<File> result = handler.extractValidFiles(transferable);

            assertEquals(1, result.size());
            assertEquals(validFile, result.get(0));
        }

        @Test
        @DisplayName("should filter out invalid extensions")
        void shouldFilterOutInvalidExtensions() throws IOException {
            File validFile = createTempFile("config.properties");
            File invalidFile = createTempFile("readme.txt");
            Transferable transferable = createFileTransferable(Arrays.asList(validFile, invalidFile));

            List<File> result = handler.extractValidFiles(transferable);

            assertEquals(1, result.size());
            assertEquals(validFile, result.get(0));
        }

        @Test
        @DisplayName("should filter out directories")
        void shouldFilterOutDirectories() throws IOException {
            File validFile = createTempFile("config.properties");
            File directory = new File(tempDir, "subdir");
            directory.mkdir();
            Transferable transferable = createFileTransferable(Arrays.asList(validFile, directory));

            List<File> result = handler.extractValidFiles(transferable);

            assertEquals(1, result.size());
            assertEquals(validFile, result.get(0));
        }

        @Test
        @DisplayName("should return empty list for invalid transferable")
        void shouldReturnEmptyListForInvalidTransferable() {
            Transferable transferable = new StringSelection("not a file list");

            List<File> result = handler.extractValidFiles(transferable);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("importData")
    class ImportData {

        @Test
        @DisplayName("should call callback for valid files")
        void shouldCallCallbackForValidFiles() throws IOException {
            File validFile = createTempFile("config.properties");
            Transferable transferable = createFileTransferable(Arrays.asList(validFile));
            TransferSupport support = new TransferSupport(new JPanel(), transferable);

            boolean result = handler.importData(support);

            assertTrue(result);
            assertEquals(1, openedFiles.size());
            assertEquals(validFile, openedFiles.get(0));
        }

        @Test
        @DisplayName("should call callback for multiple valid files")
        void shouldCallCallbackForMultipleValidFiles() throws IOException {
            File file1 = createTempFile("config.properties");
            File file2 = createTempFile("settings.yaml");
            File file3 = createTempFile("data.yml");
            Transferable transferable = createFileTransferable(Arrays.asList(file1, file2, file3));
            TransferSupport support = new TransferSupport(new JPanel(), transferable);

            boolean result = handler.importData(support);

            assertTrue(result);
            assertEquals(3, openedFiles.size());
        }

        @Test
        @DisplayName("should return false when no valid files")
        void shouldReturnFalseWhenNoValidFiles() throws IOException {
            File invalidFile = createTempFile("readme.txt");
            Transferable transferable = createFileTransferable(Arrays.asList(invalidFile));
            TransferSupport support = new TransferSupport(new JPanel(), transferable);

            boolean result = handler.importData(support);

            assertFalse(result);
            assertTrue(openedFiles.isEmpty());
        }

        @Test
        @DisplayName("should return false for unsupported flavor")
        void shouldReturnFalseForUnsupportedFlavor() {
            Transferable transferable = new StringSelection("not a file");
            TransferSupport support = new TransferSupport(new JPanel(), transferable);

            boolean result = handler.importData(support);

            assertFalse(result);
            assertTrue(openedFiles.isEmpty());
        }
    }

    private File createTempFile(String name) throws IOException {
        File file = new File(tempDir, name);
        file.createNewFile();
        return file;
    }

    private Transferable createFileTransferable(List<File> files) {
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { DataFlavor.javaFileListFlavor };
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return files;
            }
        };
    }
}
