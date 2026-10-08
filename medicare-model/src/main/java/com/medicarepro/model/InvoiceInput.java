package com.medicarepro.model;

import java.math.BigDecimal;

public record InvoiceInput(
        String patientName,
        String doctorName,
        BigDecimal consultationFee,
        BigDecimal medicationAmount,
        BigDecimal otherAmount) { }
