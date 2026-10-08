package com.medicarepro.service;

import com.medicarepro.data.Database;
import com.medicarepro.data.DatabaseException;
import com.medicarepro.model.Role;
import com.medicarepro.model.User;
import com.medicarepro.model.UserSummary;
import com.medicarepro.security.PasswordHasher;

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
                     "SELECT EXISTS (SELECT 1 FROM app_users WHERE role = 'ADMIN')")) {
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
                    "SELECT EXISTS (SELECT 1 FROM app_users WHERE role = 'ADMIN')");
                 ResultSet row = exists.executeQuery()) {
                row.next();
                if (row.getBoolean(1)) {
                    throw new IllegalStateException("An administrator account already exists.");
                }
            }
            insertUser(connection, username, displayName, credentials, Role.ADMIN);
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

    public UserSummary createUser(User actor, String username, String displayName, char[] password) {
        requireAdmin(actor);
        validateAccount(username, displayName, password);
        PasswordHasher.PasswordHash credentials = passwordHasher.hash(password);
        try (Connection connection = database.connect()) {
            return insertUser(connection, username, displayName, credentials, Role.USER);
        } catch (SQLException exception) {
            throw translateAccountError("Could not create the user account.", exception);
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

    public void setUserActive(User actor, long userId, boolean active) {
        requireAdmin(actor);
        if (actor.id() == userId) throw new IllegalArgumentException("You cannot deactivate your own administrator account.");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE app_users SET active = ? WHERE id = ? AND role = 'USER'")) {
            statement.setBoolean(1, active);
            statement.setLong(2, userId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("The user account was not found.");
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update the user account.", exception);
        }
    }

    public void resetUserPassword(User actor, long userId, char[] newPassword) {
        requireAdmin(actor);
        validatePassword(newPassword);
        PasswordHasher.PasswordHash credentials = passwordHasher.hash(newPassword);
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE app_users SET password_hash = ?, password_salt = ?
                     WHERE id = ? AND role = 'USER'
                     """)) {
            statement.setBytes(1, credentials.hash());
            statement.setBytes(2, credentials.salt());
            statement.setLong(3, userId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("The user account was not found.");
        } catch (SQLException exception) {
            throw new DatabaseException("Could not reset the user password.", exception);
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
