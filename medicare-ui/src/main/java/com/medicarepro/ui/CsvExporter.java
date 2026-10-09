package com.medicarepro.ui;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvExporter {
    private CsvExporter() { }

    public static void export(Component parent, JTable table, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(suggestedName));
        chooser.setDialogTitle("Export the visible records");
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".csv")) {
            path = path.resolveSibling(path.getFileName() + ".csv");
        }
        TableModel model = table.getModel();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                if (col > 0) writer.write(',');
                writer.write(quote(model.getColumnName(col)));
            }
            writer.newLine();
            for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                for (int col = 0; col < model.getColumnCount(); col++) {
                    if (col > 0) writer.write(',');
                    Object value = model.getValueAt(modelRow, col);
                    writer.write(quote(value == null ? "" : value.toString()));
                }
                writer.newLine();
            }
            JOptionPane.showMessageDialog(parent, "Saved " + table.getRowCount() + " rows to:\n" + path,
                    "CSV export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            FormDialogs.showError(parent, exception);
        }
    }

    private static String quote(String value) {
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
