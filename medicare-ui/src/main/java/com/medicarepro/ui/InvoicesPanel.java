package com.medicarepro.ui;

import com.medicarepro.model.Invoice;
import com.medicarepro.model.InvoiceInput;
import com.medicarepro.model.User;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class InvoicesPanel extends RecordPanel<Invoice> {
    private static final String[] COLUMNS = {"Invoice", "Patient", "Doctor", "Fee", "Medication", "Other", "Total", "Payment", "Created"};
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final ClinicRepository repository;
    private final User user;

    public InvoicesPanel(ClinicRepository repository, User user, ClinicEvents events) {
        super("Invoices", COLUMNS, InvoicesPanel::row, events);
        this.repository = repository;
        this.user = user;
        addActionButton("Mark paid", () -> withSelected(invoice ->
                runAsync("Payment update", () -> repository.markInvoicePaid(user, invoice.id()))));
        addActionButton("Export CSV", () -> CsvExporter.export(this, table, "medicare-invoices.csv"));
    }

    @Override protected List<Invoice> loadRows(String search) {
        String term = search == null ? "" : search.trim().toLowerCase();
        return repository.listInvoices(user).stream()
                .filter(invoice -> term.isEmpty() || invoice.patientName().toLowerCase().contains(term)
                        || invoice.doctorName().toLowerCase().contains(term)
                        || String.valueOf(invoice.id()).contains(term))
                .toList();
    }

    @Override protected void onAdd() {
        Map<String, String> values = invoiceForm(null);
        if (values == null) return;
        try {
            InvoiceInput input = toInput(values);
            runAsync("Invoice creation", () -> repository.addInvoice(user, input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onEdit(Invoice invoice) {
        if ("PAID".equals(invoice.paymentStatus())) {
            JOptionPane.showMessageDialog(this, "Paid invoices are locked from editing.",
                    "Invoice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Map<String, String> values = invoiceForm(invoice);
        if (values == null) return;
        try {
            InvoiceInput input = toInput(values);
            runAsync("Invoice update", () -> repository.updateInvoice(user, invoice.id(), input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onDelete(Invoice invoice) {
        if (JOptionPane.showConfirmDialog(this, "Delete invoice #" + invoice.id() + "?",
                "Delete invoice", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            runAsync("Invoice deletion", () -> repository.deleteInvoice(user, invoice.id()));
        }
    }

    private Map<String, String> invoiceForm(Invoice invoice) {
        return FormDialogs.show(this, invoice == null ? "Create invoice" : "Edit invoice",
                List.of(FieldSpec.text("patient", "Patient *", invoice == null ? "" : invoice.patientName()),
                        FieldSpec.text("doctor", "Doctor *", invoice == null ? "" : invoice.doctorName()),
                        FieldSpec.text("fee", "Consultation fee", invoice == null ? "0.00" : invoice.consultationFee().toPlainString()),
                        FieldSpec.text("medication", "Medication amount", invoice == null ? "0.00" : invoice.medicationAmount().toPlainString()),
                        FieldSpec.text("other", "Other charges", invoice == null ? "0.00" : invoice.otherAmount().toPlainString())));
    }

    private static InvoiceInput toInput(Map<String, String> values) {
        return new InvoiceInput(values.get("patient"), values.get("doctor"),
                parseMoney(values.get("fee")), parseMoney(values.get("medication")), parseMoney(values.get("other")));
    }

    private static BigDecimal parseMoney(String amount) {
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid amount with no more than two decimal places.");
        }
    }

    private static Object[] row(Invoice invoice) {
        return new Object[]{String.format("INV-%05d", invoice.id()), invoice.patientName(), invoice.doctorName(),
                invoice.consultationFee(), invoice.medicationAmount(), invoice.otherAmount(), invoice.total(),
                invoice.paymentStatus(), invoice.createdAt().atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY)};
    }
}
