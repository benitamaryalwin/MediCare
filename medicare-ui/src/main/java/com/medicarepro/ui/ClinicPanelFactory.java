package com.medicarepro.ui;

import com.medicarepro.model.User;
import com.medicarepro.service.AuthService;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.JComponent;
import java.util.function.Consumer;

/** Abstract-factory-style creation point for the clinic's Swing sections. */
public final class ClinicPanelFactory {
    private final ClinicRepository repository;
    private final AuthService authService;
    private final ClinicEvents events;

    public ClinicPanelFactory(ClinicRepository repository, AuthService authService, ClinicEvents events) {
        this.repository = repository;
        this.authService = authService;
        this.events = events;
    }

    public JComponent create(String section, User user, Consumer<String> navigate) {
        return switch (section) {
            case "dashboard" -> new DashboardPanel(repository, user, events, navigate);
            case "patients" -> new PatientsPanel(repository, user, events);
            case "appointments" -> new AppointmentsPanel(repository, user, events);
            case "doctors" -> new DoctorsPanel(repository, user, events);
            case "accounts" -> {
                if (!user.isAdmin()) throw new SecurityException("Only administrators can manage accounts.");
                yield new UsersPanel(authService, user, events);
            }
            default -> throw new IllegalArgumentException("Unknown clinic section: " + section);
        };
    }
}
