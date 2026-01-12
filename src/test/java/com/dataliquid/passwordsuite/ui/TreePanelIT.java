package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

import javax.swing.JPopupMenu;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPopupMenuFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.fixture.JTreeFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.dataliquid.passwordsuite.domain.ConfigEntry;

/**
 * UI tests for TreePanel - Configuration tree visualization and interactions.
 */
class TreePanelIT extends AbstractSwingIT {

    private FrameFixture window;
    private MainFrame frame;

    private static final String SAMPLE_YAML = """
            database:
              host: localhost
              port: 5432
              password: secret123
            """;

    private static final String SAMPLE_PROPERTIES = """
            database.host=localhost
            database.port=5432
            database.password=secret123
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
    class TreeDisplay {

        @Test
        void shouldShowConfigurationAsRootNode() {
            JTreeFixture tree = window.tree();
            assertThat(tree.target().getModel().getRoot().toString()).isEqualTo("Configuration");
        }

        @Test
        void shouldBeEmptyOnStartup() {
            JTreeFixture tree = window.tree();
            // Root should have no children initially
            assertThat(tree.target().getModel().getChildCount(tree.target().getModel().getRoot())).isZero();
        }

        @Test
        void shouldCreateHierarchicalTreeFromYamlContent() throws InterruptedException {
            // Create tab and enter YAML
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);

            // Wait for auto-parse
            Thread.sleep(800);

            // Verify tree has children
            JTreeFixture tree = window.tree("configTree");
            int childCount = tree.target().getModel().getChildCount(tree.target().getModel().getRoot());
            assertThat(childCount).isGreaterThan(0);

            // Verify hierarchical structure - root should have "database" group
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(tab.getConfigTree()).isNotNull();
            assertThat(tab.getConfigTree().getEntryCount()).isEqualTo(3); // host, port, password
        }

        @Test
        void shouldCreateTreeFromPropertiesContent() throws InterruptedException {
            // Create tab and enter Properties
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_PROPERTIES);

            // Wait for auto-parse
            Thread.sleep(800);

            // Verify tree has content
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            assertThat(tab.getConfigTree()).isNotNull();
        }

        @Test
        void shouldShowEncSuffixForEncryptedEntries() throws InterruptedException {
            // 1. Environment erstellen
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("EncSuffixEnv");
            dialog.textBox("masterPasswordField").enterText("testpassword12");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. YAML mit verschluesseltem Wert erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: ENC[encrypted123]");
            Thread.sleep(800);

            // 3. Tree expandieren und Entry pruefen
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 4. Pruefen ob Entry als encrypted erkannt wird
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean hasEncryptedEntry = tab.getConfigTree().getAllEntries().stream().anyMatch(ConfigEntry::isEncrypted);
            assertThat(hasEncryptedEntry).as("Entry with ENC[...] should be recognized as encrypted").isTrue();
        }

        @Test
        void shouldShowLockIconForMarkedEntries() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. Entry selektieren und Marker togglen (Alt+M)
            tree.clickRow(2);
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_M);
            robot.releaseKey(java.awt.event.KeyEvent.VK_M);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Pruefen ob Entry als markiert gilt
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean hasMarkedEntry = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(ConfigEntry::isMarkedForEncryption);
            assertThat(hasMarkedEntry).as("Entry should be marked for encryption").isTrue();
        }
    }

    @Nested
    class Selection {

        @Test
        void shouldSelectNodeOnSingleClick() throws InterruptedException {
            // Setup: create content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // Click on a tree node
            JTreeFixture tree = window.tree("configTree");
            tree.clickRow(1); // First child under root

            // Verify selection
            int[] selectedRows = tree.target().getSelectionRows();
            assertThat(selectedRows).isNotNull().hasSize(1);
        }

        @Test
        void shouldSupportMultiSelectionMode() throws InterruptedException {
            // Setup: create content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // Verify tree is configured for multi-selection
            JTreeFixture tree = window.tree("configTree");
            int selectionMode = tree.target().getSelectionModel().getSelectionMode();
            // DISCONTIGUOUS_TREE_SELECTION = 4
            assertThat(selectionMode).isEqualTo(javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        }

        @Test
        void shouldAddToSelectionOnCtrlClick() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. Erste Row selektieren
            tree.clickRow(2);

            // 4. Zweite Row mit Ctrl+Click hinzufuegen
            robot.pressKey(java.awt.event.KeyEvent.VK_CONTROL);
            tree.clickRow(3);
            robot.releaseKey(java.awt.event.KeyEvent.VK_CONTROL);
            robot.waitForIdle();

            // 5. Pruefen ob beide selektiert
            int[] selectedRows = tree.target().getSelectionRows();
            assertThat(selectedRows).hasSize(2);
        }

        @Test
        void shouldExtendSelectionOnShiftClick() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. Erste Row selektieren
            tree.clickRow(2);

            // 4. Bereich mit Shift+Click erweitern
            robot.pressKey(java.awt.event.KeyEvent.VK_SHIFT);
            tree.clickRow(4);
            robot.releaseKey(java.awt.event.KeyEvent.VK_SHIFT);
            robot.waitForIdle();

            // 5. Pruefen ob Range selektiert (mindestens 2 rows)
            int[] selectedRows = tree.target().getSelectionRows();
            assertThat(selectedRows.length).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    class ContextMenu {

        @Test
        void shouldShowContextMenuOnRightClick() throws InterruptedException {
            // Setup: create content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // Right-click on tree node
            JTreeFixture tree = window.tree("configTree");
            tree.rightClickRow(1);

            // Verify popup menu appears
            JPopupMenu popup = robot.findActivePopupMenu();
            assertThat(popup).isNotNull();
            assertThat(popup.isVisible()).isTrue();
        }

        @Test
        void shouldShowToggleEncryptionMarkerMenuItem() throws InterruptedException {
            // Setup: create content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // Expand tree to access leaf entry
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0); // Expand root
            tree.expandRow(1); // Expand database group
            Thread.sleep(200);

            // Right-click on leaf entry (not group)
            tree.rightClickRow(2); // Click on "host" entry

            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Toggle Encryption Marker").requireVisible();
        }

        @Test
        void shouldShowCopyKeyMenuItem() throws InterruptedException {
            // Setup: create content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // Expand tree to access leaf entry
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0); // Expand root
            tree.expandRow(1); // Expand database group
            Thread.sleep(200);

            // Right-click on leaf entry
            tree.rightClickRow(2);

            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Copy Key").requireVisible();
        }

        @Test
        void shouldCopyKeyToClipboard() throws Exception {
            // Setup: create content
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // Expand tree and select a leaf entry
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0); // Expand root
            tree.expandRow(1); // Expand database
            Thread.sleep(200);
            tree.rightClickRow(2); // Click on "host"

            // Click Copy Key
            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Copy Key").click();

            // Verify clipboard
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).isNotEmpty();
        }

        @Test
        void shouldCopyValueToClipboard() throws Exception {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry rechtsklicken
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.rightClickRow(2); // host entry

            // 3. Copy Value klicken
            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Copy Value").click();

            // 4. Clipboard pruefen
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).isEqualTo("localhost");
        }

        @Test
        void shouldCopyEntryToClipboard() throws Exception {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry rechtsklicken
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.rightClickRow(2);

            // 3. Copy Entry klicken
            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Copy Entry").click();

            // 4. Clipboard pruefen - sollte key=value oder key: value enthalten
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).contains("host").contains("localhost");
        }

        @Test
        void shouldCopyAllChildrenForGroup() throws Exception {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Group rechtsklicken (database)
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            Thread.sleep(200);
            tree.rightClickRow(1); // database group

            // 3. Copy Value klicken
            JPopupMenu popup = robot.findActivePopupMenu();
            JPopupMenuFixture popupFixture = new JPopupMenuFixture(robot, popup);
            popupFixture.menuItemWithPath("Copy Value").click();

            // 4. Clipboard pruefen - sollte alle children enthalten
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).contains("host").contains("port").contains("password");
        }
    }

    @Nested
    class DoubleClickEncryption {

        @Test
        void shouldEncryptPlainEntryOnDoubleClick() throws InterruptedException {
            // 1. Environment erstellen
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("DoubleClickEnv");
            dialog.textBox("masterPasswordField").enterText("doubleclick123");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Content mit plain value erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: plainvalue");
            Thread.sleep(800);

            // 3. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 4. Doppelklick auf Entry
            tree.doubleClickRow(2);
            Thread.sleep(500);

            // 5. Pruefen ob verschluesselt
            String content = window.textBox("fileEditor").text();
            assertThat(content).contains("ENC[");
        }

        @Test
        void shouldDecryptEncryptedEntryOnDoubleClick() throws InterruptedException {
            // 1. Environment erstellen
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("DecryptEnv");
            dialog.textBox("masterPasswordField").enterText("decryptpass123");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Content erstellen und verschluesseln
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: testvalue");
            Thread.sleep(800);

            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. Verschluesseln via Doppelklick
            tree.doubleClickRow(2);
            Thread.sleep(500);

            // Verify encrypted
            String encryptedContent = window.textBox("fileEditor").text();
            assertThat(encryptedContent).contains("ENC[");

            // 4. Nochmal Doppelklick zum Entschluesseln
            tree.doubleClickRow(2);
            Thread.sleep(500);

            // 5. Pruefen ob entschluesselt
            String decryptedContent = window.textBox("fileEditor").text();
            assertThat(decryptedContent).contains("testvalue");
            assertThat(decryptedContent).doesNotContain("ENC[");
        }

        @Test
        void shouldShowErrorOnDoubleClickWithoutPassword() throws InterruptedException {
            // 1. Content OHNE Environment erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: testvalue");
            Thread.sleep(800);

            // 2. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);

            // 3. Doppelklick ohne Environment
            tree.doubleClickRow(2);
            Thread.sleep(300);

            // 4. Error-Dialog pruefen
            window.optionPane().requireVisible();
            window.optionPane().okButton().click();
        }

        @Test
        void shouldDoNothingOnDoubleClickOnGroup() throws InterruptedException {
            // 1. Environment erstellen (um Crypto-Fehler auszuschliessen)
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("GroupTestEnv");
            dialog.textBox("masterPasswordField").enterText("grouptestpass1");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            String contentBefore = window.textBox("fileEditor").text();

            // 3. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            Thread.sleep(200);

            // 4. Doppelklick auf Group (database)
            tree.doubleClickRow(1);
            Thread.sleep(300);

            // 5. Content sollte unveraendert sein
            String contentAfter = window.textBox("fileEditor").text();
            assertThat(contentAfter).isEqualTo(contentBefore);
        }
    }

    @Nested
    class KeyboardShortcuts {

        @Test
        void shouldToggleEncryptionMarkerWithAltM() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2);

            // 3. Pruefen: nicht markiert
            FileTab tab = GuiActionRunner.execute(() -> frame.getTabbedEditorPanel().getActiveTab());
            boolean initiallyMarked = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(ConfigEntry::isMarkedForEncryption);
            assertThat(initiallyMarked).isFalse();

            // 4. Alt+M druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_M);
            robot.releaseKey(java.awt.event.KeyEvent.VK_M);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 5. Pruefen: jetzt markiert
            boolean nowMarked = tab
                    .getConfigTree()
                    .getAllEntries()
                    .stream()
                    .anyMatch(ConfigEntry::isMarkedForEncryption);
            assertThat(nowMarked).as("Entry should be marked after Alt+M").isTrue();
        }

        @Test
        void shouldToggleEncryptDecryptWithAltC() throws InterruptedException {
            // 1. Environment erstellen
            window.menuItemWithPath("Settings", "Environments...").click();
            DialogFixture dialog = window.dialog();
            dialog.textBox("environmentNameField").enterText("AltCEnv");
            dialog.textBox("masterPasswordField").enterText("altcpassword12");
            dialog.button("saveButton").click();
            dialog.button("okButton").click();
            Thread.sleep(200);

            // 2. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText("config:\n  secret: plaintext");
            Thread.sleep(800);

            // 3. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2);

            // 4. Alt+C druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_C);
            robot.releaseKey(java.awt.event.KeyEvent.VK_C);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(500);

            // 5. Pruefen: verschluesselt
            String content = window.textBox("fileEditor").text();
            assertThat(content).contains("ENC[");
        }

        @Test
        void shouldCopyKeyWithAltK() throws Exception {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host

            // 3. Alt+K druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_K);
            robot.releaseKey(java.awt.event.KeyEvent.VK_K);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Clipboard pruefen
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).isEqualTo("host");
        }

        @Test
        void shouldCopyValueWithAltV() throws Exception {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host

            // 3. Alt+V druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_V);
            robot.releaseKey(java.awt.event.KeyEvent.VK_V);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Clipboard pruefen
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).isEqualTo("localhost");
        }

        @Test
        void shouldCopyEntryWithAltE() throws Exception {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host

            // 3. Alt+E druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Clipboard pruefen - sollte key: value oder key=value enthalten
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).contains("host").contains("localhost");
        }
    }

    @Nested
    class FormatDetection {

        @Test
        void shouldUseEqualsSeparatorForPropertiesFile() throws Exception {
            // 1. Properties content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_PROPERTIES);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            Thread.sleep(200);
            tree.clickRow(1); // database.host

            // 3. Alt+E fuer Copy Entry
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Clipboard pruefen - sollte = Separator haben
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            assertThat(clipboardContent).contains("=");
        }

        @Test
        void shouldUseDefaultSeparatorForUnsavedYamlContent() throws Exception {
            // NOTE: Format detection requires saved files with .yaml/.yml extension.
            // Unsaved content defaults to properties format (= separator).
            // This test verifies the default behavior for unsaved YAML content.

            // 1. YAML content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host

            // 3. Alt+E fuer Copy Entry
            robot.pressKey(java.awt.event.KeyEvent.VK_ALT);
            robot.pressKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_E);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ALT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Clipboard pruefen - unsaved content uses = separator
            String clipboardContent = (String) Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            // Unsaved files default to properties format
            assertThat(clipboardContent).contains("=");
            assertThat(clipboardContent).contains("host");
            assertThat(clipboardContent).contains("localhost");
        }
    }

    @Nested
    class InlineEditing {

        @Test
        void shouldStartEditingOnInsertKey() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host entry

            // 3. Insert-Taste druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.releaseKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Pruefen ob Editing aktiv ist
            boolean isEditing = GuiActionRunner.execute(() -> tree.target().isEditing());
            assertThat(isEditing).as("Tree should be in editing mode after Insert key").isTrue();
        }

        @Test
        void shouldUpdateValueOnEnter() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host entry

            // 3. Insert-Taste druecken zum Start des Editings
            robot.pressKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.releaseKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 4. Neuen Wert eingeben
            robot.enterText("newhost");
            Thread.sleep(100);

            // 5. Enter druecken zum Bestaetigen
            robot.pressKey(java.awt.event.KeyEvent.VK_ENTER);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ENTER);
            robot.waitForIdle();
            Thread.sleep(300);

            // 6. Pruefen ob Editor-Text aktualisiert wurde
            String editorContent = window.textBox("fileEditor").text();
            assertThat(editorContent).contains("newhost");
            assertThat(editorContent).doesNotContain("localhost");
        }

        @Test
        void shouldCancelEditingOnEscape() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren und Entry selektieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            tree.expandRow(1);
            Thread.sleep(200);
            tree.clickRow(2); // host entry

            // 3. Insert-Taste druecken zum Start des Editings
            robot.pressKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.releaseKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.waitForIdle();
            Thread.sleep(300);

            // 4. Verify editing is active
            boolean isEditing = GuiActionRunner.execute(() -> tree.target().isEditing());
            assertThat(isEditing).as("Tree should be in editing mode").isTrue();

            // 5. Focus on the cell editor textfield and type
            javax.swing.JTextField cellEditorField = GuiActionRunner.execute(() -> {
                javax.swing.CellEditor cellEditor = tree.target().getCellEditor();
                if (cellEditor instanceof javax.swing.DefaultCellEditor) {
                    return (javax.swing.JTextField) ((javax.swing.DefaultCellEditor) cellEditor).getComponent();
                }
                return null;
            });
            assertThat(cellEditorField).isNotNull();

            // 5. Press Escape to cancel without any text changes
            robot.pressKey(java.awt.event.KeyEvent.VK_ESCAPE);
            robot.releaseKey(java.awt.event.KeyEvent.VK_ESCAPE);
            robot.waitForIdle();
            Thread.sleep(300);

            // 6. Verify editing was cancelled
            boolean isEditingAfter = GuiActionRunner.execute(() -> tree.target().isEditing());
            assertThat(isEditingAfter).as("Tree should not be in editing mode after Escape").isFalse();

            // 7. Verify editor unchanged
            String editorContent = window.textBox("fileEditor").text();
            assertThat(editorContent).contains("localhost");
        }

        @Test
        void shouldNotEditGroupNodes() throws InterruptedException {
            // 1. Content erstellen
            window.menuItemWithPath("File", "New").click();
            window.textBox("fileEditor").enterText(SAMPLE_YAML);
            Thread.sleep(800);

            // 2. Tree expandieren
            JTreeFixture tree = window.tree("configTree");
            tree.expandRow(0);
            Thread.sleep(200);

            // 3. Group-Node selektieren (database)
            tree.clickRow(1);

            // 4. Insert-Taste druecken
            robot.pressKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.releaseKey(java.awt.event.KeyEvent.VK_INSERT);
            robot.waitForIdle();
            Thread.sleep(200);

            // 5. Pruefen dass KEIN Editing aktiv ist
            boolean isEditing = GuiActionRunner.execute(() -> tree.target().isEditing());
            assertThat(isEditing).as("Group nodes should not be editable").isFalse();
        }
    }
}
