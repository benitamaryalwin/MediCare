package com.medicarepro.model;

import java.time.Instant;

public record Patient(
        long id,
        String patientCode,
        String name,
        int age,
        String gender,
        String contact,
        String doctor,
        String visitType,
        String diagnosis,
        String status,
        String source,
        Instant createdAt,
        Instant updatedAt) { }
