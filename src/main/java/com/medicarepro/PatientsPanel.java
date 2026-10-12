package com.medicarepro;


import javax.swing.*;
import java.util.List;
import java.util.Map;

public final class PatientsPanel extends RecordPanel<Patient> {
    private static final String[] COLUMNS = {"Patient ID", "Name", "Age", "Gender", "Contact", "Visit", "Status", "Source"};
    private final ClinicRepository repository;
    private final User user;

    public PatientsPanel(ClinicRepository repository, User user, ClinicEvents events) {
        super("Patient records", COLUMNS, PatientsPanel::row, events);
        this.repository = repository;
        this.user = user;
        setSubtitle(user.isAdmin()
                ? "Administrator view · manage patient records across all accounts."
                : "Register, search, update, and remove patient records.");
    }

    @Override protected List<Patient> loadRows(String search) {
        return repository.listPatients(user, search);
    }

    @Override protected void onAdd() {
        Map<String, String> values = patientForm(null);
        if (values == null) return;
        try {
            PatientInput input = toInput(values);
            runAsync("Patient registration", () -> repository.createPatient(user, input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onEdit(Patient patient) {
        Map<String, String> values = patientForm(patient);
        if (values == null) return;
        try {
            PatientInput input = toInput(values);
            runAsync("Patient update", () -> repository.updatePatient(user, patient.id(), input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onDelete(Patient patient) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + patient.name() + " (" + patient.patientCode() + ")? This cannot be undone.",
                "Delete patient", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            runAsync("Patient deletion", () -> repository.deletePatient(user, patient.id()));
        }
    }

    private Map<String, String> patientForm(Patient patient) {
        return FormDialogs.show(this, patient == null ? "Register patient" : "Update patient",
                List.of(
                        FieldSpec.text("name", "Full name *", patient == null && user.isPatient() ? user.displayName() : (patient == null ? "" : patient.name())),
                        FieldSpec.text("age", "Age *", patient == null ? "" : String.valueOf(patient.age())),
                        FieldSpec.choice("gender", "Gender", patient == null ? "Not specified" : patient.gender(),
                                "Not specified", "Female", "Male", "Other"),
                        FieldSpec.text("contact", "Contact *", patient == null ? "" : patient.contact()),
                        FieldSpec.choice("visit", "Visit type", patient == null ? "First Visit" : patient.visitType(),
                                "First Visit", "Follow-up"),
                        FieldSpec.multiline("diagnosis", "Diagnosis / notes", patient == null ? "" : patient.diagnosis()),
                        FieldSpec.choice("status", "Patient status", patient == null ? "Active" : patient.status(),
                                "Active", "Follow Up", "Discharged")
                ));
    }

    private static PatientInput toInput(Map<String, String> values) {
        int age;
        try {
            age = Integer.parseInt(values.get("age"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Age must be a whole number.");
        }
        return new PatientInput(values.get("name"), age, values.get("gender"), values.get("contact"),
                values.get("visit"), values.get("diagnosis"), values.get("status"));
    }

    private static Object[] row(Patient patient) {
        return new Object[]{patient.patientCode(), patient.name(), patient.age(), patient.gender(),
                patient.contact(), patient.visitType(), patient.status(), patient.source()};
    }
}
