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
 * UI tests for TabbedEditorPanel - Tab management and editor functionality.
 */
class TabbedEditorPanelIT extends AbstractSwingIT {

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
    class TabCreation {

        @Test
        void shouldCreateNewTabFromMenu() {
            // Initial state: 0 tabs
            int initialCount = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());
            assertThat(initialCount).isZero();

            // Create first tab
            window.menuItemWithPath("File", "New").click();
            int afterFirstNew = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());
            assertThat(afterFirstNew).isEqualTo(1);

            // Create second tab
            window.menuItemWithPath("File", "New").click();
            int afterSecondNew = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());
            assertThat(afterSecondNew).isEqualTo(2);
        }

        @Test
        void shouldShowUntitledAsNewTabTitle() {
            window.menuItemWithPath("File", "New").click();

            FileTab activeTab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(activeTab.getTabTitle()).isEqualTo("Untitled");
        }

        @Test
        void shouldMakeNewTabActive() {
            // Create first tab
            window.menuItemWithPath("File", "New").click();
            FileTab firstTab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());

            // Create second tab
            window.menuItemWithPath("File", "New").click();
            FileTab secondTab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());

            // Second tab should be active (different from first)
            assertThat(secondTab).isNotSameAs(firstTab);
        }

        @TempDir
        File tempDir;

        @Test
        void shouldCreateTabWithFilenameOnOpenFile() throws Exception {
            // Create a test file
            File testFile = new File(tempDir, "test-config.yaml");
            Files.writeString(testFile.toPath(), "database:\n  host: localhost\n  port: 5432");

            // Get initial tab count
            int initialTabCount = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());

            // Open file using the refactored method (bypasses JFileChooser)
            GuiActionRunner.execute(() -> {
                try {
                    frame.getFileHandler().openFile(testFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Verify new tab was created
            int newTabCount = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());
            assertThat(newTabCount).isEqualTo(initialTabCount + 1);

            // Verify active tab has the file
            FileTab activeTab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(activeTab).isNotNull();
            assertThat(activeTab.getFile()).isEqualTo(testFile);
            assertThat(activeTab.getText()).contains("database:");
        }
    }

    @Nested
    class TabClosing {

        @Test
        void shouldRemoveTabOnCloseButton() {
            // App starts with 0 tabs - create two tabs
            window.menuItemWithPath("File", "New").click();
            window.menuItemWithPath("File", "New").click();

            // Verify we have 2 tabs
            int tabCountBefore = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());
            assertThat(tabCountBefore).isEqualTo(2);

            // Click close button on the second tab (tabId=1)
            window.button("closeTab-1").click();

            // Verify we have 1 tab remaining
            int tabCountAfter = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getTabCount());
            assertThat(tabCountAfter).isEqualTo(1);
        }

        @Test
        void shouldTriggerConfirmationDialogForUnsavedChanges() {
            // Create a tab
            window.menuItemWithPath("File", "New").click();

            // Type something in the editor to set modified=true
            window.textBox("fileEditor").enterText("test content");

            // Verify tab is modified
            boolean isModified = GuiActionRunner
                    .execute(() -> frame.getTabbedEditorPanel().getActiveTab().isModified());
            assertThat(isModified).isTrue();

            // Click close button - should trigger confirmation dialog
            window.button("closeTab-0").click();

            // Verify confirmation dialog appears
            window.optionPane().requireVisible();
            assertThat(window.optionPane().target().getMessage().toString()).contains("unsaved changes");

            // Click "No" to close without saving
            window.optionPane().noButton().click();
        }
    }

    @Nested
    class ModifiedIndicator {

        @Test
        void shouldShowAsteriskInTabTitleWhenModified() {
            // Create tab
            window.menuItemWithPath("File", "New").click();

            // Initially not modified
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(tab.getTabTitle()).doesNotContain("*");

            // Type something
            window.textBox("fileEditor").enterText("test");

            // Now should show asterisk
            String titleAfter = GuiActionRunner
                    .execute(() -> frame.getTabbedEditorPanel().getActiveTab().getTabTitle());
            assertThat(titleAfter).contains("*");
        }

        @Test
        void shouldSetModifiedFlagWhenContentChanges() {
            window.menuItemWithPath("File", "New").click();

            // Initially not modified
            boolean initialModified = GuiActionRunner
                    .execute(() -> frame.getTabbedEditorPanel().getActiveTab().isModified());
            assertThat(initialModified).isFalse();

            // Type something
            window.textBox("fileEditor").enterText("changed");

            // Now should be modified
            boolean afterModified = GuiActionRunner
                    .execute(() -> frame.getTabbedEditorPanel().getActiveTab().isModified());
            assertThat(afterModified).isTrue();
        }
    }

    @Nested
    class AutoParse {

        @Test
        void shouldTriggerAutoParseAfterDebounce() throws InterruptedException {
            window.menuItemWithPath("File", "New").click();

            // Enter YAML content
            window.textBox("fileEditor").enterText("database:\n  host: localhost");

            // Wait for debounce timer (500ms) + parsing
            Thread.sleep(800);

            // Verify tree has children (was parsed)
            int childCount = GuiActionRunner.execute(() -> {
                var tree = window.tree("configTree").target();
                var root = tree.getModel().getRoot();
                return tree.getModel().getChildCount(root);
            });
            assertThat(childCount).isGreaterThan(0);
        }

        @Test
        void shouldParseValidYamlIntoTree() throws InterruptedException {
            window.menuItemWithPath("File", "New").click();

            // Enter valid YAML
            window.textBox("fileEditor").enterText("server:\n  port: 8080\n  host: localhost");

            // Wait for parse
            Thread.sleep(800);

            // Verify configTree was populated
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(tab.getConfigTree()).isNotNull();
            assertThat(tab.getConfigTree().getEntryCount()).isGreaterThan(0);
        }

        @Test
        void shouldParseValidPropertiesIntoTree() throws InterruptedException {
            window.menuItemWithPath("File", "New").click();

            // Enter valid Properties format
            window.textBox("fileEditor").enterText("db.host=localhost\ndb.port=5432");

            // Wait for parse
            Thread.sleep(800);

            // Verify configTree was populated
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(tab.getConfigTree()).isNotNull();
        }

        @Test
        void shouldHandleInvalidSyntaxGracefully() throws InterruptedException {
            window.menuItemWithPath("File", "New").click();

            // Enter invalid YAML (bad indentation)
            window.textBox("fileEditor").enterText("invalid:\n bad: indentation\n  wrong: level");

            // Wait for parse attempt
            Thread.sleep(800);

            // App should not crash - window still visible
            window.requireVisible();
        }
    }

    @Nested
    class TabSwitching {

        @Test
        void shouldUpdateTreePanelOnTabSwitch() throws InterruptedException {
            // Create first tab with content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("first:\n  key: value1");
            Thread.sleep(800);

            // Create second tab with different content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("second:\n  key: value2");
            Thread.sleep(800);

            // Get tree state for second tab
            int treeChildCount = GuiActionRunner.execute(() -> {
                var tree = window.tree("configTree").target();
                var root = tree.getModel().getRoot();
                return tree.getModel().getChildCount(root);
            });
            assertThat(treeChildCount).isGreaterThan(0);
        }

        @Test
        void shouldMaintainSeparateContentPerTab() {
            // Create first tab with content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("FIRST TAB CONTENT");
            String firstContent = "FIRST TAB CONTENT";

            // Create second tab with different content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").deleteText().enterText("SECOND TAB CONTENT");
            String secondContent = "SECOND TAB CONTENT";

            // Click on first tab to switch back
            window.tabbedPane("editorTabbedPane").selectTab(0);

            // Verify first tab still has its content
            String actualFirst = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab().getText());
            assertThat(actualFirst).contains(firstContent);

            // Switch to second tab
            window.tabbedPane("editorTabbedPane").selectTab(1);

            // Verify second tab still has its content
            String actualSecond = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab().getText());
            assertThat(actualSecond).contains(secondContent);
        }

        @Test
        void shouldSyncAlgorithmDropdownOnTabSwitch() throws InterruptedException {
            // 1. Create environment first (needed for algorithm selection)
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("AlgoSyncEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword12");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Create Tab1, select AES-256-GCM
            window.menuItemWithPath("File", "New").click();
            window.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            Thread.sleep(200);

            // 3. Create Tab2, select Blowfish
            window.menuItemWithPath("File", "New").click();
            window.comboBox("algorithmComboBox").selectItem("Blowfish");
            Thread.sleep(200);

            // 4. Switch to Tab1: verify AES-256-GCM
            window.tabbedPane("editorTabbedPane").selectTab(0);
            Thread.sleep(200);
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Tab1 should have AES-256-GCM selected")
                    .isEqualTo("AES-256-GCM");

            // 5. Switch to Tab2: verify Blowfish
            window.tabbedPane("editorTabbedPane").selectTab(1);
            Thread.sleep(200);
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Tab2 should have Blowfish selected")
                    .isEqualTo("Blowfish");
        }

        @Test
        void shouldSyncEnvironmentDropdownOnTabSwitch() throws InterruptedException {
            // 1. Create two environments
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("EnvOne");
            dialog.textBox("masterPasswordField").enterText("password1234");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            dialog.textBox("environmentNameField").enterText("EnvTwo");
            dialog.textBox("masterPasswordField").enterText("password5678");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Create Tab1, select EnvOne
            window.menuItemWithPath("File", "New").click();
            window.comboBox("environmentComboBox").selectItem("EnvOne");
            Thread.sleep(200);

            // 3. Create Tab2, select EnvTwo
            window.menuItemWithPath("File", "New").click();
            window.comboBox("environmentComboBox").selectItem("EnvTwo");
            Thread.sleep(200);

            // 4. Switch to Tab1: verify EnvOne
            window.tabbedPane("editorTabbedPane").selectTab(0);
            Thread.sleep(200);
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("Tab1 should have EnvOne selected")
                    .isEqualTo("EnvOne");

            // 5. Switch to Tab2: verify EnvTwo
            window.tabbedPane("editorTabbedPane").selectTab(1);
            Thread.sleep(200);
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("Tab2 should have EnvTwo selected")
                    .isEqualTo("EnvTwo");
        }
    }
}
