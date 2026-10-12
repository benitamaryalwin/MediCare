package com.medicarepro;


import javax.swing.*;
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
        setSubtitle("Administrator only · view accounts and create receptionist logins.");
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
        Map<String, String> values = FormDialogs.show(this, "Create receptionist login",
                List.of(FieldSpec.text("receptionistId", "Receptionist ID", ""),
                        FieldSpec.text("displayName", "Full name", "")));
        if (values == null) return;
        runAsync("Receptionist creation", () -> authService.createReceptionist(admin, values.get("displayName"), values.get("receptionistId")));
    }

    private static Object[] row(UserSummary user) {
        return new Object[]{user.id(), user.username(), user.displayName(), user.role(),
                user.active() ? "Active" : "Disabled"};
    }
}
