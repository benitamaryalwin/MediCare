package com.medicarepro.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Consultation(
        long id,
        Long appointmentId,
        String patientName,
        String doctorName,
        int age,
        String gender,
        String complaints,
        String diagnosis,
        String medication,
        LocalDate followUpDate,
        String notes,
        String source,
        OffsetDateTime createdAt) { }
