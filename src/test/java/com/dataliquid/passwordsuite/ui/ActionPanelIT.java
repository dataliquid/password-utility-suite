package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTreeFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * UI tests for ActionPanel - Algorithm and Environment dropdowns.
 */
class ActionPanelIT extends AbstractSwingIT {

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
    class AlgorithmDropdown {

        @Test
        void shouldShowAlgorithmDropdown() {
            JComboBoxFixture algorithmCombo = window.comboBox("algorithmComboBox");
            assertThat(algorithmCombo).isNotNull();
            assertThat(algorithmCombo.target().isVisible()).isTrue();
        }

        @Test
        void shouldShowPlaceholderInitially() {
            JComboBoxFixture algorithmCombo = window.comboBox("algorithmComboBox");
            // Initially no algorithm is selected (null or placeholder)
            Object selectedItem = algorithmCombo.target().getSelectedItem();
            assertThat(selectedItem).isNull();
        }

        @Test
        void shouldContainAvailableAlgorithms() {
            JComboBoxFixture algorithmCombo = window.comboBox("algorithmComboBox");
            String[] contents = algorithmCombo.contents();
            assertThat(contents)
                    .contains("AES-128-GCM", "AES-256-CBC", "AES-256-GCM", "Blowfish", "MuleSoft-AES-CBC",
                            "MuleSoft-AES-CBC-RandomIV");
        }

        @Test
        void shouldUpdateCryptoServiceOnAlgorithmSelection() throws InterruptedException {
            // 1. Create environment with password
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("CryptoTestEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword1234");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Select different algorithm
            window.comboBox("algorithmComboBox").selectItem("Blowfish");
            Thread.sleep(100);

            // 3. Verify algorithm is selected in dropdown (UI state reflects CryptoService
            // state)
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Algorithm dropdown should show Blowfish")
                    .isEqualTo("Blowfish");

            // 4. Change to another algorithm
            window.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            Thread.sleep(100);

            // 5. Verify new algorithm is selected
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Algorithm dropdown should show AES-256-GCM")
                    .isEqualTo("AES-256-GCM");

            // 6. Verify algorithm persists after tab switch (proves CryptoService was
            // updated)
            window.menuItemWithPath("File", "New").click();
            window.tabbedPane("editorTabbedPane").selectTab(0);
            Thread.sleep(100);
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Algorithm should persist for tab")
                    .isEqualTo("AES-256-GCM");
        }

        @Test
        void shouldSyncToTabAlgorithmOnTabSwitch() throws InterruptedException {
            // 1. Create environment (needed for algorithm selection)
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("TabAlgoEnv");
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
                    .as("Tab1 should have 'AES-256-GCM' algorithm selected")
                    .isEqualTo("AES-256-GCM");

            // 5. Switch to Tab2: verify Blowfish
            window.tabbedPane("editorTabbedPane").selectTab(1);
            Thread.sleep(200);
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Tab2 should have 'Blowfish' algorithm selected")
                    .isEqualTo("Blowfish");
        }
    }

    @Nested
    class EnvironmentDropdown {

        @Test
        void shouldShowEnvironmentDropdown() {
            JComboBoxFixture envCombo = window.comboBox("environmentComboBox");
            assertThat(envCombo).isNotNull();
            assertThat(envCombo.target().isVisible()).isTrue();
        }

        @Test
        void shouldShowNoEnvironmentInitially() {
            JComboBoxFixture envCombo = window.comboBox("environmentComboBox");
            Object selectedItem = envCombo.target().getSelectedItem();
            assertThat(selectedItem).isEqualTo("No Environment");
        }

        @Test
        void shouldBeEmptyWhenNoEnvironmentsConfigured() {
            JComboBoxFixture envCombo = window.comboBox("environmentComboBox");
            String[] contents = envCombo.contents();
            // Only "No Environment" should be present
            assertThat(contents).containsExactly("No Environment");
        }

        @Test
        void shouldListConfiguredEnvironments() throws InterruptedException {
            // 1. Create multiple environments
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("DevEnv");
            dialog.textBox("masterPasswordField").enterText("devpassword123");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            dialog.textBox("environmentNameField").enterText("ProdEnv");
            dialog.textBox("masterPasswordField").enterText("prodpassword12");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            dialog.textBox("environmentNameField").enterText("TestEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword1");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Verify all environments are in dropdown
            String[] contents = window.comboBox("environmentComboBox").contents();
            assertThat(contents).contains("DevEnv", "ProdEnv", "TestEnv");
        }

        @Test
        void shouldUpdatePasswordOnEnvironmentSelection() throws InterruptedException {
            // 1. Create environment with specific password
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("PasswordEnv");
            dialog.textBox("masterPasswordField").enterText("secretpassword1");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Verify environment is selected (indicates password is configured)
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("Environment should be selected")
                    .isEqualTo("PasswordEnv");

            // 3. Verify we can perform encryption (proves password is configured)
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("test:\n  secret: plainvalue");
            Thread.sleep(800);

            // Mark and encrypt via tree
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.doubleClickRow(2); // Toggle encryption
            Thread.sleep(500);

            // Verify value was encrypted (password was configured correctly)
            String content = window.textBox("fileEditor").text();
            assertThat(content).as("Value should be encrypted (proves password configured)").contains("ENC[");
        }

        @Test
        void shouldUpdateFormatOnEnvironmentSelection() throws InterruptedException {
            // 1. Create environment with custom format SEC{...}
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("CustomFormatEnv");
            dialog.textBox("masterPasswordField").enterText("formatpassword1");
            dialog.comboBox("algorithmComboBox").selectItem("AES-256-GCM");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Verify environment is selected
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("CustomFormatEnv should be selected")
                    .isEqualTo("CustomFormatEnv");

            // 3. Create content with SEC{...} format and verify it's recognized as
            // encrypted
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  password: \"SEC{encryptedvalue}\"");
            Thread.sleep(800);

            // 4. Verify the entry is recognized as encrypted (proves format was applied)
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean isEncrypted = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("SEC{") && e.isEncrypted());
            assertThat(isEncrypted)
                    .as("SEC{...} value should be recognized as encrypted with SEC{} format environment")
                    .isTrue();
        }

        @Test
        void shouldSetDefaultAlgorithmOnEnvironmentSelection() throws InterruptedException {
            // 1. Create environment with specific default algorithm
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("AlgoDefaultEnv");
            dialog.textBox("masterPasswordField").enterText("algopassword12");
            dialog.comboBox("algorithmComboBox").selectItem("Blowfish");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Verify algorithm dropdown shows the default
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Algorithm should be set to environment's default")
                    .isEqualTo("Blowfish");
        }

        @Test
        void shouldReparseOnEnvironmentSelection() throws InterruptedException {
            // 1. Create file with content containing custom format
            window.menuItemWithPath("File", "New").click();
            String contentWithEncrypted = "database:\n  password: \"SEC{encryptedvalue}\"";
            window.textBox("fileEditor").enterText(contentWithEncrypted);
            Thread.sleep(800);

            // 2. At this point, value is NOT recognized as encrypted (no matching env)
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean initiallyEncrypted = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("SEC{") && e.isEncrypted());
            assertThat(initiallyEncrypted).as("Should not be encrypted before environment setup").isFalse();

            // 3. Create environment with matching SEC{...} format
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("SecFormatEnv");
            dialog.textBox("masterPasswordField").enterText("secpassword123");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(800);

            // 4. Verify entry is now recognized as encrypted
            boolean nowEncrypted = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("SEC{") && e.isEncrypted());
            assertThat(nowEncrypted).as("Entry should be encrypted after environment with matching format").isTrue();
        }

        @Test
        void shouldSyncToTabEnvironmentOnTabSwitch() throws InterruptedException {
            // 1. Create two environments
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("EnvAlpha");
            dialog.textBox("masterPasswordField").enterText("alphapassword1");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            dialog.textBox("environmentNameField").enterText("EnvBeta");
            dialog.textBox("masterPasswordField").enterText("betapassword12");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Create Tab1, select EnvAlpha
            window.menuItemWithPath("File", "New").click();
            window.comboBox("environmentComboBox").selectItem("EnvAlpha");
            Thread.sleep(200);

            // 3. Create Tab2, select EnvBeta
            window.menuItemWithPath("File", "New").click();
            window.comboBox("environmentComboBox").selectItem("EnvBeta");
            Thread.sleep(200);

            // 4. Switch to Tab1: verify EnvAlpha
            window.tabbedPane("editorTabbedPane").selectTab(0);
            Thread.sleep(200);
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("Tab1 should have 'EnvAlpha' selected")
                    .isEqualTo("EnvAlpha");

            // 5. Switch to Tab2: verify EnvBeta
            window.tabbedPane("editorTabbedPane").selectTab(1);
            Thread.sleep(200);
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("Tab2 should have 'EnvBeta' selected")
                    .isEqualTo("EnvBeta");
        }
    }

    @Nested
    class DefaultValueBehavior {

        @Test
        void shouldShowDefaultsBeforeEnvironmentSetup() {
            // Verify algorithm shows null (placeholder)
            JComboBoxFixture algorithmCombo = window.comboBox("algorithmComboBox");
            assertThat(algorithmCombo.target().getSelectedItem()).isNull();

            // Verify environment shows "No Environment"
            JComboBoxFixture envCombo = window.comboBox("environmentComboBox");
            assertThat(envCombo.target().getSelectedItem()).isEqualTo("No Environment");
        }

        @Test
        void shouldAutoSelectFirstCreatedEnvironment() throws InterruptedException {
            // 1. Verify initially "No Environment"
            assertThat(window.comboBox("environmentComboBox").selectedItem()).isEqualTo("No Environment");

            // 2. Create first environment
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("FirstCreatedEnv");
            dialog.textBox("masterPasswordField").enterText("firstpassword1");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 3. Verify first environment is auto-selected
            assertThat(window.comboBox("environmentComboBox").selectedItem())
                    .as("First created environment should be auto-selected")
                    .isEqualTo("FirstCreatedEnv");
        }

        @Test
        void shouldSetDefaultAlgorithmWhenEnvironmentSelected() throws InterruptedException {
            // 1. Verify algorithm is null initially
            assertThat(window.comboBox("algorithmComboBox").target().getSelectedItem()).isNull();

            // 2. Create environment with AES-128-GCM as default
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("DefaultAlgoEnv");
            dialog.textBox("masterPasswordField").enterText("defaultalgo123");
            dialog.comboBox("algorithmComboBox").selectItem("AES-128-GCM");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 3. Verify algorithm dropdown now shows the default
            assertThat(window.comboBox("algorithmComboBox").selectedItem())
                    .as("Algorithm should be set to environment's default AES-128-GCM")
                    .isEqualTo("AES-128-GCM");
        }
    }

    @Nested
    class FormatChangeEdgeCases {

        @Test
        void shouldReparseWhenBracketsChange() throws InterruptedException {
            // 1. Create environment with ENC[...] format
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("EncBracketEnv");
            dialog.textBox("masterPasswordField").enterText("encbracket1234");
            dialog.textBox("formatPrefixField").deleteText().enterText("ENC[");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            // 2. Create environment with SEC{...} format
            dialog.textBox("environmentNameField").enterText("SecBracketEnv");
            dialog.textBox("masterPasswordField").enterText("secbracket1234");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 3. Create file with content containing SEC{...}
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: \"SEC{encrypteddata}\"");
            Thread.sleep(800);

            // 4. Select EncBracketEnv (ENC[...]) - SEC{...} should NOT be recognized
            window.comboBox("environmentComboBox").selectItem("EncBracketEnv");
            Thread.sleep(500);

            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean encryptedWithEnc = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("SEC{") && e.isEncrypted());
            assertThat(encryptedWithEnc).as("SEC{...} should NOT be recognized with ENC[...] format").isFalse();

            // 5. Switch to SecBracketEnv - SEC{...} should NOW be recognized
            window.comboBox("environmentComboBox").selectItem("SecBracketEnv");
            Thread.sleep(500);

            boolean encryptedWithSec = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(e -> e.getValue() != null && e.getValue().contains("SEC{") && e.isEncrypted());
            assertThat(encryptedWithSec).as("SEC{...} SHOULD be recognized with SEC{...} format").isTrue();
        }

        @Test
        void shouldRecognizeEncryptedValuesAfterEnvironmentSwitch() throws InterruptedException {
            // 1. Create file with custom format content FIRST (no environment yet)
            // Use non-default format !{...} that won't be recognized without environment
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("database:\n  password: \"!{someciphertext123}\"");
            Thread.sleep(800);

            // 2. Verify NOT recognized as encrypted yet (no matching environment)
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean initiallyEncrypted = tab.getConfigTree().getAllEntries().stream().anyMatch(e -> e.isEncrypted());
            assertThat(initiallyEncrypted)
                    .as("Should not be recognized as encrypted before environment setup")
                    .isFalse();

            // 3. Create environment with matching !{...} format
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("CustomFormatEnv");
            dialog.textBox("masterPasswordField").enterText("customformat12");
            dialog.textBox("formatPrefixField").deleteText().enterText("!{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(800);

            // 4. Verify NOW recognized as encrypted
            boolean nowEncrypted = tab.getConfigTree().getAllEntries().stream().anyMatch(e -> e.isEncrypted());
            assertThat(nowEncrypted)
                    .as("Should be recognized as encrypted after environment with matching format")
                    .isTrue();
        }

        @Test
        void shouldRecognizeOnlyActiveFormatInMixedContent() throws InterruptedException {
            // 1. Create two environments with different formats
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();

            dialog.textBox("environmentNameField").enterText("EncEnv");
            dialog.textBox("masterPasswordField").enterText("encenvpassword");
            dialog.textBox("formatPrefixField").deleteText().enterText("ENC[");
            dialog.textBox("formatSuffixField").deleteText().enterText("]");
            dialog.button("saveButton").click();
            Thread.sleep(100);

            dialog.textBox("environmentNameField").enterText("SecEnv");
            dialog.textBox("masterPasswordField").enterText("secenvpassword");
            dialog.textBox("formatPrefixField").deleteText().enterText("SEC{");
            dialog.textBox("formatSuffixField").deleteText().enterText("}");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Create file with BOTH formats
            window.menuItemWithPath("File", "New").click();
            String mixedContent = "database:\n  enc_password: ENC[cipher1]\n  sec_password: \"SEC{cipher2}\"";
            window.textBox("fileEditor").enterText(mixedContent);
            Thread.sleep(800);

            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());

            // 3. With EncEnv selected - only ENC[...] should be recognized
            window.comboBox("environmentComboBox").selectItem("EncEnv");
            Thread.sleep(500);

            long encEncryptedCount = tab.getConfigTree().getAllEntries().stream().filter(e -> e.isEncrypted()).count();
            assertThat(encEncryptedCount).as("Only ENC[...] format should be recognized with EncEnv").isEqualTo(1);

            // 4. Switch to SecEnv - only SEC{...} should be recognized
            window.comboBox("environmentComboBox").selectItem("SecEnv");
            Thread.sleep(500);

            long secEncryptedCount = tab.getConfigTree().getAllEntries().stream().filter(e -> e.isEncrypted()).count();
            assertThat(secEncryptedCount).as("Only SEC{...} format should be recognized with SecEnv").isEqualTo(1);
        }
    }
}
