package com.medicarepro.ui;

import com.medicarepro.service.ClinicEvents;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

/** Shared Swing panel behavior; concrete modules override CRUD actions (polymorphism). */
public abstract class RecordPanel<T> extends JPanel implements Refreshable {
    protected final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    protected final JTable table;
    protected final RecordTableModel<T> tableModel;
    protected final JTextField searchField = new JTextField(24);
    protected final ClinicEvents events;
    private final JLabel countLabel = new JLabel("Loading records…");
    private final JLabel subtitleLabel = new JLabel("Manage clinic records. Your account only sees its own records.");
    private final Timer searchTimer;
    private boolean busy;

    protected RecordPanel(String title, String[] columns, java.util.function.Function<T, Object[]> rowAdapter,
                          ClinicEvents events) {
        super(new BorderLayout(14, 14));
        setBackground(SwingTheme.PAPER);
        this.events = events;
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        tableModel = new RecordTableModel<>(columns, rowAdapter);
        table = new JTable(tableModel);
        table.setRowHeight(42);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setShowGrid(false);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(231, 240, 238));
        table.getTableHeader().setForeground(SwingTheme.MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focus, int row, int column) {
                Component component = super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                if (!selected) component.setBackground(row % 2 == 0 ? SwingTheme.WHITE : new Color(246, 250, 249));
                if (value != null && (String.valueOf(value).equalsIgnoreCase("Available")
                        || String.valueOf(value).equalsIgnoreCase("Active")
                        || String.valueOf(value).equalsIgnoreCase("Completed"))) {
                    component.setForeground(new Color(20, 126, 92));
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                } else if (value != null && (String.valueOf(value).equalsIgnoreCase("Discharged")
                        || String.valueOf(value).equalsIgnoreCase("Missed")
                        || String.valueOf(value).equalsIgnoreCase("Disabled"))) {
                    component.setForeground(new Color(181, 79, 72));
                } else if (!selected) {
                    component.setForeground(SwingTheme.INK);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return component;
            }
        });
        add(header(title), BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SwingTheme.LINE),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        tableScroll.getViewport().setBackground(Color.WHITE);
        UiKit.RoundedPanel tableCard = UiKit.surface(new BorderLayout(), 16, Color.WHITE);
        tableCard.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        tableCard.add(tableScroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(countLabel, BorderLayout.WEST);
        countLabel.setForeground(SwingTheme.MUTED);
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(footer, BorderLayout.SOUTH);
        if (supportsAdd()) addToolbarButton("Add", () -> onAdd());
        if (supportsEdit()) addToolbarButton("Edit", () -> withSelected(this::onEdit));
        if (supportsDelete()) addToolbarButton("Delete", () -> withSelected(this::onDelete));
        addToolbarButton("Refresh", this::refresh);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(new JLabel("Search"));
        toolbar.add(searchField);
        searchTimer = new Timer(250, event -> refresh());
        searchTimer.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { searchTimer.restart(); }
        });
        events.addListener(event -> SwingUtilities.invokeLater(this::refresh));
        SwingUtilities.invokeLater(this::refresh);
    }

    private JComponent header(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(SwingTheme.INK);
        subtitleLabel.setForeground(SwingTheme.MUTED);
        JPanel copy = new JPanel(new GridLayout(2, 1, 0, 4));
        copy.setOpaque(false);
        copy.add(heading);
        copy.add(subtitleLabel);
        panel.add(copy, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Sets the grey line under the page title. Subclasses call this from their own constructor
     * (after their fields are assigned) instead of overriding a method that the super constructor
     * would call too early.
     */
    protected final void setSubtitle(String text) { subtitleLabel.setText(text); }

    protected void addActionButton(String label, Runnable action) {
        addToolbarButton(label, action);
    }

    protected final void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            if (widths[i] > 0) {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
                table.getColumnModel().getColumn(i).setMinWidth(Math.min(widths[i], 52));
            }
        }
    }

    protected void addToolbarButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SwingTheme.LINE),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        button.addActionListener(event -> action.run());
        toolbar.add(button);
    }

    protected abstract List<T> loadRows(String search) throws Exception;
    protected abstract void onAdd();
    protected void onEdit(T row) { }
    protected void onDelete(T row) { }
    protected boolean supportsAdd() { return true; }
    protected boolean supportsEdit() { return true; }
    protected boolean supportsDelete() { return true; }

    protected T selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return tableModel.row(table.convertRowIndexToModel(viewRow));
    }

    protected void withSelected(java.util.function.Consumer<T> action) {
        T row = selected();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "MediCare Pro", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        action.accept(row);
    }

    protected void runAsync(String operation, Runnable task) {
        if (busy) return;
        busy = true;
        setActionsEnabled(false);
        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            @Override protected Object doInBackground() {
                task.run();
                return null;
            }
            @Override protected void done() {
                busy = false;
                setActionsEnabled(true);
                try {
                    get();
                    events.recordsChanged("clinic");
                    JOptionPane.showMessageDialog(RecordPanel.this, operation + " completed.",
                            "MediCare Pro", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception error) {
                    FormDialogs.showError(RecordPanel.this, error);
                }
                refresh();
            }
        };
        worker.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        searchField.setEnabled(enabled);
        for (Component component : toolbar.getComponents()) component.setEnabled(enabled);
    }

    protected void recordsChanged(String area) {
        firePropertyChange("records.changed", null, area);
    }

    @Override public final void refresh() {
        SwingWorker<List<T>, Void> worker = new SwingWorker<>() {
            @Override protected List<T> doInBackground() throws Exception {
                return loadRows(searchField.getText());
            }
            @Override protected void done() {
                try {
                    List<T> rows = get();
                    tableModel.replaceRows(rows);
                    countLabel.setText(rows.size() + (rows.size() == 1 ? " record" : " records"));
                } catch (Exception error) {
                    tableModel.replaceRows(List.of());
                    countLabel.setText("Unable to load records");
                    FormDialogs.showError(RecordPanel.this, error);
                }
            }
        };
        worker.execute();
    }
}
