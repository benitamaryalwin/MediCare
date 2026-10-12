package com.medicarepro;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class SwingTheme {
    public static final Color INK = new Color(31, 53, 58);
    public static final Color MUTED = new Color(101, 121, 122);
    public static final Color ACCENT = new Color(49, 119, 105);
    public static final Color PAPER = new Color(247, 247, 242);
    public static final Color WHITE = Color.WHITE;
    public static final Color LINE = new Color(225, 232, 228);

    private SwingTheme() { }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the platform's current Swing look and feel if Nimbus is unavailable.
        }
        UIManager.put("Panel.background", PAPER);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Button.background", WHITE);
        UIManager.put("Button.foreground", INK);
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.background", WHITE);
        UIManager.put("Table.selectionBackground", new Color(218, 235, 228));
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TextField.background", WHITE);
        UIManager.put("TextField.foreground", INK);
    }
}
