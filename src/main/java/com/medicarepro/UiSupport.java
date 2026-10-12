package com.medicarepro;

import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

record FieldSpec(String key, String label, String initialValue, List<String> choices, boolean multiline) {
    public static FieldSpec text(String key, String label, String initialValue) {
        return new FieldSpec(key, label, initialValue == null ? "" : initialValue, List.of(), false);
    }

    public static FieldSpec choice(String key, String label, String initialValue, String... choices) {
        return new FieldSpec(key, label, initialValue == null ? "" : initialValue, List.of(choices), false);
    }

    public static FieldSpec multiline(String key, String label, String initialValue) {
        return new FieldSpec(key, label, initialValue == null ? "" : initialValue, List.of(), true);
    }
}

final class FormDialogs {
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
        String friendly = message == null || message.isBlank()
                ? "We could not complete that action. Please check the details and try again."
                : message;
        JOptionPane.showMessageDialog(parent,
                friendly + "\n\nNo changes were saved.",
                "Could not complete action", JOptionPane.ERROR_MESSAGE);
    }
}

/** Generic JTable adapter that maps domain objects into Swing table rows. */
final class RecordTableModel<T> extends AbstractTableModel {
    private final String[] columns;
    private final Function<T, Object[]> rowAdapter;
    private List<T> rows = new ArrayList<>();

    public RecordTableModel(String[] columns, Function<T, Object[]> rowAdapter) {
        this.columns = columns.clone();
        this.rowAdapter = rowAdapter;
    }

    public void replaceRows(List<T> records) {
        rows = new ArrayList<>(records);
        fireTableDataChanged();
    }

    public T row(int index) {
        return rows.get(index);
    }

    public int size() {
        return rows.size();
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int column) { return columns[column]; }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        return rowAdapter.apply(rows.get(rowIndex))[columnIndex];
    }
    @Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
}

interface Refreshable {
    void refresh();
}

final class SwingTheme {
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

/** Observer event source used by Swing screens to refresh after record changes. */
final class ClinicEvents {
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public void addListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void recordsChanged(String area) {
        changes.firePropertyChange("records", null, area);
    }
}
