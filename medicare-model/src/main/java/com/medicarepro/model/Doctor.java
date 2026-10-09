package com.medicarepro.model;

public record Doctor(
        long id,
        String name,
        String specialization,
        String qualifications,
        int experienceYears,
        String phone,
        String status) { }
