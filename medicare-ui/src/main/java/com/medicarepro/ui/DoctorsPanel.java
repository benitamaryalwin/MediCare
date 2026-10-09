package com.medicarepro.ui;

import com.medicarepro.model.Doctor;
import com.medicarepro.model.DoctorInput;
import com.medicarepro.model.User;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public final class DoctorsPanel extends RecordPanel<Doctor> {
    private static final String[] COLUMNS = {"ID", "Doctor", "Specialization", "Qualifications", "Experience", "Phone", "Status"};
    private final ClinicRepository repository;
    private final User user;

    public DoctorsPanel(ClinicRepository repository, User user, ClinicEvents events) {
        super("Doctor directory", COLUMNS, DoctorsPanel::row, events);
        this.repository = repository;
        this.user = user;
        setColumnWidths(60, 190, 170, 180, 100, 130, 110);
        setSubtitle(user.isAdmin()
                ? "Head Doctor only · manage doctors, specializations, and availability."
                : "View the doctor directory. Only the Head Doctor can change doctor records.");
        if (user.isAdmin()) {
            addActionButton("Cycle availability", () -> withSelected(doctor ->
                    runAsync("Availability update", () -> repository.cycleDoctorStatus(user, doctor.id()))));
        } else {
            for (java.awt.Component component : toolbar.getComponents()) {
                if (component instanceof JButton button &&
                        java.util.Set.of("Add", "Edit", "Delete").contains(button.getText())) {
                    button.setVisible(false);
                }
            }
        }
        addActionButton("Export CSV", () -> CsvExporter.export(this, table, "medicare-doctors.csv"));
    }

    @Override protected List<Doctor> loadRows(String search) {
        return repository.listDoctors(user, search);
    }

    @Override protected void onAdd() {
        Map<String, String> values = doctorForm(null);
        if (values == null) return;
        try {
            DoctorInput input = input(values);
            runAsync("Doctor registration", () -> repository.addDoctor(user, input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onEdit(Doctor doctor) {
        Map<String, String> values = doctorForm(doctor);
        if (values == null) return;
        try {
            DoctorInput input = input(values);
            runAsync("Doctor update", () -> repository.updateDoctor(user, doctor.id(), input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onDelete(Doctor doctor) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Remove " + doctor.name() + " from your doctor directory?", "Delete doctor",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            runAsync("Doctor deletion", () -> repository.deleteDoctor(user, doctor.id()));
        }
    }

    private Map<String, String> doctorForm(Doctor doctor) {
        return FormDialogs.show(this, doctor == null ? "Add doctor" : "Edit doctor",
                List.of(FieldSpec.text("name", "Name *", doctor == null ? "" : doctor.name()),
                        FieldSpec.text("specialization", "Specialization *", doctor == null ? "" : doctor.specialization()),
                        FieldSpec.text("qualifications", "Qualifications", doctor == null ? "" : doctor.qualifications()),
                        FieldSpec.text("experience", "Years of experience", doctor == null ? "0" : String.valueOf(doctor.experienceYears())),
                        FieldSpec.text("phone", "Phone", doctor == null ? "" : doctor.phone())));
    }

    private static DoctorInput input(Map<String, String> values) {
        try {
            return new DoctorInput(values.get("name"), values.get("specialization"), values.get("qualifications"),
                    Integer.parseInt(values.get("experience")), values.get("phone"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Experience must be a whole number.");
        }
    }

    private static Object[] row(Doctor doctor) {
        return new Object[]{doctor.id(), doctor.name(), doctor.specialization(), doctor.qualifications(),
                doctor.experienceYears() + " yrs", doctor.phone(), doctor.status()};
    }
}
