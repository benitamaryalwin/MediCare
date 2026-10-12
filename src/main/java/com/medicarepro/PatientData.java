package com.medicarepro;

import java.time.Instant;

record DashboardStats(
        long patients,
        long activePatients,
        long followUpPatients,
        long onlinePatients) { }

record Patient(
        long id,
        String patientCode,
        String name,
        int age,
        String gender,
        String contact,
        String visitType,
        String diagnosis,
        String status,
        String source,
        Instant createdAt,
        Instant updatedAt) { }

record PatientInput(
        String name,
        int age,
        String gender,
        String contact,
        String visitType,
        String diagnosis,
        String status) { }

enum Role {
    HEAD_DOCTOR,
    USER,
    RECEPTIONIST,
    PATIENT
}

record User(long id, String username, String displayName, Role role, boolean active) {
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

record UserSummary(long id, String username, String displayName, String role, boolean active) { }
