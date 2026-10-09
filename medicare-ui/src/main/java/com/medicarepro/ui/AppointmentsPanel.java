package com.medicarepro.ui;

import com.medicarepro.model.*;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class AppointmentsPanel extends RecordPanel<Appointment> {
    private static final String[] COLUMNS = {"ID", "Patient", "Doctor", "Appointment time", "Source", "Status"};
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm");
    private final ClinicRepository repository;
    private final User user;

    public AppointmentsPanel(ClinicRepository repository, User user, ClinicEvents events) {
        super("Appointments", COLUMNS, AppointmentsPanel::row, events);
        this.repository = repository;
        this.user = user;
        addActionButton("Move to waiting", () -> withSelected(appointment ->
                runAsync("Queue update", () -> repository.advanceAppointment(user, appointment.id()))));
        addActionButton("Mark missed", () -> withSelected(appointment -> {
            if (JOptionPane.showConfirmDialog(this, "Mark appointment #" + appointment.id() + " as missed?",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                runAsync("Appointment update", () -> repository.markAppointmentMissed(user, appointment.id()));
            }
        }));
        addActionButton("Export CSV", () -> CsvExporter.export(this, table, "medicare-appointments.csv"));
    }

    @Override protected List<Appointment> loadRows(String search) {
        return repository.listAppointments(user, search);
    }

    @Override protected void onAdd() {
        try {
            AppointmentInput input = appointmentForm(null);
            if (input == null) return;
            runAsync("Appointment booking", () -> repository.addAppointment(user, input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onEdit(Appointment appointment) {
        try {
            AppointmentInput input = appointmentForm(appointment);
            if (input == null) return;
            runAsync("Appointment update", () -> repository.updateAppointment(user, appointment.id(), input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onDelete(Appointment appointment) {
        if (JOptionPane.showConfirmDialog(this, "Cancel appointment #" + appointment.id() + "?",
                "Cancel appointment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            runAsync("Appointment cancellation", () -> repository.deleteAppointment(user, appointment.id()));
        }
    }

    private AppointmentInput appointmentForm(Appointment appointment) {
        List<Patient> patients = repository.listPatients(user, "");
        if (patients.isEmpty()) throw new IllegalArgumentException("Register a patient before booking an appointment.");
        HashMap<String, Patient> patientByLabel = new HashMap<>();
        TreeSet<String> sortedPatientLabels = new TreeSet<>();
        for (Patient patient : patients) {
            String label = patient.patientCode() + " · " + patient.name();
            patientByLabel.put(label, patient);
            sortedPatientLabels.add(label);
        }
        JComboBox<String> patientBox = new JComboBox<>(sortedPatientLabels.toArray(String[]::new));
        if (appointment != null && appointment.patientId() != null) {
            patientByLabel.forEach((label, patient) -> {
                if (patient.id() == appointment.patientId()) patientBox.setSelectedItem(label);
            });
        }
        JComboBox<String> doctorBox = new JComboBox<>();
        HashMap<String, Doctor> doctorByLabel = new HashMap<>();
        Runnable populateDoctors = () -> {
            doctorByLabel.clear();
            Patient patient = patientByLabel.get(String.valueOf(patientBox.getSelectedItem()));
            if (patient == null) {
                doctorBox.setModel(new DefaultComboBoxModel<>());
                return;
            }
            TreeSet<String> sortedDoctorLabels = new TreeSet<>();
            for (Doctor doctor : repository.listAvailableDoctorsForPatient(user, patient.id())) {
                String label = "#" + doctor.id() + " · " + doctor.name() + " — " + doctor.specialization();
                doctorByLabel.put(label, doctor);
                sortedDoctorLabels.add(label);
            }
            doctorBox.setModel(new DefaultComboBoxModel<>(sortedDoctorLabels.toArray(String[]::new)));
            if (appointment != null && appointment.doctorId() != null) {
                doctorByLabel.forEach((label, doctor) -> {
                    if (doctor.id() == appointment.doctorId()) doctorBox.setSelectedItem(label);
                });
            }
        };
        patientBox.addActionListener(event -> populateDoctors.run());
        populateDoctors.run();
        if (doctorByLabel.isEmpty()) {
            throw new IllegalArgumentException("Add an available doctor for the selected patient's account before booking.");
        }
        JTextField dateTimeField = new JTextField(appointment == null
                ? LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : appointment.appointmentAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 20);
        JComboBox<String> modeBox = new JComboBox<>(new String[]{"Offline", "Online"});
        if (appointment != null) modeBox.setSelectedItem(appointment.visitMode());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 7, 6, 7);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(new JLabel("Patient"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(patientBox, c);
        c.gridx = 0; c.gridy++;
        form.add(new JLabel("Available doctor"), c);
        c.gridx = 1;
        form.add(doctorBox, c);
        c.gridx = 0; c.gridy++;
        form.add(new JLabel("Date and time"), c);
        c.gridx = 1;
        form.add(dateTimeField, c);
        c.gridx = 0; c.gridy++;
        form.add(new JLabel("Booking source"), c);
        c.gridx = 1;
        form.add(modeBox, c);
        form.setPreferredSize(new Dimension(530, 190));
        if (JOptionPane.showConfirmDialog(this, form, appointment == null ? "Book appointment" : "Edit appointment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
        Patient patient = patientByLabel.get(String.valueOf(patientBox.getSelectedItem()));
        Doctor doctor = doctorByLabel.get(String.valueOf(doctorBox.getSelectedItem()));
        if (patient == null || doctor == null) throw new IllegalArgumentException("Choose a patient and available doctor.");
        try {
            OffsetDateTime dateTime = LocalDateTime.parse(dateTimeField.getText().trim(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
            return new AppointmentInput(patient.id(), doctor.id(), dateTime, String.valueOf(modeBox.getSelectedItem()));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Enter the appointment time as yyyy-MM-dd HH:mm.");
        }
    }

    private static Object[] row(Appointment appointment) {
        return new Object[]{appointment.id(), appointment.patientName(), appointment.doctorName(),
                appointment.appointmentAt().atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY),
                appointment.visitMode(), appointment.status()};
    }
}
