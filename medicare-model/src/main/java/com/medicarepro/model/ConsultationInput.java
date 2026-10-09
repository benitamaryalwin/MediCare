package com.medicarepro.model;

import java.time.LocalDate;

public record ConsultationInput(
        long appointmentId,
        int age,
        String gender,
        String complaints,
        String diagnosis,
        String medication,
        LocalDate followUpDate,
        String notes) { }
