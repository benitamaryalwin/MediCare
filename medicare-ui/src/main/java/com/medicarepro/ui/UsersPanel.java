package com.medicarepro.ui;

import com.medicarepro.model.User;
import com.medicarepro.model.UserSummary;
import com.medicarepro.service.AuthService;
import com.medicarepro.service.ClinicEvents;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class UsersPanel extends RecordPanel<UserSummary> {
    private static final String[] COLUMNS = {"ID", "Username", "Display name", "Role", "Account"};
    private final AuthService authService;
    private final User admin;

    public UsersPanel(AuthService authService, User admin, ClinicEvents events) {
        super("User accounts", COLUMNS, UsersPanel::row, events);
        this.authService = authService;
        this.admin = admin;
        setSubtitle("Administrator only · create separate logins and disable accounts without deleting their records.");
        addActionButton("Reset password", () -> withSelected(this::resetPassword));
        addActionButton("Enable / disable", () -> withSelected(this::toggleAccount));
        addActionButton("Export CSV", () -> CsvExporter.export(this, table, "medicare-users.csv"));
    }

    @Override protected boolean supportsEdit() { return false; }
    @Override protected boolean supportsDelete() { return false; }

    @Override protected List<UserSummary> loadRows(String search) {
        String term = search == null ? "" : search.trim().toLowerCase();
        return authService.listUsers(admin).stream()
                .filter(user -> term.isEmpty() || user.username().toLowerCase().contains(term)
                        || user.displayName().toLowerCase().contains(term))
                .toList();
    }

    @Override protected void onAdd() {
        Map<String, String> values = FormDialogs.show(this, "Create user account",
                List.of(FieldSpec.text("username", "Username", ""),
                        FieldSpec.text("displayName", "Display name", "")));
        if (values == null) return;
        char[] password = FormDialogs.askPassword(this, "Set initial password", "Password (at least 10 characters)");
        if (password == null) return;
        char[] confirm = FormDialogs.askPassword(this, "Confirm password", "Enter the same password again");
        if (confirm == null) {
            Arrays.fill(password, '\0');
            return;
        }
        if (!Arrays.equals(password, confirm)) {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
            FormDialogs.showError(this, new IllegalArgumentException("The passwords do not match."));
            return;
        }
        Arrays.fill(confirm, '\0');
        runAsync("User account creation", () -> {
            try {
                authService.createUser(admin, values.get("username"), values.get("displayName"), password);
            } finally {
                Arrays.fill(password, '\0');
            }
        });
    }

    private void resetPassword(UserSummary account) {
        if (!"USER".equals(account.role())) {
            JOptionPane.showMessageDialog(this, "Only regular user passwords can be reset from this screen.",
                    "Account security", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        char[] password = FormDialogs.askPassword(this, "Reset password for " + account.username(),
                "New password (at least 10 characters)");
        if (password == null) return;
        runAsync("Password reset", () -> {
            try {
                authService.resetUserPassword(admin, account.id(), password);
            } finally {
                Arrays.fill(password, '\0');
            }
        });
    }

    private void toggleAccount(UserSummary account) {
        if (!"USER".equals(account.role())) {
            JOptionPane.showMessageDialog(this, "The administrator account cannot be disabled here.",
                    "Account security", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        boolean activate = !account.active();
        String action = activate ? "Enable" : "Disable";
        if (JOptionPane.showConfirmDialog(this, action + " " + account.username() + "?",
                action + " account", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            runAsync("Account " + action.toLowerCase(), () ->
                    authService.setUserActive(admin, account.id(), activate));
        }
    }

    @Override protected void onDelete(UserSummary account) {
        toggleAccount(account);
    }

    private static Object[] row(UserSummary user) {
        return new Object[]{user.id(), user.username(), user.displayName(), user.role(),
                user.active() ? "Active" : "Disabled"};
    }
}
