package com.dataliquid.passwordsuite;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataliquid.passwordsuite.ui.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Main entry point for the Password Utility Suite application.
 */
public class PasswordUtilitySuite {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtilitySuite.class);

    public static void main(String[] args) {
        logger.info("Starting Password Utility Suite application");

        // Set FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            logger.info("FlatLaf Look and Feel initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize FlatLaf Look and Feel", e);
            // Fall back to system default
        }

        // Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                logger.info("MainFrame displayed successfully");
            } catch (Exception e) {
                logger.error("Failed to create MainFrame", e);
            }
        });
    }
}
