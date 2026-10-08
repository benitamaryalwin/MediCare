package com.medicarepro.service;

import com.medicarepro.data.Database;
import com.medicarepro.data.DatabaseException;
import com.medicarepro.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * JDBC repository for the clinic records. Every user-owned query is scoped
 * here: administrators can view all rows; regular users are bound to their ID.
 */
public final class ClinicRepository {
    private static final Set<String> PATIENT_STATUSES = Set.of("Active", "Follow Up", "Discharged");
    private static final List<String> APPOINTMENT_FLOW =
            List.of("Scheduled", "Yet to be Consulted", "In Consultation", "Completed");
    private final Database database;

    public ClinicRepository(Database database) {
        this.database = database;
    }

    public List<Patient> listPatients(User actor, String search) {
        String term = likeTerm(search);
        String sql = """
                SELECT id, name, age, gender, contact, doctor, visit_type, diagnosis, status, source,
                       created_at, updated_at
                FROM patients p
                WHERE (CAST(p.id AS TEXT) ILIKE ? OR p.name ILIKE ? OR p.contact ILIKE ?
                       OR p.doctor ILIKE ? OR p.diagnosis ILIKE ?)
                """ + scope(actor, "p") + " ORDER BY p.created_at DESC, p.id DESC";
        List<Patient> patients = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) statement.setString(i, term);
            bindScope(statement, 6, actor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) patients.add(readPatient(rows));
            }
            return patients;
        } catch (SQLException exception) {
            throw databaseError("Could not load patient records.", exception);
        }
    }

    public Patient createPatient(User actor, PatientInput input) {
        validatePatient(input);
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO patients
                         (owner_user_id, name, age, gender, contact, doctor, visit_type, diagnosis, status, source)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     RETURNING id, name, age, gender, contact, doctor, visit_type, diagnosis, status, source,
                               created_at, updated_at
                     """)) {
            statement.setLong(1, actor.id());
            statement.setString(2, input.name().trim());
            statement.setInt(3, input.age());
            statement.setString(4, clean(input.gender()));
            statement.setString(5, clean(input.contact()));
            statement.setString(6, clean(input.doctor()));
            statement.setString(7, input.visitType());
            statement.setString(8, clean(input.diagnosis()));
            statement.setString(9, input.status());
            statement.setString(10, input.source());
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return readPatient(row);
            }
        } catch (SQLException exception) {
            throw databaseError("Could not save the patient record.", exception);
        }
    }

    public Patient updatePatient(User actor, long patientId, PatientInput input) {
        validatePatient(input);
        String sql = """
                UPDATE patients SET name = ?, age = ?, gender = ?, contact = ?, doctor = ?, visit_type = ?,
                    diagnosis = ?, status = ?, source = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """ + scope(actor, "patients") + """
                RETURNING id, name, age, gender, contact, doctor, visit_type, diagnosis, status, source,
                          created_at, updated_at
                """;
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPatient(statement, input, 1);
            statement.setLong(10, patientId);
            bindScope(statement, 11, actor);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Patient record not found.");
                return readPatient(row);
            }
        } catch (SQLException exception) {
            throw databaseError("Could not update the patient record.", exception);
        }
    }

    public void deletePatient(User actor, long patientId) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM patients WHERE id = ?" + scope(actor, "patients"))) {
            statement.setLong(1, patientId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Patient record not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not delete the patient record.", exception);
        }
    }

    public List<Doctor> listDoctors(User actor, String search) {
        String term = likeTerm(search);
        List<Doctor> doctors = new ArrayList<>();
        String sql = """
                SELECT id, name, specialization, qualifications, experience_years, phone, status
                FROM doctors d
                WHERE (d.name ILIKE ? OR d.specialization ILIKE ? OR d.phone ILIKE ?)
                """ + scope(actor, "d") + " ORDER BY d.name";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, term);
            statement.setString(2, term);
            statement.setString(3, term);
            bindScope(statement, 4, actor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) doctors.add(new Doctor(rows.getLong("id"), rows.getString("name"),
                        rows.getString("specialization"), rows.getString("qualifications"),
                        rows.getInt("experience_years"), rows.getString("phone"), rows.getString("status")));
            }
            return doctors;
        } catch (SQLException exception) {
            throw databaseError("Could not load doctors.", exception);
        }
    }

    public void addDoctor(User actor, DoctorInput input) {
        if (input.name().isBlank() || input.specialization().isBlank()) {
            throw new IllegalArgumentException("Enter a doctor name and specialization.");
        }
        if (input.experienceYears() < 0 || input.experienceYears() > 80) {
            throw new IllegalArgumentException("Experience must be between 0 and 80 years.");
        }
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO doctors
                         (owner_user_id, name, specialization, qualifications, experience_years, phone)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setLong(1, actor.id());
            statement.setString(2, input.name().trim());
            statement.setString(3, input.specialization().trim());
            statement.setString(4, clean(input.qualifications()));
            statement.setInt(5, input.experienceYears());
            statement.setString(6, clean(input.phone()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("Could not add the doctor.", exception);
        }
    }

    public void cycleDoctorStatus(User actor, long doctorId) {
        String sql = "UPDATE doctors SET status = CASE status WHEN 'Available' THEN 'In Consultation' " +
                "WHEN 'In Consultation' THEN 'On Leave' ELSE 'Available' END WHERE id = ?" + scope(actor, "doctors");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, doctorId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Doctor not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not change doctor availability.", exception);
        }
    }

    public void updateDoctor(User actor, long doctorId, DoctorInput input) {
        if (input.name().isBlank() || input.specialization().isBlank()) {
            throw new IllegalArgumentException("Enter a doctor name and specialization.");
        }
        if (input.experienceYears() < 0 || input.experienceYears() > 80) {
            throw new IllegalArgumentException("Experience must be between 0 and 80 years.");
        }
        String sql = """
                UPDATE doctors SET name = ?, specialization = ?, qualifications = ?,
                    experience_years = ?, phone = ?
                WHERE id = ?
                """ + scope(actor, "doctors");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, input.name().trim());
            statement.setString(2, input.specialization().trim());
            statement.setString(3, clean(input.qualifications()));
            statement.setInt(4, input.experienceYears());
            statement.setString(5, clean(input.phone()));
            statement.setLong(6, doctorId);
            bindScope(statement, 7, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Doctor not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not update the doctor.", exception);
        }
    }

    public void deleteDoctor(User actor, long doctorId) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM doctors WHERE id = ?" + scope(actor, "doctors"))) {
            statement.setLong(1, doctorId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Doctor not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not delete the doctor.", exception);
        }
    }

    public List<Appointment> listAppointments(User actor, String search) {
        String term = likeTerm(search);
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
                SELECT id, patient_id, doctor_id, patient_name, doctor_name, appointment_at, visit_mode, status, billed
                FROM appointments a
                WHERE (a.patient_name ILIKE ? OR a.doctor_name ILIKE ? OR a.status ILIKE ?)
                """ + scope(actor, "a") + " ORDER BY a.appointment_at, a.id";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, term);
            statement.setString(2, term);
            statement.setString(3, term);
            bindScope(statement, 4, actor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) appointments.add(readAppointment(rows));
            }
            return appointments;
        } catch (SQLException exception) {
            throw databaseError("Could not load appointments.", exception);
        }
    }

    public List<Appointment> listConsultationQueue(User actor) {
        List<Appointment> queue = new ArrayList<>();
        String sql = """
                SELECT id, patient_id, doctor_id, patient_name, doctor_name, appointment_at, visit_mode, status, billed
                FROM appointments a
                WHERE a.status IN ('Scheduled', 'Yet to be Consulted', 'In Consultation')
                """ + scope(actor, "a") + " ORDER BY a.appointment_at, a.id";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScope(statement, 1, actor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) queue.add(readAppointment(rows));
            }
            return queue;
        } catch (SQLException exception) {
            throw databaseError("Could not load the consultation queue.", exception);
        }
    }

    public void addAppointment(User actor, AppointmentInput input) {
        if (input.patientId() <= 0 || input.doctorId() <= 0 || input.appointmentAt() == null) {
            throw new IllegalArgumentException("Choose a patient, doctor, and appointment time.");
        }
        if (!Set.of("Online", "Offline").contains(input.visitMode())) {
            throw new IllegalArgumentException("Choose Online or Offline for the appointment source.");
        }
        if (input.appointmentAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Appointment time must be in the future.");
        }
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            AppointmentLinks links = requirePatientAndDoctor(connection, actor, input.patientId(), input.doctorId());
            try (PreparedStatement duplicate = connection.prepareStatement("""
                    SELECT EXISTS (SELECT 1 FROM appointments a
                      WHERE a.doctor_id = ? AND a.appointment_at = ?
                    """ + scope(actor, "a") + ")")) {
                duplicate.setLong(1, input.doctorId());
                duplicate.setObject(2, input.appointmentAt());
                bindScope(duplicate, 3, actor);
                try (ResultSet row = duplicate.executeQuery()) {
                    row.next();
                    if (row.getBoolean(1)) throw new IllegalArgumentException("That doctor already has an appointment at this time.");
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO appointments
                        (owner_user_id, patient_id, doctor_id, patient_name, doctor_name, appointment_at, visit_mode)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                insert.setLong(1, links.ownerUserId());
                insert.setLong(2, input.patientId());
                insert.setLong(3, input.doctorId());
                insert.setString(4, links.patientName());
                insert.setString(5, links.doctorName());
                insert.setObject(6, input.appointmentAt());
                insert.setString(7, input.visitMode());
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            throw databaseError("Could not book the appointment.", exception);
        }
    }

    public void advanceAppointment(User actor, long appointmentId) {
        String sql = "UPDATE appointments SET status = 'Yet to be Consulted' " +
                "WHERE id = ? AND status = 'Scheduled'" + scope(actor, "appointments");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("This appointment cannot advance from its current status.");
            }
        } catch (SQLException exception) {
            throw databaseError("Could not update appointment status.", exception);
        }
    }

    public void updateAppointment(User actor, long appointmentId, AppointmentInput input) {
        if (input.patientId() <= 0 || input.doctorId() <= 0 || input.appointmentAt() == null) {
            throw new IllegalArgumentException("Choose a patient, doctor, and appointment time.");
        }
        if (!Set.of("Online", "Offline").contains(input.visitMode())) {
            throw new IllegalArgumentException("Choose Online or Offline for the appointment source.");
        }
        if (input.appointmentAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Appointment time must be in the future.");
        }
        try (Connection connection = database.connect()) {
            AppointmentLinks links = requirePatientAndDoctor(connection, actor, input.patientId(), input.doctorId());
            long appointmentOwner = appointmentOwner(connection, actor, appointmentId);
            if (links.ownerUserId() != appointmentOwner) {
                throw new IllegalArgumentException("An appointment must remain with the same account as its patient.");
            }
            String duplicateSql = """
                    SELECT EXISTS (SELECT 1 FROM appointments a
                     WHERE a.id <> ? AND a.doctor_id = ? AND a.appointment_at = ?
                    """ + scope(actor, "a") + ")";
            try (PreparedStatement duplicate = connection.prepareStatement(duplicateSql)) {
                duplicate.setLong(1, appointmentId);
                duplicate.setLong(2, input.doctorId());
                duplicate.setObject(3, input.appointmentAt());
                bindScope(duplicate, 4, actor);
                try (ResultSet row = duplicate.executeQuery()) {
                    row.next();
                    if (row.getBoolean(1)) throw new IllegalArgumentException("That doctor already has an appointment at this time.");
                }
            }
            String sql = """
                    UPDATE appointments SET patient_id = ?, doctor_id = ?, patient_name = ?, doctor_name = ?,
                        appointment_at = ?, visit_mode = ?
                    WHERE id = ? AND status = 'Scheduled'
                    """ + scope(actor, "appointments");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, input.patientId());
                statement.setLong(2, input.doctorId());
                statement.setString(3, links.patientName());
                statement.setString(4, links.doctorName());
                statement.setObject(5, input.appointmentAt());
                statement.setString(6, input.visitMode());
                statement.setLong(7, appointmentId);
                bindScope(statement, 8, actor);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Only scheduled appointments can be edited.");
                }
            }
        } catch (SQLException exception) {
            throw databaseError("Could not edit the appointment.", exception);
        }
    }

    public void startConsultation(User actor, long appointmentId) {
        List<Appointment> queue = listConsultationQueue(actor);
        Appointment next = queue.stream()
                .filter(item -> !"In Consultation".equals(item.status()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There are no appointments waiting in the queue."));
        if (next.id() != appointmentId) {
            throw new IllegalArgumentException("Start the oldest waiting appointment first.");
        }
        String sql = """
                UPDATE appointments SET status = 'In Consultation'
                WHERE id = ? AND status IN ('Scheduled', 'Yet to be Consulted')
                """ + scope(actor, "appointments");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("This appointment cannot be started.");
        } catch (SQLException exception) {
            throw databaseError("Could not start the consultation.", exception);
        }
    }

    public void markAppointmentMissed(User actor, long appointmentId) {
        updateAppointmentStatus(actor, appointmentId, "Missed",
                "UPDATE appointments SET status = 'Missed' WHERE id = ? AND status IN ('Scheduled', 'Yet to be Consulted')");
    }

    public void deleteAppointment(User actor, long appointmentId) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM appointments WHERE id = ?" + scope(actor, "appointments"))) {
            statement.setLong(1, appointmentId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Appointment not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not cancel the appointment.", exception);
        }
    }

    public void createInvoiceFromAppointment(User actor, long appointmentId,
                                             BigDecimal fee, BigDecimal medication, BigDecimal other) {
        validateMoney(fee, medication, other);
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            String select = """
                    SELECT patient_name, doctor_name, status, billed, owner_user_id FROM appointments
                    WHERE id = ?
                    """ + scope(actor, "appointments") + " FOR UPDATE";
            String patient;
            String doctor;
            long recordOwnerId;
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setLong(1, appointmentId);
                bindScope(statement, 2, actor);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) throw new IllegalArgumentException("Appointment not found.");
                    if (!"Completed".equals(row.getString("status"))) {
                        throw new IllegalArgumentException("Complete the appointment before creating its invoice.");
                    }
                    if (row.getBoolean("billed")) throw new IllegalArgumentException("An invoice already exists for this appointment.");
                    patient = row.getString("patient_name");
                    doctor = row.getString("doctor_name");
                    recordOwnerId = row.getLong("owner_user_id");
                }
            }
            insertInvoice(connection, recordOwnerId, appointmentId, patient, doctor, fee, medication, other);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE appointments SET billed = TRUE WHERE id = ?" + scope(actor, "appointments"))) {
                update.setLong(1, appointmentId);
                bindScope(update, 2, actor);
                update.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            throw databaseError("Could not create the appointment invoice.", exception);
        }
    }

    public List<Consultation> listConsultations(User actor) {
        List<Consultation> history = new ArrayList<>();
        String sql = """
                SELECT id, appointment_id, patient_name, doctor_name, age, gender, complaints, diagnosis,
                       medication, follow_up_date, notes, source, created_at
                FROM consultations c
                WHERE TRUE
                """ + scope(actor, "c") + " ORDER BY c.created_at DESC, c.id DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScope(statement, 1, actor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) history.add(readConsultation(rows));
            }
            return history;
        } catch (SQLException exception) {
            throw databaseError("Could not load consultation history.", exception);
        }
    }

    public void saveConsultation(User actor, ConsultationInput input) {
        if (input.age() < 0 || input.age() > 120) throw new IllegalArgumentException("Age must be from 0 to 120.");
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            String appointmentSql = """
                    SELECT patient_id, patient_name, doctor_name, owner_user_id, visit_mode FROM appointments
                    WHERE id = ? AND status = 'In Consultation'
                    """ + scope(actor, "appointments") + " FOR UPDATE";
            String patient;
            String doctor;
            String visitMode;
            Long patientId;
            long recordOwnerId;
            try (PreparedStatement statement = connection.prepareStatement(appointmentSql)) {
                statement.setLong(1, input.appointmentId());
                bindScope(statement, 2, actor);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) throw new IllegalArgumentException("Start the appointment before saving a consultation.");
                    long patientValue = row.getLong("patient_id");
                    patientId = row.wasNull() ? null : patientValue;
                    patient = row.getString("patient_name");
                    doctor = row.getString("doctor_name");
                    visitMode = row.getString("visit_mode");
                    recordOwnerId = row.getLong("owner_user_id");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO consultations
                        (owner_user_id, appointment_id, patient_name, doctor_name, age, gender, complaints,
                         diagnosis, medication, follow_up_date, notes, source)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setLong(1, recordOwnerId);
                statement.setLong(2, input.appointmentId());
                statement.setString(3, patient);
                statement.setString(4, doctor);
                statement.setInt(5, input.age());
                statement.setString(6, clean(input.gender()));
                statement.setString(7, clean(input.complaints()));
                statement.setString(8, clean(input.diagnosis()));
                statement.setString(9, clean(input.medication()));
                if (input.followUpDate() == null) statement.setNull(10, Types.DATE);
                else statement.setObject(10, input.followUpDate());
                statement.setString(11, clean(input.notes()));
                statement.setString(12, visitMode);
                statement.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE appointments SET status = 'Completed' WHERE id = ?" + scope(actor, "appointments"))) {
                update.setLong(1, input.appointmentId());
                bindScope(update, 2, actor);
                update.executeUpdate();
            }
            String patientStatus = input.followUpDate() == null ? "Active" : "Follow Up";
            if (patientId != null) {
                // Update the exact patient on the appointment (not "any patient with the same name").
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE patients SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND owner_user_id = ?
                        """)) {
                    update.setString(1, patientStatus);
                    update.setLong(2, patientId);
                    update.setLong(3, recordOwnerId);
                    update.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            throw databaseError("Could not save the consultation.", exception);
        }
    }

    public boolean undoLastConsultation(User actor) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            String sql = "SELECT id, appointment_id FROM consultations c WHERE TRUE" +
                    scope(actor, "c") + " ORDER BY c.created_at DESC, c.id DESC LIMIT 1 FOR UPDATE";
            long consultationId;
            Long appointmentId;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindScope(statement, 1, actor);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) {
                        connection.rollback();
                        return false;
                    }
                    consultationId = row.getLong("id");
                    long value = row.getLong("appointment_id");
                    appointmentId = row.wasNull() ? null : value;
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM consultations WHERE id = ?" + scope(actor, "consultations"))) {
                statement.setLong(1, consultationId);
                bindScope(statement, 2, actor);
                statement.executeUpdate();
            }
            if (appointmentId != null) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE appointments SET status = 'Yet to be Consulted' WHERE id = ?" +
                                scope(actor, "appointments"))) {
                    statement.setLong(1, appointmentId);
                    bindScope(statement, 2, actor);
                    statement.executeUpdate();
                }
            }
            connection.commit();
            return true;
        } catch (SQLException exception) {
            throw databaseError("Could not undo the latest consultation.", exception);
        }
    }

    public List<Invoice> listInvoices(User actor) {
        List<Invoice> invoices = new ArrayList<>();
        String sql = """
                SELECT id, appointment_id, patient_name, doctor_name, consultation_fee, medication_amount,
                       other_amount, payment_status, created_at
                FROM invoices i WHERE TRUE
                """ + scope(actor, "i") + " ORDER BY i.created_at DESC, i.id DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScope(statement, 1, actor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) invoices.add(readInvoice(rows));
            }
            return invoices;
        } catch (SQLException exception) {
            throw databaseError("Could not load invoices.", exception);
        }
    }

    public void addInvoice(User actor, InvoiceInput input) {
        if (input.patientName().isBlank() || input.doctorName().isBlank()) {
            throw new IllegalArgumentException("Enter the patient and doctor names.");
        }
        validateMoney(input.consultationFee(), input.medicationAmount(), input.otherAmount());
        try (Connection connection = database.connect()) {
            insertInvoice(connection, actor.id(), null, input.patientName().trim(), input.doctorName().trim(),
                    input.consultationFee(), input.medicationAmount(), input.otherAmount());
        } catch (SQLException exception) {
            throw databaseError("Could not create the invoice.", exception);
        }
    }

    public void updateInvoice(User actor, long invoiceId, InvoiceInput input) {
        if (input.patientName().isBlank() || input.doctorName().isBlank()) {
            throw new IllegalArgumentException("Enter the patient and doctor names.");
        }
        validateMoney(input.consultationFee(), input.medicationAmount(), input.otherAmount());
        String sql = """
                UPDATE invoices SET patient_name = ?, doctor_name = ?, consultation_fee = ?,
                    medication_amount = ?, other_amount = ?
                WHERE id = ?
                """ + scope(actor, "invoices");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, input.patientName().trim());
            statement.setString(2, input.doctorName().trim());
            statement.setBigDecimal(3, input.consultationFee());
            statement.setBigDecimal(4, input.medicationAmount());
            statement.setBigDecimal(5, input.otherAmount());
            statement.setLong(6, invoiceId);
            bindScope(statement, 7, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Invoice not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not update the invoice.", exception);
        }
    }

    public void markInvoicePaid(User actor, long invoiceId) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE invoices SET payment_status = 'PAID' WHERE id = ?" + scope(actor, "invoices"))) {
            statement.setLong(1, invoiceId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Invoice not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not mark the invoice as paid.", exception);
        }
    }

    public void deleteInvoice(User actor, long invoiceId) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM invoices WHERE id = ?" + scope(actor, "invoices"))) {
            statement.setLong(1, invoiceId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Invoice not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not delete the invoice.", exception);
        }
    }

    public DashboardStats dashboardStats(User actor) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM patients p WHERE TRUE
                """ + scope(actor, "p") + """
                    ) AS patients,
                    (SELECT COUNT(*) FROM patients p WHERE p.status = 'Active'
                """ + scope(actor, "p") + """
                    ) AS active_patients,
                    (SELECT COUNT(*) FROM patients p WHERE p.status = 'Follow Up'
                """ + scope(actor, "p") + """
                    ) AS follow_up_patients,
                    (SELECT COUNT(*) FROM appointments a
                     WHERE a.appointment_at::date = CURRENT_DATE
                """ + scope(actor, "a") + """
                    ) AS appointments_today,
                    (SELECT COUNT(*) FROM doctors d WHERE d.status = 'Available'
                """ + scope(actor, "d") + """
                    ) AS doctors_available,
                    (SELECT COUNT(*) FROM invoices i WHERE i.payment_status = 'UNPAID'
                """ + scope(actor, "i") + """
                    ) AS unpaid_invoices
                """;
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            parameter = bindScope(statement, parameter, actor);
            parameter = bindScope(statement, parameter, actor);
            parameter = bindScope(statement, parameter, actor);
            parameter = bindScope(statement, parameter, actor);
            parameter = bindScope(statement, parameter, actor);
            bindScope(statement, parameter, actor);
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return new DashboardStats(row.getLong("patients"), row.getLong("active_patients"),
                        row.getLong("follow_up_patients"), row.getLong("appointments_today"),
                        row.getLong("doctors_available"), row.getLong("unpaid_invoices"));
            }
        } catch (SQLException exception) {
            throw databaseError("Could not load dashboard totals.", exception);
        }
    }

    public List<Doctor> listAvailableDoctorsForPatient(User actor, long patientId) {
        try (Connection connection = database.connect()) {
            long ownerId = patientOwner(connection, actor, patientId);
            List<Doctor> doctors = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, name, specialization, qualifications, experience_years, phone, status
                    FROM doctors WHERE owner_user_id = ? AND status = 'Available'
                    ORDER BY name, id
                    """)) {
                statement.setLong(1, ownerId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) doctors.add(new Doctor(rows.getLong("id"), rows.getString("name"),
                            rows.getString("specialization"), rows.getString("qualifications"),
                            rows.getInt("experience_years"), rows.getString("phone"), rows.getString("status")));
                }
            }
            return doctors;
        } catch (SQLException exception) {
            throw databaseError("Could not load available doctors for this patient.", exception);
        }
    }

    private AppointmentLinks requirePatientAndDoctor(Connection connection, User actor,
                                                      long patientId, long doctorId) throws SQLException {
        String patientSql = "SELECT p.owner_user_id, p.name FROM patients p WHERE p.id = ?" + scope(actor, "p");
        long patientOwnerId;
        String patientName;
        try (PreparedStatement statement = connection.prepareStatement(patientSql)) {
            statement.setLong(1, patientId);
            bindScope(statement, 2, actor);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Select a patient from your patient records.");
                patientOwnerId = row.getLong("owner_user_id");
                patientName = row.getString("name");
            }
        }
        String doctorSql = "SELECT owner_user_id, name, status FROM doctors d WHERE d.id = ?" + scope(actor, "d");
        String doctorName;
        try (PreparedStatement statement = connection.prepareStatement(doctorSql)) {
            statement.setLong(1, doctorId);
            bindScope(statement, 2, actor);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Select a doctor from your doctor directory.");
                if (!"Available".equals(row.getString("status"))) {
                    throw new IllegalArgumentException("That doctor is not currently available.");
                }
                if (row.getLong("owner_user_id") != patientOwnerId) {
                    throw new IllegalArgumentException("Choose a doctor from the same account as the patient.");
                }
                doctorName = row.getString("name");
            }
        }
        return new AppointmentLinks(patientOwnerId, patientName, doctorName);
    }

    private long patientOwner(Connection connection, User actor, long patientId) throws SQLException {
        String sql = "SELECT owner_user_id FROM patients WHERE id = ?" + scope(actor, "patients");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, patientId);
            bindScope(statement, 2, actor);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Patient record not found.");
                return row.getLong("owner_user_id");
            }
        }
    }

    private long appointmentOwner(Connection connection, User actor, long appointmentId) throws SQLException {
        String sql = "SELECT owner_user_id FROM appointments WHERE id = ?" + scope(actor, "appointments");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            bindScope(statement, 2, actor);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Appointment not found.");
                return row.getLong("owner_user_id");
            }
        }
    }

    private void insertInvoice(Connection connection, long ownerUserId, Long appointmentId, String patient,
                               String doctor, BigDecimal fee, BigDecimal medication, BigDecimal other)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO invoices
                    (owner_user_id, appointment_id, patient_name, doctor_name, consultation_fee,
                     medication_amount, other_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, ownerUserId);
            if (appointmentId == null) statement.setNull(2, Types.BIGINT);
            else statement.setLong(2, appointmentId);
            statement.setString(3, patient);
            statement.setString(4, doctor);
            statement.setBigDecimal(5, fee);
            statement.setBigDecimal(6, medication);
            statement.setBigDecimal(7, other);
            statement.executeUpdate();
        }
    }

    private void updateAppointmentStatus(User actor, long appointmentId, String status, String updateSql) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(updateSql + scope(actor, "appointments"))) {
            statement.setLong(1, appointmentId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Appointment cannot be marked " + status + ".");
        } catch (SQLException exception) {
            throw databaseError("Could not update appointment status.", exception);
        }
    }

    private static Patient readPatient(ResultSet row) throws SQLException {
        long id = row.getLong("id");
        return new Patient(id, String.format(Locale.ROOT, "P%04d", id), row.getString("name"),
                row.getInt("age"), row.getString("gender"), row.getString("contact"), row.getString("doctor"),
                row.getString("visit_type"), row.getString("diagnosis"), row.getString("status"),
                row.getString("source"), row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant());
    }

    private static Appointment readAppointment(ResultSet row) throws SQLException {
        long patientValue = row.getLong("patient_id");
        Long patientId = row.wasNull() ? null : patientValue;
        long doctorValue = row.getLong("doctor_id");
        Long doctorId = row.wasNull() ? null : doctorValue;
        return new Appointment(row.getLong("id"), patientId, doctorId,
                row.getString("patient_name"), row.getString("doctor_name"),
                row.getObject("appointment_at", OffsetDateTime.class), row.getString("visit_mode"),
                row.getString("status"), row.getBoolean("billed"));
    }

    private static Consultation readConsultation(ResultSet row) throws SQLException {
        long appointmentIdValue = row.getLong("appointment_id");
        Long appointmentId = row.wasNull() ? null : appointmentIdValue;
        return new Consultation(row.getLong("id"), appointmentId, row.getString("patient_name"),
                row.getString("doctor_name"), row.getInt("age"), row.getString("gender"),
                row.getString("complaints"), row.getString("diagnosis"), row.getString("medication"),
                row.getObject("follow_up_date", java.time.LocalDate.class), row.getString("notes"),
                row.getString("source"), row.getObject("created_at", OffsetDateTime.class));
    }

    private static Invoice readInvoice(ResultSet row) throws SQLException {
        long appointmentValue = row.getLong("appointment_id");
        Long appointmentId = row.wasNull() ? null : appointmentValue;
        BigDecimal fee = row.getBigDecimal("consultation_fee");
        BigDecimal medication = row.getBigDecimal("medication_amount");
        BigDecimal other = row.getBigDecimal("other_amount");
        return new Invoice(row.getLong("id"), appointmentId, row.getString("patient_name"),
                row.getString("doctor_name"), fee, medication, other, fee.add(medication).add(other),
                row.getString("payment_status"), row.getObject("created_at", OffsetDateTime.class));
    }

    private static void validatePatient(PatientInput input) {
        if (input.name() == null || input.name().isBlank() || input.name().trim().length() > 120) {
            throw new IllegalArgumentException("Patient name is required (maximum 120 characters).");
        }
        if (input.age() < 0 || input.age() > 120) throw new IllegalArgumentException("Age must be a whole number from 0 to 120.");
        if (input.contact() == null || input.contact().isBlank() || input.contact().trim().length() > 30) {
            throw new IllegalArgumentException("Enter a contact number (maximum 30 characters).");
        }
        if (!Set.of("First Visit", "Follow-up").contains(input.visitType())) {
            throw new IllegalArgumentException("Choose First Visit or Follow-up.");
        }
        if (!PATIENT_STATUSES.contains(input.status())) throw new IllegalArgumentException("Choose a valid patient status.");
        if (!Set.of("Online", "Offline").contains(input.source())) throw new IllegalArgumentException("Choose Online or Offline.");
    }

    private static void bindPatient(PreparedStatement statement, PatientInput input, int start) throws SQLException {
        statement.setString(start, input.name().trim());
        statement.setInt(start + 1, input.age());
        statement.setString(start + 2, clean(input.gender()));
        statement.setString(start + 3, clean(input.contact()));
        statement.setString(start + 4, clean(input.doctor()));
        statement.setString(start + 5, input.visitType());
        statement.setString(start + 6, clean(input.diagnosis()));
        statement.setString(start + 7, input.status());
        statement.setString(start + 8, input.source());
    }

    private static void validateMoney(BigDecimal... amounts) {
        for (BigDecimal amount : amounts) {
            if (amount == null || amount.signum() < 0 || amount.scale() > 2) {
                throw new IllegalArgumentException("Amounts must be non-negative and use no more than two decimal places.");
            }
        }
    }

    private static String scope(User actor, String alias) {
        return actor.isAdmin() ? "" : " AND " + alias + ".owner_user_id = ?";
    }

    private static int bindScope(PreparedStatement statement, int parameter, User actor) throws SQLException {
        if (!actor.isAdmin()) {
            statement.setLong(parameter, actor.id());
            return parameter + 1;
        }
        return parameter;
    }

    private static String likeTerm(String search) {
        return "%" + (search == null ? "" : search.trim()) + "%";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static DatabaseException databaseError(String message, SQLException exception) {
        return new DatabaseException(message, exception);
    }

    private record AppointmentLinks(long ownerUserId, String patientName, String doctorName) { }
}
