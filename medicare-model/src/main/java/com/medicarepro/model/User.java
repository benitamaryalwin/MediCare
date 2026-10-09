package com.medicarepro.model;

public record User(long id, String username, String displayName, Role role, boolean active) {
    public boolean isAdmin() {
        return role == Role.HEAD_DOCTOR;
    }

    public boolean isHeadDoctor() {
        return role == Role.HEAD_DOCTOR;
    }
}
