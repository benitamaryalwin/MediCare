package com.medicarepro;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AuthService {
    private final Database database;
    private final PasswordHasher passwordHasher = new PasswordHasher();

    public AuthService(Database database) {
        this.database = database;
    }

    public boolean hasAdminAccount() {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT EXISTS (SELECT 1 FROM app_users WHERE role = 'HEAD_DOCTOR')")) {
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return row.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not check the initial administrator account.", exception);
        }
    }

    public void createInitialAdmin(String username, String displayName, char[] password) {
        validateAccount(username, displayName, password);
        PasswordHasher.PasswordHash credentials = passwordHasher.hash(password);
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (Statement lock = connection.createStatement()) {
                lock.execute("SELECT pg_advisory_xact_lock(78392041)");
            }
            try (PreparedStatement exists = connection.prepareStatement(
                    "SELECT EXISTS (SELECT 1 FROM app_users WHERE role = 'HEAD_DOCTOR')");
                 ResultSet row = exists.executeQuery()) {
                row.next();
                if (row.getBoolean(1)) {
                    throw new IllegalStateException("An administrator account already exists.");
                }
            }
            insertUser(connection, username, displayName, credentials, Role.HEAD_DOCTOR);
            connection.commit();
        } catch (SQLException exception) {
            throw translateAccountError("Could not create the administrator account.", exception);
        }
    }

    public User authenticate(String username, char[] password) {
        String canonical = canonicalUsername(username);
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, username, display_name, password_hash, password_salt, role, active
                     FROM app_users WHERE username = ?
                     """)) {
            statement.setString(1, canonical);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !row.getBoolean("active")) return null;
                byte[] hash = row.getBytes("password_hash");
                byte[] salt = row.getBytes("password_salt");
                if (!passwordHasher.verify(password, hash, salt)) return null;
                return new User(row.getLong("id"), row.getString("username"),
                        row.getString("display_name"), Role.valueOf(row.getString("role")),
                        row.getBoolean("active"));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not sign in to MediCare Pro.", exception);
        }
    }

    /** Passwordless identity login used by receptionists and patients. */
    public User authenticateByNameAndId(String displayName, String loginId, Role role) {
        if (displayName == null || displayName.isBlank() || loginId == null || loginId.isBlank()) return null;
        if (role != Role.RECEPTIONIST && role != Role.PATIENT) {
            throw new IllegalArgumentException("Unsupported passwordless login role.");
        }
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, username, display_name, role, active
                     FROM app_users
                     WHERE LOWER(display_name) = LOWER(?) AND LOWER(login_id) = LOWER(?) AND role = ?
                     """)) {
            statement.setString(1, displayName.trim());
            statement.setString(2, loginId.trim());
            statement.setString(3, role.name());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !row.getBoolean("active")) return null;
                return readUser(row);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not sign in to MediCare Pro.", exception);
        }
    }

    /** Patients can create their own passwordless account the first time they use the online mode. */
    public User registerOrAuthenticatePatient(String displayName, String patientId) {
        String name = cleanIdentity(displayName, "Patient name");
        String id = cleanIdentity(patientId, "Patient ID");
        try (Connection connection = database.connect()) {
            try (PreparedStatement find = connection.prepareStatement("""
                    SELECT id, username, display_name, role, active FROM app_users
                    WHERE LOWER(login_id) = LOWER(?) AND role = 'PATIENT'
                    """)) {
                find.setString(1, id);
                try (ResultSet row = find.executeQuery()) {
                    if (row.next()) {
                        if (!row.getString("display_name").equalsIgnoreCase(name)) return null;
                        return readUser(row);
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO app_users (username, display_name, login_id, role, active)
                    VALUES (?, ?, ?, 'PATIENT', TRUE) RETURNING id, username, display_name, role, active
                    """)) {
                insert.setString(1, "patient-" + id.toLowerCase(Locale.ROOT));
                insert.setString(2, name);
                insert.setString(3, id);
                try (ResultSet row = insert.executeQuery()) {
                    row.next();
                    return readUser(row);
                }
            }
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) return null;
            throw new DatabaseException("Could not register the patient login.", exception);
        }
    }

    public void createReceptionist(User actor, String displayName, String receptionistId) {
        requireAdmin(actor);
        String name = cleanIdentity(displayName, "Receptionist name");
        String id = cleanIdentity(receptionistId, "Receptionist ID");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO app_users (username, display_name, login_id, role, active)
                     VALUES (?, ?, ?, 'RECEPTIONIST', TRUE)
                     """)) {
            statement.setString(1, "receptionist-" + id.toLowerCase(Locale.ROOT));
            statement.setString(2, name);
            statement.setString(3, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                throw new IllegalArgumentException("That receptionist ID is already in use.", exception);
            }
            throw new DatabaseException("Could not create the receptionist login.", exception);
        }
    }

    public List<UserSummary> listUsers(User actor) {
        requireAdmin(actor);
        List<UserSummary> users = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, username, display_name, role, active
                     FROM app_users ORDER BY role, display_name, id
                     """);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                users.add(new UserSummary(rows.getLong("id"), rows.getString("username"),
                        rows.getString("display_name"), rows.getString("role"),
                        rows.getBoolean("active")));
            }
            return users;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load user accounts.", exception);
        }
    }

    private UserSummary insertUser(Connection connection, String username, String displayName,
                                   PasswordHasher.PasswordHash credentials, Role role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO app_users (username, display_name, password_hash, password_salt, role)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """)) {
            statement.setString(1, canonicalUsername(username));
            statement.setString(2, displayName.trim());
            statement.setBytes(3, credentials.hash());
            statement.setBytes(4, credentials.salt());
            statement.setString(5, role.name());
            try (ResultSet row = statement.executeQuery()) {
                row.next();
                return new UserSummary(row.getLong(1), canonicalUsername(username),
                        displayName.trim(), role.name(), true);
            }
        }
    }

    private static User readUser(ResultSet row) throws SQLException {
        return new User(row.getLong("id"), row.getString("username"), row.getString("display_name"),
                Role.valueOf(row.getString("role")), row.getBoolean("active"));
    }

    private static String cleanIdentity(String value, String label) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isBlank() || cleaned.length() > 60) throw new IllegalArgumentException(label + " is required (maximum 60 characters).");
        return cleaned;
    }

    private static void requireAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) throw new SecurityException("Only an administrator can manage user accounts.");
    }

    private static void validateAccount(String username, String displayName, char[] password) {
        String canonical = canonicalUsername(username);
        if (!canonical.matches("[a-z0-9._-]{3,60}")) {
            throw new IllegalArgumentException("Username must be 3–60 characters: letters, numbers, dot, underscore, or hyphen.");
        }
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 120) {
            throw new IllegalArgumentException("Enter a display name of 1–120 characters.");
        }
        validatePassword(password);
    }

    private static void validatePassword(char[] password) {
        if (password == null || password.length < 10 || password.length > 128) {
            throw new IllegalArgumentException("Password must contain 10–128 characters.");
        }
    }

    private static String canonicalUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static RuntimeException translateAccountError(String message, SQLException exception) {
        if ("23505".equals(exception.getSQLState())) {
            return new IllegalArgumentException("That username is already in use.", exception);
        }
        return new DatabaseException(message, exception);
    }
}
