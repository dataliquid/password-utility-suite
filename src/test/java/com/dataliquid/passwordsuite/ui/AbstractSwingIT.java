package com.dataliquid.passwordsuite.ui;

import java.util.concurrent.TimeUnit;

import javax.swing.UIManager;

import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Abstract base class for AssertJ Swing UI tests. Provides common setup and
 * teardown for Swing component testing.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public abstract class AbstractSwingIT {

    protected Robot robot;

    @BeforeAll
    static void setUpOnce() {
        // Install repaint manager that detects EDT violations
        FailOnThreadViolationRepaintManager.install();

        // Initialize FlatLaf Look and Feel (same as production)
        GuiActionRunner.execute(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                // Fall back to system default if FlatLaf fails
            }
            return null;
        });
    }

    @BeforeEach
    void setUp() {
        robot = BasicRobot.robotWithCurrentAwtHierarchy();
    }

    @AfterEach
    void tearDown() {
        if (robot != null) {
            robot.cleanUp();
        }
    }
}
