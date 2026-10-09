package com.medicarepro.ui;

import com.medicarepro.model.Appointment;
import com.medicarepro.model.Consultation;
import com.medicarepro.model.ConsultationInput;
import com.medicarepro.model.User;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

public final class ConsultationsPanel extends JPanel implements Refreshable {
    private static final String[] QUEUE_COLUMNS = {"ID", "Patient", "Doctor", "Appointment", "Status"};
    private static final String[] HISTORY_COLUMNS = {"ID", "Patient", "Doctor", "Diagnosis", "Medication", "Follow-up", "Recorded"};
    private final ClinicRepository repository;
    private final ClinicEvents events;
    private final User user;
    private final RecordTableModel<Appointment> queueModel =
            new RecordTableModel<>(QUEUE_COLUMNS, ConsultationsPanel::queueRow);
    private final RecordTableModel<Consultation> historyModel =
            new RecordTableModel<>(HISTORY_COLUMNS, ConsultationsPanel::historyRow);
    private final JTable queueTable = new JTable(queueModel);
    private final JTable historyTable = new JTable(historyModel);

    public ConsultationsPanel(ClinicRepository repository, User user, ClinicEvents events) {
        super(new BorderLayout(14, 14));
        this.repository = repository;
        this.user = user;
        this.events = events;
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        add(header(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Waiting queue", configured(queueTable));
        tabs.addTab("Consultation history", configured(historyTable));
        add(tabs, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton startNext = new JButton("Start next in queue");
        startNext.addActionListener(event -> startNext());
        JButton complete = new JButton("Complete selected");
        complete.addActionListener(event -> completeSelected());
        JButton undo = new JButton("Undo latest consultation");
        undo.addActionListener(event -> undoLatest());
        JButton export = new JButton("Export history CSV");
        export.addActionListener(event -> CsvExporter.export(this, historyTable, "medicare-consultations.csv"));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        actions.add(startNext);
        actions.add(complete);
        actions.add(undo);
        actions.add(export);
        actions.add(refresh);
        add(actions, BorderLayout.SOUTH);
        events.addListener(event -> SwingUtilities.invokeLater(this::refresh));
        SwingUtilities.invokeLater(this::refresh);
    }

    private JComponent header() {
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Consultations");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel(user.isAdmin()
                ? "Administrator view · appointments are served in first-in, first-out order."
                : "Serve the appointment queue, record care, and review the latest-first history.");
        subtitle.setForeground(SwingTheme.MUTED);
        header.add(title);
        header.add(subtitle);
        return header;
    }

    private static JScrollPane configured(JTable table) {
        table.setRowHeight(34);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        return new JScrollPane(table);
    }

    private void startNext() {
        List<Appointment> queue = repository.listConsultationQueue(user);
        ArrayDeque<Appointment> fifo = new ArrayDeque<>(queue);
        Appointment next = fifo.stream().filter(a -> !"In Consultation".equals(a.status())).findFirst().orElse(null);
        if (next == null) {
            JOptionPane.showMessageDialog(this, "There are no appointments waiting.", "Queue", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        runAsync("Consultation started", () -> repository.startConsultation(user, next.id()));
    }

    private void completeSelected() {
        int viewRow = queueTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select an appointment from the waiting queue first.",
                    "Consultations", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Appointment appointment = queueModel.row(queueTable.convertRowIndexToModel(viewRow));
        if (!"In Consultation".equals(appointment.status())) {
            JOptionPane.showMessageDialog(this, "Start this appointment from the queue before completing it.",
                    "Consultations", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Map<String, String> values = FormDialogs.show(this, "Record consultation for " + appointment.patientName(),
                List.of(FieldSpec.text("age", "Patient age", "0"),
                        FieldSpec.choice("gender", "Gender", "Not specified", "Not specified", "Female", "Male", "Other"),
                        FieldSpec.multiline("complaints", "Complaints", ""),
                        FieldSpec.multiline("diagnosis", "Diagnosis", ""),
                        FieldSpec.multiline("medication", "Medication", ""),
                        FieldSpec.text("followUp", "Follow-up date (yyyy-MM-dd, optional)", ""),
                        FieldSpec.multiline("notes", "Notes", "")));
        if (values == null) return;
        try {
            int age = Integer.parseInt(values.get("age"));
            LocalDate followUp = values.get("followUp").isBlank()
                    ? null : LocalDate.parse(values.get("followUp"), DateTimeFormatter.ISO_LOCAL_DATE);
            ConsultationInput input = new ConsultationInput(appointment.id(), age, values.get("gender"),
                    values.get("complaints"), values.get("diagnosis"), values.get("medication"), followUp,
                    values.get("notes"));
            runAsync("Consultation saved", () -> repository.saveConsultation(user, input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    private void undoLatest() {
        if (JOptionPane.showConfirmDialog(this,
                "Remove the newest consultation record and return its appointment to the waiting queue?",
                "Undo latest consultation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        runAsync("Undo", () -> {
            if (!repository.undoLastConsultation(user)) throw new IllegalArgumentException("There are no consultations to undo.");
        });
    }

    private void runAsync(String message, Runnable action) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Object, Void>() {
            @Override protected Object doInBackground() {
                action.run();
                return null;
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    events.recordsChanged("clinic");
                    JOptionPane.showMessageDialog(ConsultationsPanel.this, message + ".", "MediCare Pro",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception error) {
                    FormDialogs.showError(ConsultationsPanel.this, error);
                }
                refresh();
            }
        }.execute();
    }

    @Override public void refresh() {
        new SwingWorker<Snapshot, Void>() {
            @Override protected Snapshot doInBackground() {
                return new Snapshot(repository.listConsultationQueue(user), repository.listConsultations(user));
            }
            @Override protected void done() {
                try {
                    Snapshot snapshot = get();
                    queueModel.replaceRows(snapshot.queue());
                    historyModel.replaceRows(snapshot.history());
                } catch (Exception error) {
                    queueModel.replaceRows(List.of());
                    historyModel.replaceRows(List.of());
                    FormDialogs.showError(ConsultationsPanel.this, error);
                }
            }
        }.execute();
    }

    private static Object[] queueRow(Appointment appointment) {
        return new Object[]{appointment.id(), appointment.patientName(), appointment.doctorName(),
                appointment.appointmentAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                appointment.status()};
    }

    private static Object[] historyRow(Consultation consultation) {
        return new Object[]{consultation.id(), consultation.patientName(), consultation.doctorName(),
                consultation.diagnosis(), consultation.medication(),
                consultation.followUpDate() == null ? "—" : consultation.followUpDate(),
                consultation.createdAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))};
    }

    private record Snapshot(List<Appointment> queue, List<Consultation> history) { }
}
