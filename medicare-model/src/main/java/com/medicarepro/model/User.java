package com.medicarepro.model;

public record User(long id, String username, String displayName, Role role, boolean active) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
