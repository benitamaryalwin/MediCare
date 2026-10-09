package com.medicarepro.model;

public record DashboardStats(
        long patients,
        long activePatients,
        long followUpPatients,
        long appointmentsToday,
        long doctorsAvailable) { }
