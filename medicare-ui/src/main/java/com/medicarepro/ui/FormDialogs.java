package com.medicarepro.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormDialogs {
    private FormDialogs() { }

    public static Map<String, String> show(Component parent, String title, List<FieldSpec> fields) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12, 8, 4, 8));
        Map<String, JComponent> inputs = new LinkedHashMap<>();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        for (int i = 0; i < fields.size(); i++) {
            FieldSpec field = fields.get(i);
            constraints.gridx = 0;
            constraints.gridy = i;
            constraints.weightx = 0;
            form.add(new JLabel(field.label()), constraints);
            JComponent input;
            if (!field.choices().isEmpty()) {
                JComboBox<String> combo = new JComboBox<>(field.choices().toArray(String[]::new));
                combo.setSelectedItem(field.initialValue());
                input = combo;
            } else if (field.multiline()) {
                JTextArea area = new JTextArea(field.initialValue(), 3, 25);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                input = new JScrollPane(area);
            } else {
                input = new JTextField(field.initialValue(), 24);
            }
            constraints.gridx = 1;
            constraints.weightx = 1;
            form.add(input, constraints);
            inputs.put(field.key(), input);
        }
        JScrollPane content = new JScrollPane(form);
        content.setBorder(BorderFactory.createEmptyBorder());
        content.setPreferredSize(new Dimension(520, Math.min(520, 52 + fields.size() * 48)));
        if (JOptionPane.showConfirmDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
        Map<String, String> values = new LinkedHashMap<>();
        inputs.forEach((key, input) -> {
            if (input instanceof JComboBox<?> combo) {
                values.put(key, String.valueOf(combo.getSelectedItem()));
            } else if (input instanceof JScrollPane scroll && scroll.getViewport().getView() instanceof JTextArea area) {
                values.put(key, area.getText().trim());
            } else if (input instanceof JTextField text) {
                values.put(key, text.getText().trim());
            }
        });
        return values;
    }

    public static char[] askPassword(Component parent, String title, String label) {
        JPasswordField password = new JPasswordField(22);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(password, BorderLayout.CENTER);
        if (JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
        return password.getPassword();
    }

    public static void showError(Component parent, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String message = cause.getMessage();
        JOptionPane.showMessageDialog(parent,
                message == null || message.isBlank() ? "The action could not be completed." : message,
                "MediCare Pro", JOptionPane.ERROR_MESSAGE);
    }
}
