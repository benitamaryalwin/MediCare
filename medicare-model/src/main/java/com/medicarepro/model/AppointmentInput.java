package com.medicarepro.model;

import java.time.OffsetDateTime;

public record AppointmentInput(
        long patientId,
        long doctorId,
        OffsetDateTime appointmentAt,
        String visitMode) { }
