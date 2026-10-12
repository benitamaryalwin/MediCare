package com.medicarepro;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** JDBC operations for patient records and the patient dashboard. */
public final class ClinicRepository {
    private static final String PATIENT_COLUMNS = "id, name, age, gender, contact, visit_type, diagnosis, status, source, created_at, updated_at";
    private static final Set<String> PATIENT_STATUSES = Set.of("Active", "Follow Up", "Discharged");
    private final Database database;

    public ClinicRepository(Database database) {
        this.database = database;
    }

    public List<Patient> listPatients(User actor, String search) {
        String sql = "SELECT " + PATIENT_COLUMNS + " FROM patients p "
                + "WHERE (CAST(p.id AS TEXT) ILIKE ? OR p.name ILIKE ? OR p.contact ILIKE ? "
                + "OR p.diagnosis ILIKE ? OR p.status ILIKE ?)" + scope(actor, "p")
                + " ORDER BY p.created_at DESC, p.id DESC";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String term = likeTerm(search);
            for (int index = 1; index <= 5; index++) statement.setString(index, term);
            bindScope(statement, 6, actor);
            return readPatients(statement);
        } catch (SQLException exception) {
            throw databaseError("Could not load patient records.", exception);
        }
    }

    public List<Patient> recentPatients(User actor) {
        String sql = "SELECT " + PATIENT_COLUMNS + " FROM patients p WHERE TRUE" + scope(actor, "p")
                + " ORDER BY p.created_at DESC, p.id DESC LIMIT 6";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScope(statement, 1, actor);
            return readPatients(statement);
        } catch (SQLException exception) {
            throw databaseError("Could not load recent patients.", exception);
        }
    }

    public Patient createPatient(User actor, PatientInput input) {
        validatePatient(input);
        String sql = """
                INSERT INTO patients (owner_user_id, name, age, gender, contact, visit_type, diagnosis, status, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, name, age, gender, contact, visit_type, diagnosis, status, source, created_at, updated_at
                """;
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actor.id());
            bindPatient(statement, input, 2);
            statement.setString(9, actor.isPatient() ? "Online" : "Offline");
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
                UPDATE patients SET name = ?, age = ?, gender = ?, contact = ?, visit_type = ?, diagnosis = ?,
                    status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """ + scope(actor, "patients") + " RETURNING " + PATIENT_COLUMNS;
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPatient(statement, input, 1);
            statement.setLong(8, patientId);
            bindScope(statement, 9, actor);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("Patient record not found.");
                return readPatient(row);
            }
        } catch (SQLException exception) {
            throw databaseError("Could not update the patient record.", exception);
        }
    }

    public void deletePatient(User actor, long patientId) {
        String sql = "DELETE FROM patients WHERE id = ?" + scope(actor, "patients");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, patientId);
            bindScope(statement, 2, actor);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Patient record not found.");
        } catch (SQLException exception) {
            throw databaseError("Could not delete the patient record.", exception);
        }
    }

    public DashboardStats dashboardStats(User actor) {
        String sql = """
                SELECT COUNT(*) AS patients,
                       COUNT(*) FILTER (WHERE status = 'Active') AS active_patients,
                       COUNT(*) FILTER (WHERE status = 'Follow Up') AS follow_up_patients,
                       COUNT(*) FILTER (WHERE source = 'Online') AS online_patients
                FROM patients p WHERE TRUE
                """ + scope(actor, "p");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScope(statement, 1, actor);
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return new DashboardStats(row.getLong("patients"), row.getLong("active_patients"),
                        row.getLong("follow_up_patients"), row.getLong("online_patients"));
            }
        } catch (SQLException exception) {
            throw databaseError("Could not load dashboard totals.", exception);
        }
    }

    private List<Patient> readPatients(PreparedStatement statement) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery()) {
            while (rows.next()) patients.add(readPatient(rows));
        }
        return patients;
    }

    private static Patient readPatient(ResultSet row) throws SQLException {
        long id = row.getLong("id");
        return new Patient(id, String.format(Locale.ROOT, "P%04d", id), row.getString("name"),
                row.getInt("age"), row.getString("gender"), row.getString("contact"),
                row.getString("visit_type"), row.getString("diagnosis"), row.getString("status"),
                row.getString("source"), row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant());
    }

    private static void validatePatient(PatientInput input) {
        if (input == null || input.name() == null || input.name().isBlank() || input.name().trim().length() > 120) {
            throw new IllegalArgumentException("Patient name is required (maximum 120 characters).");
        }
        if (input.age() < 0 || input.age() > 120) throw new IllegalArgumentException("Age must be from 0 to 120.");
        if (input.contact() == null || input.contact().isBlank() || input.contact().trim().length() > 30) {
            throw new IllegalArgumentException("Enter a contact number (maximum 30 characters).");
        }
        if (!Set.of("First Visit", "Follow-up").contains(input.visitType())) {
            throw new IllegalArgumentException("Choose First Visit or Follow-up.");
        }
        if (!PATIENT_STATUSES.contains(input.status())) throw new IllegalArgumentException("Choose a valid patient status.");
    }

    private static void bindPatient(PreparedStatement statement, PatientInput input, int start) throws SQLException {
        statement.setString(start, input.name().trim());
        statement.setInt(start + 1, input.age());
        statement.setString(start + 2, clean(input.gender()));
        statement.setString(start + 3, clean(input.contact()));
        statement.setString(start + 4, input.visitType());
        statement.setString(start + 5, clean(input.diagnosis()));
        statement.setString(start + 6, input.status());
    }

    private static String scope(User actor, String alias) {
        return actor.isAdmin() ? "" : " AND " + alias + ".owner_user_id = ?";
    }

    private static int bindScope(PreparedStatement statement, int parameter, User actor) throws SQLException {
        if (actor.isAdmin()) return parameter;
        statement.setLong(parameter, actor.id());
        return parameter + 1;
    }

    private static String likeTerm(String value) {
        return "%" + (value == null ? "" : value.trim()) + "%";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static DatabaseException databaseError(String message, SQLException exception) {
        return new DatabaseException(message, exception);
    }
}
