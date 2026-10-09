package com.medicarepro.ui;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class SwingTheme {
    public static final Color INK = new Color(25, 43, 52);
    public static final Color MUTED = new Color(103, 121, 130);
    public static final Color ACCENT = new Color(25, 132, 112);
    public static final Color ACCENT_DARK = new Color(18, 91, 83);
    public static final Color PAPER = new Color(244, 247, 248);
    public static final Color WHITE = Color.WHITE;
    public static final Color LINE = new Color(218, 227, 229);

    private SwingTheme() { }

    public static void install() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Keep the platform's current Swing look and feel if Nimbus is unavailable.
        }
        UIManager.put("Panel.background", PAPER);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Button.background", WHITE);
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Table.rowHeight", 36);
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 11));
        UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createEmptyBorder());
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 12));
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.background", WHITE);
        UIManager.put("Table.selectionBackground", new Color(218, 235, 228));
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TextField.background", WHITE);
        UIManager.put("TextField.foreground", INK);
    }
}
