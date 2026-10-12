package com.medicarepro;

public record PatientInput(
        String name,
        int age,
        String gender,
        String contact,
        String visitType,
        String diagnosis,
        String status) { }
