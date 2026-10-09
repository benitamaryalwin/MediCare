package com.medicarepro.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

final class UiKit {
    private UiKit() { }

    static RoundedPanel surface(LayoutManager layout, int radius, Color color) {
        RoundedPanel panel = new RoundedPanel(layout, radius);
        panel.setBackground(color);
        return panel;
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 17, 10, 17));
        button.setBackground(primary ? SwingTheme.ACCENT : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : SwingTheme.INK);
        return button;
    }

    static void input(JComponent component) {
        component.setBackground(new Color(246, 249, 253));
        component.setForeground(SwingTheme.INK);
        component.setFont(new Font("SansSerif", Font.PLAIN, 13));
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(211, 223, 235)),
                new EmptyBorder(9, 12, 9, 12)));
    }

    static JLabel eyebrow(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(SwingTheme.ACCENT);
        return label;
    }

    static final class RoundedPanel extends JPanel {
        private final int radius;

        RoundedPanel(LayoutManager layout, int radius) {
            super(layout);
            this.radius = radius;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    static final class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        GradientPanel(LayoutManager layout, Color start, Color end) {
            super(layout);
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
