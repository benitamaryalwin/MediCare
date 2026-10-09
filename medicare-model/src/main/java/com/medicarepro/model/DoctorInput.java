package com.medicarepro.model;

public record DoctorInput(
        String name,
        String specialization,
        String qualifications,
        int experienceYears,
        String phone) { }
