package com.medicarepro.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Invoice(
        long id,
        Long appointmentId,
        String patientName,
        String doctorName,
        BigDecimal consultationFee,
        BigDecimal medicationAmount,
        BigDecimal otherAmount,
        BigDecimal total,
        String paymentStatus,
        OffsetDateTime createdAt) { }
