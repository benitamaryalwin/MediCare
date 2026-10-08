package com.medicarepro.model;

public record PatientInput(
        String name,
        int age,
        String gender,
        String contact,
        String doctor,
        String visitType,
        String diagnosis,
        String status,
        String source) { }
