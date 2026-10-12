package com.medicarepro;

public record DashboardStats(
        long patients,
        long activePatients,
        long followUpPatients,
        long onlinePatients) { }
