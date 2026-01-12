package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for drag and drop file support.
 */
@DisplayName("File Drag & Drop Integration Tests")
class FileDropIT extends AbstractSwingIT {

    private FrameFixture window;
    private MainFrame frame;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUpFrame() {
        frame = GuiActionRunner.execute(() -> new MainFrame());
        window = new FrameFixture(robot, frame);
        window.show(new java.awt.Dimension(1400, 900));
    }

    @AfterEach
    void tearDownFrame() {
        if (window != null) {
            window.cleanUp();
        }
    }

    @Test
    @DisplayName("should open single file via drag and drop")
    void shouldOpenSingleFileViaDragAndDrop() throws IOException {
        // Given: A properties file
        File propertiesFile = createTempFile("config.properties", "server.port=8080\nserver.host=localhost");

        // When: Drop the file
        simulateFileDrop(Arrays.asList(propertiesFile));

        // Then: A new tab is created with the file content
        assertThat(frame.getTabbedEditorPanel().getTabCount()).isEqualTo(1); // dropped file creates tab
        FileTab activeTab = frame.getTabbedEditorPanel().getActiveTab();
        assertThat(activeTab).isNotNull();
        assertThat(activeTab.getFile()).isEqualTo(propertiesFile);
        assertThat(activeTab.getText()).contains("server.port=8080");
    }

    @Test
    @DisplayName("should open multiple files via drag and drop")
    void shouldOpenMultipleFilesViaDragAndDrop() throws IOException {
        // Given: Multiple valid files
        File file1 = createTempFile("config.properties", "key1=value1");
        File file2 = createTempFile("settings.yaml", "key2: value2");
        File file3 = createTempFile("data.yml", "key3: value3");

        // When: Drop all files
        simulateFileDrop(Arrays.asList(file1, file2, file3));

        // Then: 3 new tabs are created
        assertThat(frame.getTabbedEditorPanel().getTabCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("should ignore files with invalid extension")
    void shouldIgnoreFilesWithInvalidExtension() throws IOException {
        // Given: One valid and one invalid file
        File validFile = createTempFile("config.properties", "valid=true");
        File invalidFile = createTempFile("readme.txt", "This is a text file");

        // When: Drop both files
        simulateFileDrop(Arrays.asList(validFile, invalidFile));

        // Then: Only the valid file creates a tab
        assertThat(frame.getTabbedEditorPanel().getTabCount()).isEqualTo(1); // only valid file
        FileTab activeTab = frame.getTabbedEditorPanel().getActiveTab();
        assertThat(activeTab.getFile()).isEqualTo(validFile);
    }

    @Test
    @DisplayName("should handle mixed valid and invalid files")
    void shouldHandleMixedValidAndInvalidFiles() throws IOException {
        // Given: A mix of valid and invalid files
        File propFile = createTempFile("test.properties", "prop=value");
        File txtFile = createTempFile("readme.txt", "text content");
        File yamlFile = createTempFile("config.yaml", "yaml: content");
        File pngFile = createTempFile("image.png", "fake png content");

        // When: Drop all files
        simulateFileDrop(Arrays.asList(propFile, txtFile, yamlFile, pngFile));

        // Then: Only 2 valid files create tabs
        assertThat(frame.getTabbedEditorPanel().getTabCount()).isEqualTo(2); // only valid files
    }

    @Test
    @DisplayName("should parse dropped file content automatically")
    void shouldParseDroppedFileContentAutomatically() throws IOException {
        // Given: A properties file with configuration entries
        File propertiesFile = createTempFile("app.properties",
                "database.url=jdbc:mysql://localhost:3306/mydb\ndatabase.user=admin\ndatabase.password=secret");

        // When: Drop the file
        simulateFileDrop(Arrays.asList(propertiesFile));

        // Then: The file is parsed and tree is populated
        // Wait for EDT to process
        GuiActionRunner.execute(() -> {
            FileTab activeTab = frame.getTabbedEditorPanel().getActiveTab();
            // ConfigTree should be set after auto-parse
            assertThat(activeTab.getConfigTree()).isNotNull();
            assertThat(activeTab.getConfigTree().getEntryCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("should accept yaml files via drag and drop")
    void shouldAcceptYamlFilesViaDragAndDrop() throws IOException {
        // Given: A YAML file
        File yamlFile = createTempFile("config.yaml", "server:\n  port: 8080\n  host: localhost");

        // When: Drop the file
        simulateFileDrop(Arrays.asList(yamlFile));

        // Then: Tab is created with YAML content
        assertThat(frame.getTabbedEditorPanel().getTabCount()).isEqualTo(1);
        FileTab activeTab = frame.getTabbedEditorPanel().getActiveTab();
        assertThat(activeTab.getFile().getName()).endsWith(".yaml");
        assertThat(activeTab.getText()).contains("server:");
    }

    @Test
    @DisplayName("should have transfer handler on content pane")
    void shouldHaveTransferHandlerOnContentPane() {
        // The content pane should have a TransferHandler configured
        TransferHandler handler = GuiActionRunner
                .execute(() -> ((JComponent) frame.getContentPane()).getTransferHandler());
        assertThat(handler).isNotNull();
    }

    private File createTempFile(String name, String content) throws IOException {
        File file = new File(tempDir, name);
        Files.writeString(file.toPath(), content);
        return file;
    }

    private void simulateFileDrop(List<File> files) {
        GuiActionRunner.execute(() -> {
            JComponent contentPane = (JComponent) frame.getContentPane();
            TransferHandler handler = contentPane.getTransferHandler();
            Transferable transferable = createFileTransferable(files);
            TransferSupport support = new TransferSupport(contentPane, transferable);
            handler.importData(support);
            return null;
        });
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
