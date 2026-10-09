package com.medicarepro.model;

import java.time.OffsetDateTime;

public record Appointment(
        long id,
        Long patientId,
        Long doctorId,
        String patientName,
        String doctorName,
        OffsetDateTime appointmentAt,
        String visitMode,
        String status) { }
