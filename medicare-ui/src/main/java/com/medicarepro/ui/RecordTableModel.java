package com.medicarepro.ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Generic JTable adapter that maps domain objects into Swing table rows. */
public final class RecordTableModel<T> extends AbstractTableModel {
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
