package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * UI tests for StatusBar - File name and environment count display.
 */
class StatusBarIT extends AbstractSwingIT {

    private FrameFixture window;
    private MainFrame frame;

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

    @Nested
    class FileNameDisplay {

        @Test
        void shouldShowStatusBar() {
            assertThat(window.panel("statusBar").target()).isNotNull();
            assertThat(window.panel("statusBar").target().isVisible()).isTrue();
        }

        @Test
        void shouldShowNoFileInitially() {
            String text = window.label("fileNameLabel").text();
            assertThat(text).isEqualTo("No file");
        }

        @Test
        void shouldMaintainNoFileForMultipleUnsavedTabs() {
            // Create multiple new tabs - all unsaved, so status bar shows "No file"
            window.menuItemWithPath("File", "New").click();
            assertThat(window.label("fileNameLabel").text()).isEqualTo("No file");

            window.menuItemWithPath("File", "New").click();
            assertThat(window.label("fileNameLabel").text()).isEqualTo("No file");

            // Switch back to first tab
            window.tabbedPane("editorTabbedPane").selectTab(0);
            assertThat(window.label("fileNameLabel").text()).isEqualTo("No file");
        }

        @TempDir
        File tempDir;

        @Test
        void shouldShowFilenameAfterFileOpened() throws Exception {
            // Create a test file
            File testFile = new File(tempDir, "test-config.yaml");
            Files.writeString(testFile.toPath(), "database:\n  host: localhost");

            // Open file using the FileHandler (bypasses JFileChooser)
            GuiActionRunner.execute(() -> {
                try {
                    frame.getFileHandler().openFile(testFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Verify status bar shows filename
            String text = window.label("fileNameLabel").text();
            assertThat(text).isEqualTo("test-config.yaml");
        }

        @Test
        void shouldUpdateOnTabSwitch() throws Exception {
            // Create first tab with saved file
            File file1 = new File(tempDir, "file1.yaml");
            Files.writeString(file1.toPath(), "first: value");
            GuiActionRunner.execute(() -> {
                try {
                    frame.getFileHandler().openFile(file1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Create second tab (unsaved)
            window.menuItemWithPath("File", "New").click();

            // Verify status bar shows "No file" for unsaved tab
            assertThat(window.label("fileNameLabel").text()).isEqualTo("No file");

            // Switch to first tab
            window.tabbedPane("editorTabbedPane").selectTab(0);

            // Verify status bar shows filename
            assertThat(window.label("fileNameLabel").text()).isEqualTo("file1.yaml");
        }
    }

    @Nested
    class EnvironmentCountDisplay {

        @Test
        void shouldShowNoEnvironmentsInitially() {
            String text = window.label("environmentCountLabel").text();
            assertThat(text).isEqualTo("No environments");
        }

        @Test
        void shouldShowOneEnvForSingleEnvironment() {
            // Create one environment
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("SingleEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // Verify status bar shows "1 Env"
            String text = window.label("environmentCountLabel").text();
            assertThat(text).isEqualTo("1 Env");
        }

        @Test
        void shouldShowNEnvsForMultipleEnvironments() {
            // Create two environments
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("Env1");
            dialog.textBox("masterPasswordField").enterText("password1");
            dialog.button("saveButton").click();

            dialog.textBox("environmentNameField").enterText("Env2");
            dialog.textBox("masterPasswordField").enterText("password2");
            dialog.button("saveButton").click();

            dialog.button("okButton").click();

            // Verify status bar shows "2 Envs"
            String text = window.label("environmentCountLabel").text();
            assertThat(text).isEqualTo("2 Envs");
        }

        @Test
        void shouldUpdateAfterEnvironmentDeletion() throws InterruptedException {
            // Create two environments first
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("ToKeep");
            dialog.textBox("masterPasswordField").enterText("keeppassword");
            dialog.button("saveButton").click();

            dialog.textBox("environmentNameField").enterText("ToDelete");
            dialog.textBox("masterPasswordField").enterText("deletepassword");
            dialog.button("saveButton").click();

            dialog.button("okButton").click();
            Thread.sleep(100);

            // Verify "2 Envs"
            assertThat(window.label("environmentCountLabel").text()).isEqualTo("2 Envs");

            // Delete one environment
            window.menuItemWithPath("Settings", "Environments...").click();
            dialog = window.dialog();
            dialog.comboBox("environmentComboBox").selectItem("ToDelete");
            dialog.button("deleteButton").click();
            window.optionPane().yesButton().click();
            dialog.button("okButton").click();
            Thread.sleep(100);

            // Verify status bar now shows "1 Env"
            String text = window.label("environmentCountLabel").text();
            assertThat(text).isEqualTo("1 Env");
        }
    }
}
