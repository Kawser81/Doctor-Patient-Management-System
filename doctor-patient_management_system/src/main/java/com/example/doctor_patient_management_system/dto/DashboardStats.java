package com.example.doctor_patient_management_system.dto;

//public record DashboardStats(
//        long totalAdmins,
//        long totalDoctors,
//        long totalPatients,
//        long totalUsers
//) {}


//public record DashboardStats(
//        long totalAdmins,
//        long totalDoctors,
//        long totalPatients,
//        long totalUsers,
//        long totalAppointments,  // New
//        long pendingAppointments, // New
//        long completedAppointments // New
//) {}

public record DashboardStats(
        long totalAdmins,
        long totalDoctors,
        long totalPatients,
        long totalUsers,
        long totalAppointments,
        long confirmedAppointments,  // Fixed
        long cancelledAppointments   // Fixed
) {}