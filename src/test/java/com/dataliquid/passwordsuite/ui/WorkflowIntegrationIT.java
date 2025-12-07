package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import javax.swing.JPopupMenu;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPopupMenuFixture;
import org.assertj.swing.fixture.JTreeFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for complex workflows and edge cases. Tests interactions
 * between multiple components.
 */
class WorkflowIntegrationIT extends AbstractSwingIT {

    private FrameFixture window;
    private MainFrame frame;

    private static final String YAML_WITH_ENC = """
            database:
              host: localhost
              password: ENC[abc123xyz]
            """;

    private static final String YAML_WITH_SEC = """
            database:
              host: localhost
              password: SEC{abc123xyz}
            """;

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
    class LateEnvironmentConfiguration {

        @Test
        void shouldFailGracefullyWhenCryptoOperationWithoutEnvironment() throws InterruptedException {
            // 1. Create file with encrypted content (no environment configured)
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("database:\n  password: ENC[encryptedvalue]");
            Thread.sleep(800);

            // 2. Expand tree and try to decrypt without environment
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. Double-click on encrypted entry - should show error
            tree.doubleClickRow(2);

            // 4. VERIFY: Error dialog appears (no password configured)
            window.optionPane().requireVisible();
            window.optionPane().okButton().click();

            // 5. Content should remain unchanged
            String content = window.textBox("fileEditor").text();
            assertThat(content).contains("ENC[encryptedvalue]");
        }

        @Test
        void shouldReparseTreeAfterEnvironmentCreatedWithMatchingFormat() throws InterruptedException {
            // 1. Create new file with ALREADY encrypted content using ![...] format
            // Note: In YAML, ![...] must be quoted because ! is a YAML tag indicator
            window.menuItemWithPath("File", "New").click();
            String contentWithEncrypted = "database:\n  password: \"![encryptedvalue123]\"";
            window.textBox("fileEditor").enterText(contentWithEncrypted);
            Thread.sleep(800);

            // 2. At this point, tree should show the value but NOT marked as encrypted
            // (no environment configured yet, format unknown)
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. NOW create environment with matching ![...] format
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("BracketEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword1234");
            dialog.textBox("formatPrefixField").deleteText().enterText("![");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // Wait for reparse after environment selection
            Thread.sleep(800);

            // 4. VERIFY: Tree should now recognize ![...] as encrypted
            // Check via the ConfigTree that the entry is marked as encrypted
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(tab.getConfigTree()).isNotNull();

            // The entry should be recognized as encrypted now
            var entries = tab.getConfigTree().getAllEntries();
            assertThat(entries).isNotEmpty();

            // Find the password entry and verify it's marked as encrypted
            // The value in ConfigEntry will be the unquoted value: ![encryptedvalue123]
            boolean foundEncrypted = entries
                    .stream()
                    .anyMatch(entry -> entry.getValue() != null && entry.getValue().contains("![")
                            && entry.isEncrypted());
            assertThat(foundEncrypted)
                    .as("Entry with ![...] should be marked as encrypted after environment setup")
                    .isTrue();
        }

    }

    @Nested
    class EnvironmentSwitchingFormats {

        @Test
        void shouldReparseWhenSwitchingFromEncToSecFormat() throws InterruptedException {
            // 1. Create env1 with ENC[ and ]
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("EncEnv");
            dialog.textBox("masterPasswordField").enterText("encpassword1234");
            dialog.textBox("formatPrefixField").deleteText().enterText("ENC[");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");
            dialog.button("saveButton").click();

            // 2. Create env2 with SEC{ and }
            dialog.textBox("environmentNameField").enterText("SecEnv");
            dialog.textBox("masterPasswordField").enterText("secpassword1234");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 3. Enter content with ENC[...] format
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("database:\n  password: ENC[encrypted123]");
            Thread.sleep(800);

            // 4. With EncEnv selected, ENC values should be recognized as encrypted
            window.comboBox("environmentComboBox").selectItem("EncEnv");
            Thread.sleep(500);

            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            var entries = tab.getConfigTree().getAllEntries();
            boolean encRecognized = entries
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("ENC[") && e.isEncrypted());
            assertThat(encRecognized).as("ENC format should be recognized with EncEnv").isTrue();

            // 5. Switch to SecEnv - ENC values should NOT be recognized
            window.comboBox("environmentComboBox").selectItem("SecEnv");
            Thread.sleep(500);

            entries = tab.getConfigTree().getAllEntries();
            boolean encStillRecognized = entries
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("ENC[") && e.isEncrypted());
            assertThat(encStillRecognized).as("ENC format should NOT be recognized with SecEnv").isFalse();
        }

        @Test
        void shouldRecognizeEncryptedValuesOnlyWithMatchingFormat() throws InterruptedException {
            // 1. Create env with ENC[ format
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("EncOnlyEnv");
            dialog.textBox("masterPasswordField").enterText("encpassword1234");
            dialog.textBox("formatPrefixField").deleteText().enterText("ENC[");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Enter content with ENC[...] - should be recognized
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("database:\n  password: ENC[secret]");
            Thread.sleep(800);

            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            var entries = tab.getConfigTree().getAllEntries();
            boolean encRecognized = entries
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("ENC[") && e.isEncrypted());
            assertThat(encRecognized).as("ENC[...] should be recognized as encrypted").isTrue();

            // 3. Enter content with SEC{...} - should NOT be recognized
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("database:\n  password: SEC{secret}");
            Thread.sleep(800);

            tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            entries = tab.getConfigTree().getAllEntries();
            boolean secRecognized = entries
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("SEC{") && e.isEncrypted());
            assertThat(secRecognized).as("SEC{...} should NOT be recognized with ENC format").isFalse();
        }

        @Test
        void shouldCreateEncryptedValuesWithSelectedFormat() throws InterruptedException {
            // 1. Create env with SEC{ and }
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("SecFormatEnv");
            dialog.textBox("masterPasswordField").enterText("secpassword12345");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Enter plain value and encrypt
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: plainvalue");
            Thread.sleep(800);

            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // Mark and encrypt
            tree.rightClickRow(2);
            JPopupMenu popup = robot.findActivePopupMenu();
            new JPopupMenuFixture(robot, popup).menuItemWithPath("Toggle Encryption Marker").click();

            tree.rightClickRow(2);
            popup = robot.findActivePopupMenu();
            new JPopupMenuFixture(robot, popup).menuItemWithPath("Toggle Encrypt/Decrypt").click();
            Thread.sleep(500);

            // 4. VERIFY: Encrypted value uses SEC{...} format
            String content = window.textBox("fileEditor").text();
            assertThat(content).contains("SEC{");
            assertThat(content).contains("}");
            assertThat(content).doesNotContain("ENC[");
        }
    }

    @Nested
    class EnvironmentDeletionEdgeCases {

        @Test
        void shouldFallBackToNoEnvironmentWhenActiveDeleted() throws InterruptedException {
            // 1. Create environment
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("ToDelete");
            dialog.textBox("masterPasswordField").enterText("deletepassword1");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();

            // Verify environment was created
            assertThat(dialog.comboBox("environmentComboBox").contents()).contains("ToDelete");

            // 2. Select and delete the environment
            dialog.comboBox("environmentComboBox").selectItem("ToDelete");
            dialog.button("deleteButton").click();

            // Confirm deletion
            window.optionPane().yesButton().click();
            Thread.sleep(200);

            // Close dialog
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 3. Verify dropdown shows "No Environment"
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("After deleting the only environment, should show 'No Environment'")
                    .isEqualTo("No Environment");

            // 4. Verify algorithm dropdown is cleared (null/placeholder)
            assertThat(window.comboBox("algorithmComboBox").target().getSelectedItem())
                    .as("Algorithm should be cleared after environment deletion")
                    .isNull();
        }

        @Test
        void shouldKeepFilesOpenWhenActiveEnvironmentDeleted() throws InterruptedException {
            // 1. Create environment
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("FileEnv");
            dialog.textBox("masterPasswordField").enterText("filepassword12");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Create file with content
            window.menuItemWithPath("File", "New").click();
            String testContent = "test:\n  value: keepme";
            window.textBox("fileEditor").enterText(testContent);
            Thread.sleep(500);

            // 3. Delete the environment
            window.menuItemWithPath("Settings", "Environments...").click();
            dialog = window.dialog();
            dialog.comboBox("environmentComboBox").selectItem("FileEnv");
            dialog.button("deleteButton").click();
            window.optionPane().yesButton().click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 4. VERIFY: Tab is still open with content
            int tabCount = window.tabbedPane("editorTabbedPane").target().getTabCount();
            assertThat(tabCount).as("Tab should still be open").isGreaterThanOrEqualTo(1);

            String content = window.textBox("fileEditor").text();
            assertThat(content).as("Content should be preserved").contains("keepme");
        }

        @Test
        void shouldNotAffectCurrentWhenNonActiveEnvironmentDeleted() throws InterruptedException {
            // 1. Create two environments
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("ActiveEnv");
            dialog.textBox("masterPasswordField").enterText("activepassword1");
            dialog.button("saveButton").click();

            dialog.textBox("environmentNameField").enterText("ToDeleteEnv");
            dialog.textBox("masterPasswordField").enterText("deletepassword1");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Select ActiveEnv
            window.comboBox("environmentComboBox").selectItem("ActiveEnv");
            Thread.sleep(200);

            // 3. Delete ToDeleteEnv (non-active)
            window.menuItemWithPath("Settings", "Environments...").click();
            dialog = window.dialog();
            dialog.comboBox("environmentComboBox").selectItem("ToDeleteEnv");
            dialog.button("deleteButton").click();
            window.optionPane().yesButton().click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 4. VERIFY: ActiveEnv is still selected
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("ActiveEnv should still be selected")
                    .isEqualTo("ActiveEnv");
        }
    }

    @Nested
    class EncryptionDecryptionWorkflows {

        @Test
        void shouldEncryptValueWithFullWorkflow() throws InterruptedException {
            // 1. Create environment with password
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("EncryptEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword1234"); // 16 chars for AES
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // 2. Create new file and enter plain YAML content
            window.menuItemWithPath("File", "New").click();
            String plainYaml = "database:\n  password: mysecretvalue";
            window.textBox("fileEditor").enterText(plainYaml);

            // Wait for auto-parse
            Thread.sleep(800);

            // 3. Expand tree and select the password entry
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0); // Expand root
            tree.expandRow(1); // Expand database
            Thread.sleep(200);

            // 4. Right-click on password entry (row 2) and toggle encryption marker
            tree.rightClickRow(2);
            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Toggle Encryption Marker").click();

            // 5. Right-click again and trigger encrypt
            tree.rightClickRow(2);
            popup = robot.findActivePopupMenu();
            popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Toggle Encrypt/Decrypt").click();

            // Wait for encryption to complete
            Thread.sleep(500);

            // 6. VERIFY: Content in editor now contains ENC[...]
            String encryptedContent = window.textBox("fileEditor").text();
            assertThat(encryptedContent).contains("ENC[");
            assertThat(encryptedContent).doesNotContain("mysecretvalue"); // Plain value should be gone
        }

        @Test
        void shouldDecryptValueWithRoundTrip() throws InterruptedException {
            // 1. Create environment
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("DecryptEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword1234");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // 2. Create file with content
            window.menuItemWithPath("File", "New").click();
            String originalValue = "originalplaintext";
            window.textBox("fileEditor").enterText("config:\n  secret: " + originalValue);
            Thread.sleep(800);

            // 3. Encrypt the value
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // Mark and encrypt
            tree.rightClickRow(2);
            JPopupMenu popup = robot.findActivePopupMenu();
            new JPopupMenuFixture(robot, popup).menuItemWithPath("Toggle Encryption Marker").click();

            tree.rightClickRow(2);
            popup = robot.findActivePopupMenu();
            new JPopupMenuFixture(robot, popup).menuItemWithPath("Toggle Encrypt/Decrypt").click();
            Thread.sleep(500);

            // Verify encrypted
            String encryptedContent = window.textBox("fileEditor").text();
            assertThat(encryptedContent).contains("ENC[");

            // 4. Now DECRYPT via double-click
            tree.doubleClickRow(2);
            Thread.sleep(500);

            // 5. VERIFY: Content is decrypted back to original
            String decryptedContent = window.textBox("fileEditor").text();
            assertThat(decryptedContent).contains(originalValue);
            assertThat(decryptedContent).doesNotContain("ENC[");
        }

        @Test
        void shouldShowErrorDialogWhenDecryptionWithWrongPassword() throws InterruptedException {
            // 1. Create first environment and encrypt with it
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("CorrectPwdEnv");
            dialog.textBox("masterPasswordField").enterText("correctpassword1");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // Create file and encrypt
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("data:\n  key: secretdata");
            Thread.sleep(800);

            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            tree.rightClickRow(2);
            JPopupMenu popup = robot.findActivePopupMenu();
            new JPopupMenuFixture(robot, popup).menuItemWithPath("Toggle Encryption Marker").click();

            tree.rightClickRow(2);
            popup = robot.findActivePopupMenu();
            new JPopupMenuFixture(robot, popup).menuItemWithPath("Toggle Encrypt/Decrypt").click();
            Thread.sleep(500);

            // 2. Create second environment with DIFFERENT password
            window.menuItemWithPath("Settings", "Environments...").click();
            dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("WrongPwdEnv");
            dialog.textBox("masterPasswordField").enterText("wrongpassword12!");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // 3. Select the wrong password environment
            window.comboBox("environmentComboBox").selectItem("WrongPwdEnv");
            Thread.sleep(300);

            // 4. Try to decrypt - should fail and show error dialog
            tree.doubleClickRow(2);

            // 5. VERIFY: Error dialog appears with meaningful message
            window.optionPane().requireVisible();
            String dialogMessage = window.optionPane().label("OptionPane.label").text();
            assertThat(dialogMessage).as("Error dialog should show decryption failure").contains("Decryption failed");
            window.optionPane().okButton().click();

            // 6. Value should still be encrypted (decryption failed)
            String contentAfterFailedDecrypt = window.textBox("fileEditor").text();
            assertThat(contentAfterFailedDecrypt).contains("ENC[");
        }
    }

    @Nested
    class FileOperationsWithEnvironments {

        @TempDir
        File tempDir;

        @Test
        void shouldInheritCurrentEnvironmentForNewFile() throws InterruptedException {
            // 1. Create and select environment
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("InheritEnv");
            dialog.textBox("masterPasswordField").enterText("inheritpassword1");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // Verify InheritEnv is selected
            assertThat(window.comboBox("environmentComboBox").selectedItem()).isEqualTo("InheritEnv");

            // 2. Create new file
            window.menuItemWithPath("File", "New").click();
            Thread.sleep(200);

            // 3. VERIFY: New tab has same environment selected
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("New file should inherit current environment")
                    .isEqualTo("InheritEnv");
        }

        @Test
        void shouldParseOpenedFileWithCurrentFormat() throws Exception {
            // 1. Create environment with ENC[ format
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("ParseEnv");
            dialog.textBox("masterPasswordField").enterText("parsepassword123");
            dialog.textBox("formatPrefixField").deleteText().enterText("ENC[");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Create test file with encrypted content
            File testFile = new File(tempDir, "encrypted.yaml");
            Files.writeString(testFile.toPath(), "database:\n  password: ENC[encryptedvalue]");

            // Open file
            GuiActionRunner.execute(() -> {
                try {
                    frame.getFileHandler().openFile(testFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(500);

            // 3. VERIFY: Encrypted value is recognized
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            var entries = tab.getConfigTree().getAllEntries();
            boolean encRecognized = entries
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("ENC[") && e.isEncrypted());
            assertThat(encRecognized).as("ENC[...] should be recognized after file open").isTrue();
        }

    }

    @Nested
    class StatusBarUpdates {

        @Test
        void shouldUpdateStatusBarAfterEnvironmentCreation() {
            // Create environment via dialog
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("StatusBarEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();

            // Verify status bar shows "1 Env"
            String text = window.label("environmentCountLabel").text();
            assertThat(text).isEqualTo("1 Env");
        }

        @Test
        void shouldShowCorrectCountForMultipleEnvironments() {
            // Create 3 environments via dialog
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            for (int i = 1; i <= 3; i++) {
                dialog.textBox("environmentNameField").enterText("Env" + i);
                dialog.textBox("masterPasswordField").enterText("password" + i);
                dialog.button("saveButton").click();
            }

            // VERIFY: All 3 environments appear in ComboBox
            String[] envItems = dialog.comboBox("environmentComboBox").contents();
            assertThat(envItems).contains("Env1", "Env2", "Env3");

            dialog.button("okButton").click();

            // Verify status bar shows "3 Envs"
            String text = window.label("environmentCountLabel").text();
            assertThat(text).isEqualTo("3 Envs");

            // VERIFY: Main window environment ComboBox also has all 3
            String[] mainEnvItems = window.comboBox("environmentComboBox").contents();
            assertThat(mainEnvItems).contains("Env1", "Env2", "Env3");
        }

    }
}
