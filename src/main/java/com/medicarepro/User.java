package com.medicarepro;

public record User(long id, String username, String displayName, Role role, boolean active) {
    public boolean isAdmin() {
        return role == Role.HEAD_DOCTOR;
    }

    public boolean isReceptionist() {
        return role == Role.RECEPTIONIST;
    }

    public boolean isPatient() {
        return role == Role.PATIENT;
    }
}
