package com.example.doctor_patient_management_system.dto;


public record DashboardStats(
        long totalAdmins,
        long totalDoctors,
        long totalPatients,
        long totalUsers,
        long totalAppointments,
        long confirmedAppointments,
        long cancelledAppointments
) {}

//Auto-generated: constructor, getters, equals(), hashCode(), toString()
