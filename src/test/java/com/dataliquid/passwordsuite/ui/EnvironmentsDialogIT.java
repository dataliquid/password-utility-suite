package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * UI tests for EnvironmentsDialog - Environment configuration management.
 */
class EnvironmentsDialogIT extends AbstractSwingIT {

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

    private DialogFixture openEnvironmentsDialog() {
        window.menuItemWithPath("Settings", "Environments...").click();
        return window.dialog();
    }

    @Nested
    class DialogDisplay {

        @Test
        void shouldOpenDialogFromMenu() {
            DialogFixture dialog = openEnvironmentsDialog();
            dialog.requireVisible();
            assertThat(dialog.target().getTitle()).isEqualTo("Environments");
        }

        @Test
        void shouldOpenDialogWithCtrlEShortcut() throws InterruptedException {
            // Ensure frame has focus and is ready
            window.focus();
            window.robot().waitForIdle();
            Thread.sleep(200);

            // Press Ctrl+E using robot directly for better reliability
            robot.pressKey(java.awt.event.KeyEvent.VK_CONTROL);
            robot.pressKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_CONTROL);
            robot.waitForIdle();

            // Verify dialog opens
            DialogFixture dialog = window.dialog();
            dialog.requireVisible();
            assertThat(dialog.target().getTitle()).isEqualTo("Environments");
            dialog.button("cancelButton").click();
        }

        @Test
        void shouldBeModalDialog() {
            DialogFixture dialog = openEnvironmentsDialog();
            assertThat(dialog.target().isModal()).isTrue();
            dialog.close();
        }

        @Test
        void shouldHaveOkAndCancelButtons() {
            DialogFixture dialog = openEnvironmentsDialog();
            dialog.button(org.assertj.swing.core.matcher.JButtonMatcher.withText("OK")).requireVisible();
            dialog.button(org.assertj.swing.core.matcher.JButtonMatcher.withText("Cancel")).requireVisible();
            dialog.close();
        }

        @Test
        void shouldHaveSaveButton() {
            DialogFixture dialog = openEnvironmentsDialog();
            dialog.button(org.assertj.swing.core.matcher.JButtonMatcher.withText("Save")).requireVisible();
            dialog.close();
        }

        @Test
        void shouldCloseDialogOnEsc() {
            DialogFixture dialog = openEnvironmentsDialog();
            dialog.pressAndReleaseKeys(java.awt.event.KeyEvent.VK_ESCAPE);
            // Dialog should be closed
        }
    }

    @Nested
    class EnvironmentCreation {

        @Test
        void shouldCreateNewEnvironmentOnSave() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Enter name and password
            dialog.textBox("environmentNameField").enterText("TestEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");

            // Click Save
            dialog.button("saveButton").click();

            // Verify environment in dropdown
            String[] items = dialog.comboBox("environmentComboBox").contents();
            assertThat(items).contains("TestEnv");

            dialog.button("okButton").click();
        }

        @Test
        void shouldRequireEnvironmentName() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Try to save without name (leave name empty, but enter password)
            dialog.textBox("masterPasswordField").enterText("password");
            dialog.button("saveButton").click();

            // Verify error dialog appears
            window.optionPane().requireVisible();
            window.optionPane().okButton().click();

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldSaveCustomPrefixSuffix() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Enter name, password, and custom prefix/suffix
            dialog.textBox("environmentNameField").enterText("CustomEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC[");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");

            // Click Save
            dialog.button("saveButton").click();

            // Verify environment was created
            String[] items = dialog.comboBox("environmentComboBox").contents();
            assertThat(items).contains("CustomEnv");

            dialog.button("okButton").click();
        }

        @Test
        void shouldSaveDefaultAlgorithm() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Enter name and select algorithm
            dialog.textBox("environmentNameField").enterText("AlgoEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");

            // Click Save
            dialog.button("saveButton").click();

            // Verify environment was created with algorithm
            String[] items = dialog.comboBox("environmentComboBox").contents();
            assertThat(items).contains("AlgoEnv");

            dialog.button("okButton").click();
        }
    }

    @Nested
    class EnvironmentEditing {

        @Test
        void shouldLoadSelectedEnvironmentOnEdit() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Create environment first
            dialog.textBox("environmentNameField").enterText("EditTestEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.button("saveButton").click();

            // Select the environment
            dialog.comboBox("environmentComboBox").selectItem("EditTestEnv");

            // Click Edit
            dialog.button("editButton").click();

            // Verify fields are populated
            assertThat(dialog.textBox("environmentNameField").text()).isEqualTo("EditTestEnv");

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldUpdateExistingEnvironmentOnSave() throws InterruptedException {
            DialogFixture dialog = openEnvironmentsDialog();

            // 1. Create initial environment with AES-256-GCM
            dialog.textBox("environmentNameField").enterText("UpdateTestEnv");
            dialog.textBox("masterPasswordField").enterText("initialpassword1");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            // 2. Verify environment was created
            String[] items = dialog.comboBox("environmentComboBox").contents();
            assertThat(items).contains("UpdateTestEnv");

            // 3. Select and edit the environment
            dialog.comboBox("environmentComboBox").selectItem("UpdateTestEnv");
            dialog.button("editButton").click();
            Thread.sleep(100);

            // 4. Change the algorithm to Blowfish
            dialog.comboBox("algorithmComboBox").selectItem("Blowfish");

            // 5. Save the changes
            dialog.button("saveButton").click();
            Thread.sleep(100);

            // 6. Clear selection and re-select to verify changes persisted
            dialog.comboBox("environmentComboBox").selectItem("UpdateTestEnv");
            dialog.button("editButton").click();
            Thread.sleep(100);

            // 7. Verify algorithm was updated
            assertThat(dialog.comboBox("algorithmComboBox").selectedItem()).isEqualTo("Blowfish");

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldDisableEditButtonWhenNoSelection() {
            DialogFixture dialog = openEnvironmentsDialog();

            // With no environments, Edit button should be disabled
            assertThat(dialog.button("editButton").isEnabled()).isFalse();

            dialog.button("cancelButton").click();
        }
    }

    @Nested
    class EnvironmentDeletion {

        @Test
        void shouldRemoveEnvironmentOnDelete() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Create environment first
            dialog.textBox("environmentNameField").enterText("DeleteTestEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.button("saveButton").click();

            // Select the environment
            dialog.comboBox("environmentComboBox").selectItem("DeleteTestEnv");

            // Click Delete
            dialog.button("deleteButton").click();

            // Confirm deletion
            window.optionPane().yesButton().click();

            // Verify environment is removed from list
            String[] items = dialog.comboBox("environmentComboBox").contents();
            assertThat(items).doesNotContain("DeleteTestEnv");

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldShowConfirmationDialogOnDelete() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Create environment first
            dialog.textBox("environmentNameField").enterText("ConfirmDeleteEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.button("saveButton").click();

            // Select the environment
            dialog.comboBox("environmentComboBox").selectItem("ConfirmDeleteEnv");

            // Click Delete
            dialog.button("deleteButton").click();

            // Verify confirmation dialog appears
            window.optionPane().requireVisible();
            window.optionPane().noButton().click(); // Cancel deletion

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldDisableDeleteButtonWhenNoSelection() {
            DialogFixture dialog = openEnvironmentsDialog();

            // With no environments, Delete button should be disabled
            assertThat(dialog.button("deleteButton").isEnabled()).isFalse();

            dialog.button("cancelButton").click();
        }
    }

    @Nested
    class Validation {

        @Test
        void shouldShowErrorForEmptyName() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Try to save without name
            dialog.button("saveButton").click();

            // Verify error dialog appears
            window.optionPane().requireVisible();
            window.optionPane().okButton().click();

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldValidatePasswordLengthForMuleSoft() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Enter name and invalid password for MuleSoft
            dialog.textBox("environmentNameField").enterText("MuleSoftEnv");
            dialog.textBox("masterPasswordField").enterText("shortpwd"); // Not 16 or 32 chars
            dialog.comboBox("algorithmComboBox").selectItem("MuleSoft-AES-CBC");

            // Click Save
            dialog.button("saveButton").click();

            // Verify error dialog appears
            window.optionPane().requireVisible();
            window.optionPane().okButton().click();

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldUpdateFormatFieldsOnAlgorithmChange() throws InterruptedException {
            DialogFixture dialog = openEnvironmentsDialog();

            // 1. Verify initial format is ENC[...]
            assertThat(dialog.textBox("formatPrefixField").text()).isEqualTo("ENC[");
            assertThat(dialog.textBox("formatSuffixField").text()).isEqualTo("]");

            // 2. Select MuleSoft algorithm which uses ![...] format
            dialog.comboBox("algorithmComboBox").selectItem("MuleSoft-AES-CBC-RandomIV");
            Thread.sleep(100);

            // 3. Verify format fields updated to MuleSoft default
            assertThat(dialog.textBox("formatPrefixField").text())
                    .as("MuleSoft algorithm should set format prefix to '!['")
                    .isEqualTo("![");
            assertThat(dialog.textBox("formatSuffixField").text())
                    .as("MuleSoft algorithm should keep format suffix as ']'")
                    .isEqualTo("]");

            // 4. Switch back to AES-256-GCM
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            Thread.sleep(100);

            // 5. Verify format fields reset to standard ENC[...]
            assertThat(dialog.textBox("formatPrefixField").text())
                    .as("AES-256-GCM should reset format prefix to 'ENC['")
                    .isEqualTo("ENC[");

            dialog.button("cancelButton").click();
        }
    }

    @Nested
    class KeePassIntegration {

        @Test
        void shouldTriggerSaveOnEnterInPasswordField() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Enter name and password
            dialog.textBox("environmentNameField").enterText("EnterKeyEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");

            // Press Enter in password field
            dialog.textBox("masterPasswordField").pressAndReleaseKeys(java.awt.event.KeyEvent.VK_ENTER);

            // Verify environment was saved
            String[] items = dialog.comboBox("environmentComboBox").contents();
            assertThat(items).contains("EnterKeyEnv");

            dialog.button("cancelButton").click();
        }

        @Test
        void shouldClearFieldsAfterSave() {
            DialogFixture dialog = openEnvironmentsDialog();

            // Enter and save environment
            dialog.textBox("environmentNameField").enterText("ClearFieldsEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword");
            dialog.button("saveButton").click();

            // Verify fields are cleared
            assertThat(dialog.textBox("environmentNameField").text()).isEmpty();

            dialog.button("cancelButton").click();
        }
    }

    @Nested
    class DialogClosure {

        @Test
        void shouldCloseAndApplyChangesOnOk() {
            DialogFixture dialog = openEnvironmentsDialog();
            dialog.button(org.assertj.swing.core.matcher.JButtonMatcher.withText("OK")).click();
            // Dialog should be closed
        }

        @Test
        void shouldCloseWithoutChangesOnCancel() {
            DialogFixture dialog = openEnvironmentsDialog();
            dialog.button(org.assertj.swing.core.matcher.JButtonMatcher.withText("Cancel")).click();
            // Dialog should be closed
        }
    }
}
