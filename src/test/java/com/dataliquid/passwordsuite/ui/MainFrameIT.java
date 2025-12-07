package com.dataliquid.passwordsuite.ui;

import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * UI tests for MainFrame using AssertJ Swing.
 */
class MainFrameIT extends AbstractSwingIT {

    private FrameFixture window;

    @BeforeEach
    void setUpFrame() {
        MainFrame frame = GuiActionRunner.execute(() -> new MainFrame());
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
    void shouldDisplayMainFrame() {
        window.requireVisible();
        window.requireTitle("Password Utility Suite");
    }

    @Test
    void shouldHaveMenuBar() {
        JMenuBar menuBar = ((JFrame) window.target()).getJMenuBar();
        assertThat(menuBar).isNotNull();
        assertThat(menuBar.getMenuCount()).isEqualTo(3);
    }

    @Test
    void shouldHaveFileMenu() {
        window.menuItemWithPath("File").click();
        window.menuItemWithPath("File", "New").requireVisible();
        window.menuItemWithPath("File", "Open...").requireVisible();
        window.menuItemWithPath("File", "Save").requireVisible();
    }

    @Test
    void shouldHaveEditMenu() {
        window.menuItemWithPath("Edit").click();
        window.menuItemWithPath("Edit", "Undo").requireVisible();
        window.menuItemWithPath("Edit", "Redo").requireVisible();
    }

    @Test
    void shouldHaveSettingsMenu() {
        window.menuItemWithPath("Settings").click();
        window.menuItemWithPath("Settings", "Environments...").requireVisible();
    }
}
